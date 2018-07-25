package com.gillsoft.client;

import java.io.Serializable;

public class GeoLocation implements Serializable {

	private static final long serialVersionUID = -4170873759775261581L;
	
	private String latitude;
	private String longitude;

	public String getLatitude() {
		return latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

}
