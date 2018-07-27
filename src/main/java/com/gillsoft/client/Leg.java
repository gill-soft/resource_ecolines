package com.gillsoft.client;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public class Leg implements Serializable {

	private static final long serialVersionUID = 2082191507236670442L;

	private String id;
	private int origin;
	private int destination;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	private Date departure;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	private Date arrival;
	
	private int duration;
	private String direction;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

}
