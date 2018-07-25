package com.gillsoft.client;

import java.io.Serializable;

public class Journey implements Serializable {

	private static final long serialVersionUID = 1769724626912980336L;
	
	private String id;
	private Bound outbound;
	private Bound inbound;
	private int fare;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Bound getOutbound() {
		return outbound;
	}

	public void setOutbound(Bound outbound) {
		this.outbound = outbound;
	}

	public Bound getInbound() {
		return inbound;
	}

	public void setInbound(Bound inbound) {
		this.inbound = inbound;
	}

	public int getFare() {
		return fare;
	}

	public void setFare(int fare) {
		this.fare = fare;
	}

}
