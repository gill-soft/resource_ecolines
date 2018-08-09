package com.gillsoft.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gillsoft.model.AbstractJsonModel;

public class OrderIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 4318484251645220464L;

	private List<String> ids;
	private Map<String, List<TicketIdModel>> tickets;

	public OrderIdModel() {
		
	}

	public List<String> getIds() {
		if (ids == null) {
			ids = new ArrayList<>();
		}
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}

	public Map<String, List<TicketIdModel>> getTickets() {
		if (tickets == null) {
			tickets = new HashMap<>();
		}
		return tickets;
	}

	public void setTickets(Map<String, List<TicketIdModel>> tickets) {
		this.tickets = tickets;
	}
	
	public List<TicketIdModel> getTickets(String id) {
		List<TicketIdModel> ids = getTickets().get(id);
		if (ids == null) {
			ids = new ArrayList<>();
			getTickets().put(id, ids);
		}
		return ids;
	}

	@Override
	public OrderIdModel create(String json) {
		return (OrderIdModel) super.create(json);
	}

}
