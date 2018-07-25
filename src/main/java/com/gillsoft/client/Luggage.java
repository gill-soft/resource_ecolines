package com.gillsoft.client;

import java.io.Serializable;

public class Luggage implements Serializable {

	private static final long serialVersionUID = 5124141844072455254L;

	private LuggageType type;
	private int maxWeight;
	private String maxSize;
	private int quantity;

	public LuggageType getType() {
		return type;
	}

	public void setType(LuggageType type) {
		this.type = type;
	}

	public int getMaxWeight() {
		return maxWeight;
	}

	public void setMaxWeight(int maxWeight) {
		this.maxWeight = maxWeight;
	}

	public String getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(String maxSize) {
		this.maxSize = maxSize;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

}
