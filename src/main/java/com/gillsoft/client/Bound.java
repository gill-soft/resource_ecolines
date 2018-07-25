package com.gillsoft.client;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public class Bound implements Serializable {

	private static final long serialVersionUID = 1663177689490805247L;
	
	private int origin;
	private int destination;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	private Date departure;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	private Date arrival;
	
	private int changes;
	private int duration;
	private String displayCarrierTitle;
	private String displayCarrierRulesUrl;
	private List<Luggage> luggage;
	private List<CancelRule> cancelRules;

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public int getDestination() {
		return destination;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}

	public Date getDeparture() {
		return departure;
	}

	public void setDeparture(Date departure) {
		this.departure = departure;
	}

	public Date getArrival() {
		return arrival;
	}

	public void setArrival(Date arrival) {
		this.arrival = arrival;
	}

	public int getChanges() {
		return changes;
	}

	public void setChanges(int changes) {
		this.changes = changes;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getDisplayCarrierTitle() {
		return displayCarrierTitle;
	}

	public void setDisplayCarrierTitle(String displayCarrierTitle) {
		this.displayCarrierTitle = displayCarrierTitle;
	}

	public String getDisplayCarrierRulesUrl() {
		return displayCarrierRulesUrl;
	}

	public void setDisplayCarrierRulesUrl(String displayCarrierRulesUrl) {
		this.displayCarrierRulesUrl = displayCarrierRulesUrl;
	}

	public List<Luggage> getLuggage() {
		return luggage;
	}

	public void setLuggage(List<Luggage> luggage) {
		this.luggage = luggage;
	}

	public List<CancelRule> getCancelRules() {
		return cancelRules;
	}

	public void setCancelRules(List<CancelRule> cancelRules) {
		this.cancelRules = cancelRules;
	}

}
