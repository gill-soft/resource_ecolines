package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TariffIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 6914701845642755939L;

	private String id;
	private String discountId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDiscountId() {
		return discountId;
	}

	public void setDiscountId(String discountId) {
		this.discountId = discountId;
	}
	
	@Override
	public TariffIdModel create(String json) {
		return (TariffIdModel) super.create(json);
	}

}
