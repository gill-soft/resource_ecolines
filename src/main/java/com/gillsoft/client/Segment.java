package com.gillsoft.client;

import java.io.Serializable;

public class Segment implements Serializable {

	private static final long serialVersionUID = 4751293526028860467L;
	
	private int origin;
	private int destination;
	
	public int getOrigin() {
		return origin;
	}
	
	public String getStringOrigin() {
		return String.valueOf(origin);
	}

	public int getDestination() {
		return destination;
	}
	
	public String getStringDestination() {
		return String.valueOf(destination);
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}
	
}
