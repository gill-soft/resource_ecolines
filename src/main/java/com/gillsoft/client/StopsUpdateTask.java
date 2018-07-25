package com.gillsoft.client;

import com.gillsoft.model.Lang;

public class StopsUpdateTask extends SegmentsUpdateTask {

	private static final long serialVersionUID = 798640510151220337L;
	
	private Lang lang;
	
	public StopsUpdateTask() {
		
	}

	public StopsUpdateTask(Lang lang) {
		this.lang = lang;
	}
	
	@Override
	protected String getCacheKey() {
		return RestClient.getStopsCacheKey(lang);
	}
	
	@Override
	protected Object getCachedObject(RestClient client) {
		return client.getStops(lang);
	}

}
