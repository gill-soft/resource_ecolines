package com.gillsoft.client;

import java.io.Serializable;

public class Stop implements Serializable {

	private static final long serialVersionUID = -9057000272926592389L;

	private int id;
	private String title;
	private String description;
	private String state;
	private GeoLocation location;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public GeoLocation getLocation() {
		return location;
	}

	public void setLocation(GeoLocation location) {
		this.location = location;
	}

}
