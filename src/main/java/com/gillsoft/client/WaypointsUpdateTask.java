package com.gillsoft.client;

import java.util.Date;
import java.util.List;

import com.gillsoft.cache.AbstractUpdateTask;
import com.gillsoft.util.ContextProvider;

public class WaypointsUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = -1203046250280412592L;
	
	protected String journeyId;
	
	public WaypointsUpdateTask() {
		
	}
	
	public WaypointsUpdateTask(String journeyId) {
		this.journeyId = journeyId;
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			Object cached = getCachedObject(client);
			writeObject(client.getCache(), getCacheKey(), cached,
					getTimeToLive(getTimeToLiveDate(cached)), Config.getCacheRouteUpdateDelay());
		} catch (ResponseError e) {

			// ошибку поиска тоже кладем в кэш но с другим временем жизни
			writeObject(client.getCache(), getCacheKey(), e,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Date getTimeToLiveDate(Object cached) throws ResponseError {
		List<Waypoint> waypoints = (List<Waypoint>) cached;
		Date max = null;
		for (Waypoint waypoint : waypoints) {
			if (max == null
					|| (waypoint.getArrival() != null
						&& max.getTime() < waypoint.getArrival().getTime())) {
				max = waypoint.getArrival();
			}
		}
		if (max == null) {
			throw new ResponseError("Invalid time to live");
		}
		return max;
	}
	
	protected String getCacheKey() {
		return RestClient.getWaypointsCacheKey(journeyId);
	}
	
	protected Object getCachedObject(RestClient client) throws ResponseError {
		return client.getWaypoints(journeyId);
	}
	
	protected long getTimeToLive(Date date) {
		if (Config.getCacheRouteTimeToLive() != 0) {
			return Config.getCacheRouteTimeToLive();
		}
		return date.getTime() - System.currentTimeMillis();
	}

}
