package com.gillsoft.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gillsoft.model.AbstractJsonModel;

public class OrderIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 4318484251645220464L;

	private List<String> ids;
	private Map<String, List<String>> tickets;

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

	public Map<String, List<String>> getTickets() {
		if (tickets == null) {
			tickets = new HashMap<>();
		}
		return tickets;
	}

	public void setTickets(Map<String, List<String>> tickets) {
		this.tickets = tickets;
	}
	
	public List<String> getTickets(String id) {
		List<String> ids = getTickets().get(id);
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
