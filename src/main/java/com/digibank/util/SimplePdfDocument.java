package com.digibank.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SimplePdfDocument {

	private static final int LINES_PER_PAGE = 50;

	private SimplePdfDocument() {
	}

	public static byte[] create(List<String> sourceLines) {
		List<String> lines = sourceLines == null || sourceLines.isEmpty() ? List.of("No transactions found.") : sourceLines;
		int pageCount = (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE;
		int fontObject = 3 + (pageCount * 2);
		List<byte[]> objects = new ArrayList<>();
		for (int index = 0; index <= fontObject; index++) {
			objects.add(null);
		}
		objects.set(1, bytes("<< /Type /Catalog /Pages 2 0 R >>"));
		StringBuilder kids = new StringBuilder();
		for (int page = 0; page < pageCount; page++) {
			kids.append(3 + (page * 2)).append(" 0 R ");
		}
		objects.set(2, bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>"));
		for (int page = 0; page < pageCount; page++) {
			int pageObject = 3 + (page * 2);
			int contentObject = pageObject + 1;
			objects.set(pageObject, bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
					+ "/Resources << /Font << /F1 " + fontObject + " 0 R >> >> /Contents "
					+ contentObject + " 0 R >>"));
			int start = page * LINES_PER_PAGE;
			int end = Math.min(start + LINES_PER_PAGE, lines.size());
			StringBuilder content = new StringBuilder("BT\n/F1 9 Tf\n40 805 Td\n");
			for (int line = start; line < end; line++) {
				content.append('(').append(escape(shortLine(lines.get(line)))).append(") Tj\n0 -15 Td\n");
			}
			content.append("ET\n");
			byte[] stream = bytes(content.toString());
			objects.set(contentObject, bytes("<< /Length " + stream.length + " >>\nstream\n"
					+ content + "endstream"));
		}
		objects.set(fontObject, bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
		return assemble(objects, fontObject);
	}

	private static byte[] assemble(List<byte[]> objects, int objectCount) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			output.write(bytes("%PDF-1.4\n%DBNK\n"));
			int[] offsets = new int[objectCount + 1];
			for (int object = 1; object <= objectCount; object++) {
				offsets[object] = output.size();
				output.write(bytes(object + " 0 obj\n"));
				output.write(objects.get(object));
				output.write(bytes("\nendobj\n"));
			}
			int xref = output.size();
			output.write(bytes("xref\n0 " + (objectCount + 1) + "\n0000000000 65535 f \n"));
			for (int object = 1; object <= objectCount; object++) {
				output.write(bytes(String.format("%010d 00000 n \n", offsets[object])));
			}
			output.write(bytes("trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\nstartxref\n"
					+ xref + "\n%%EOF"));
			return output.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to create PDF statement.", ex);
		}
	}

	private static String shortLine(String value) {
		String safe = value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
		return safe.length() <= 105 ? safe : safe.substring(0, 102) + "...";
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
	}

	private static byte[] bytes(String value) {
		return value.getBytes(StandardCharsets.ISO_8859_1);
	}
}
