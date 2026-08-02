package com.digibank.enums;

public enum BillerProvider {
	CEB(BillerCategory.ELECTRICITY, "Ceylon Electricity Board", "Electricity account number"),
	LECO(BillerCategory.ELECTRICITY, "Lanka Electricity Company", "Electricity account number"),
	NWSDB(BillerCategory.WATER, "National Water Supply", "Water account number"),
	DIALOG_MOBILE(BillerCategory.MOBILE, "Dialog Mobile", "Mobile number"),
	MOBITEL(BillerCategory.MOBILE, "SLT-Mobitel", "Mobile number"),
	HUTCH(BillerCategory.MOBILE, "Hutch", "Mobile number"),
	AIRTEL(BillerCategory.MOBILE, "Airtel", "Mobile number"),
	SLT_BROADBAND(BillerCategory.INTERNET, "SLT Broadband", "Broadband account number"),
	DIALOG_HOME(BillerCategory.INTERNET, "Dialog Home Broadband", "Connection number");

	private final BillerCategory category;
	private final String displayName;
	private final String referenceLabel;

	BillerProvider(BillerCategory category, String displayName, String referenceLabel) {
		this.category = category;
		this.displayName = displayName;
		this.referenceLabel = referenceLabel;
	}

	public BillerCategory getCategory() { return category; }
	public String getDisplayName() { return displayName; }
	public String getReferenceLabel() { return referenceLabel; }
}
