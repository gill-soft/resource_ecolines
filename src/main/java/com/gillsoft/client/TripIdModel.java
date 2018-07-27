package com.gillsoft.client;

import java.util.ArrayList;
import java.util.List;

import com.gillsoft.model.AbstractJsonModel;

public class TripIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 6685617842271023619L;

	private String id;
	private List<String> ids = new ArrayList<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}

	@Override
	public TripIdModel create(String json) {
		return (TripIdModel) super.create(json);
	}

}
