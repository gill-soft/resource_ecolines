package com.gillsoft.client;

public enum EcolinesTariff {

	ADULT(1),
	CHILD(10),
	TEEN(14),
	SENIOR(8);
	
	private int tariff;
	
	private EcolinesTariff(int tariff) {
		this.tariff = tariff;
	}

	public int getTariff() {
		return tariff;
	}
	
}


