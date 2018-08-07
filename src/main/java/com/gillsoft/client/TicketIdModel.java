package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TicketIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = -1358783660644439668L;

	private String orderId;
	private String id;
	
	public TicketIdModel() {
		
	}

	public TicketIdModel(String orderId, String id) {
		this.orderId = orderId;
		this.id = id;
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

	@Override
	public TicketIdModel create(String json) {
		return (TicketIdModel) super.create(json);
	}
}
