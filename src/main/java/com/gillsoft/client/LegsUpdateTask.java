package com.gillsoft.client;

import java.util.Date;

public class LegsUpdateTask extends WaypointsUpdateTask {

	private static final long serialVersionUID = 4575163882046344179L;
	
	private Date dispatchDate;
	
	public LegsUpdateTask() {
		super();
	}

	public LegsUpdateTask(String journeyId, Date dispatchDate) {
		super(journeyId);
		this.dispatchDate = dispatchDate;
	}

	@Override
	protected String getCacheKey() {
		return RestClient.getLegsCacheKey(journeyId);
	}
	
	@Override
	protected Object getCachedObject(RestClient client) throws ResponseError {
		return client.getLegs(journeyId);
	}
	
	@Override
	protected Date getTimeToLiveDate(Object cached) {
		return dispatchDate;
	}
	
}
