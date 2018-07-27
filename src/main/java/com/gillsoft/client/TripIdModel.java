package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TripIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 6685617842271023619L;

	private String id;
	private String legId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLegId() {
		return legId;
	}

	public void setLegId(String legId) {
		this.legId = legId;
	}

	@Override
	public TripIdModel create(String json) {
		return (TripIdModel) super.create(json);
	}

}
