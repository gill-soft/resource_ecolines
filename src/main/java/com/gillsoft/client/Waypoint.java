package com.gillsoft.client;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

public class Waypoint implements Serializable {

	private static final long serialVersionUID = 8311606147723015270L;
	
	private int stop;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	private Date arrival;
	
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	private Date departure;
	
	private String platform;
	private boolean boarding;
	private int line;
	private String carrier;
	private String direction;

	public int getStop() {
		return stop;
	}

	public void setStop(int stop) {
		this.stop = stop;
	}

	public Date getArrival() {
		return arrival;
	}

	public void setArrival(Date arrival) {
		this.arrival = arrival;
	}

	public Date getDeparture() {
		return departure;
	}

	public void setDeparture(Date departure) {
		this.departure = departure;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public boolean isBoarding() {
		return boarding;
	}

	public void setBoarding(boolean boarding) {
		this.boarding = boarding;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public String getCarrier() {
		return carrier;
	}

	public void setCarrier(String carrier) {
		this.carrier = carrier;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

}
