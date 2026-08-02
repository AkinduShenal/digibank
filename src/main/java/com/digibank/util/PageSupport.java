package com.digibank.util;

import org.springframework.data.domain.*;
import java.util.List;

public final class PageSupport {
	private PageSupport() { }
	public static <T> Page<T> page(List<T> values, int page, int size) {
		List<T> safe = values == null ? List.of() : values;
		int safeSize = Math.min(100, Math.max(5, size));
		int requestedPage = Math.max(0, page);
		int start = (int) Math.min((long) requestedPage * safeSize, safe.size());
		int end = Math.min(start + safeSize, safe.size());
		return new PageImpl<>(safe.subList(start, end), PageRequest.of(requestedPage, safeSize), safe.size());
	}
	public static String query(String value) { return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT); }
}
