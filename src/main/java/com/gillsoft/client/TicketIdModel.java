package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TicketIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = -1358783660644439668L;

	private String orderId;
	private String id;
	private int from;
	private int to;
	private String currency;
	
	public TicketIdModel() {
		
	}

	public TicketIdModel(String orderId, String id, int from, int to, String currency) {
		this.orderId = orderId;
		this.id = id;
		this.from = from;
		this.to = to;
		this.currency = currency;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	@Override
	public TicketIdModel create(String json) {
		return (TicketIdModel) super.create(json);
	}
}
