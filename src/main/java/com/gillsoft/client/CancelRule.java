package com.gillsoft.client;

import java.io.Serializable;
import java.math.BigDecimal;

public class CancelRule implements Serializable {

	private static final long serialVersionUID = 8098272496231806577L;

	private int timeFrom;
	private int timeTill;
	private BigDecimal returnPercent;

	public int getTimeFrom() {
		return timeFrom;
	}

	public void setTimeFrom(int timeFrom) {
		this.timeFrom = timeFrom;
	}

	public int getTimeTill() {
		return timeTill;
	}

	public void setTimeTill(int timeTill) {
		this.timeTill = timeTill;
	}

	public BigDecimal getReturnPercent() {
		return returnPercent;
	}

	public void setReturnPercent(BigDecimal returnPercent) {
		this.returnPercent = returnPercent;
	}

}
