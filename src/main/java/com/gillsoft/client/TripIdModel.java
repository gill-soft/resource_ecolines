package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TripIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 6685617842271023619L;
	
	private String id;
	private String legId;
	private int from;
	private int to;
	private int part; // 0 - когда рейс в одну сторону и без пересадки, 1 .. N - номер части поездки

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

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public int getPart() {
		return part;
	}

	public void setPart(int part) {
		this.part = part;
	}

	@Override
	public TripIdModel create(String json) {
		return (TripIdModel) super.create(json);
	}

}
