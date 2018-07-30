package com.gillsoft.client;

import java.util.Objects;

public enum TariffType {

	ADULT("1", 19, 59),
	CHILD("10", 0, 12),
	TEEN("14", 13, 18),
	SENIOR("8", 60, null);
	
	private String code;
	private Integer minAge;
	private Integer maxAge;
	
	private TariffType(String code, Integer minAge, Integer maxAge) {
		this.code = code;
		this.minAge = minAge;
		this.maxAge = maxAge;
	}

	public String getCode() {
		return code;
	}

	public Integer getMinAge() {
		return minAge;
	}

	public Integer getMaxAge() {
		return maxAge;
	}
	
	public static TariffType getType(String id) {
		for (TariffType type : values()) {
			if (Objects.equals(id, type.getCode())) {
				return type;
			}
		}
		return null;
	}
	
}


