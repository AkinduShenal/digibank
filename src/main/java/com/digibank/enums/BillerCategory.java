package com.digibank.enums;

public enum BillerCategory {
	ELECTRICITY("Electricity", "⚡"),
	WATER("Water", "◉"),
	MOBILE("Mobile", "▣"),
	INTERNET("Internet", "⌁");

	private final String displayName;
	private final String icon;

	BillerCategory(String displayName, String icon) {
		this.displayName = displayName;
		this.icon = icon;
	}

	public String getDisplayName() { return displayName; }
	public String getIcon() { return icon; }
}
