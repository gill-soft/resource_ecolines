package com.gillsoft.client;

public class Fare {

	private int amount;
	private int tariff;
	private Integer discount;
	private Integer limit;
	private boolean allowCancel;
	private boolean allowChangeDate;

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public int getTariff() {
		return tariff;
	}

	public void setTariff(int tariff) {
		this.tariff = tariff;
	}

	public Integer getDiscount() {
		return discount;
	}

	public void setDiscount(Integer discount) {
		this.discount = discount;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public boolean isAllowCancel() {
		return allowCancel;
	}

	public void setAllowCancel(boolean allowCancel) {
		this.allowCancel = allowCancel;
	}

	public boolean isAllowChangeDate() {
		return allowChangeDate;
	}

	public void setAllowChangeDate(boolean allowChangeDate) {
		this.allowChangeDate = allowChangeDate;
	}

}
