package com.gillsoft.client;

public enum TariffType {

	ADULT("1"),
	CHILD("10"),
	TEEN("14"),
	SENIOR("8");
	
	private String code;
	
	private TariffType(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
	
}


