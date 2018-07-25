package com.gillsoft.client;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.util.ContextProvider;

public class JourneyUpdateTask implements Runnable, Serializable {

	private static final long serialVersionUID = 6325078282507603948L;
	
	private String fromId;
	private String toId;
	private Date date;
	
	public JourneyUpdateTask() {
		
	}

	public JourneyUpdateTask(String fromId, String toId, Date date) {
		this.fromId = fromId;
		this.toId = toId;
		this.date = date;
	}

	@Override
	public void run() {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.getJourneysCacheKey(fromId, toId, date));
		params.put(RedisMemoryCache.UPDATE_TASK, this);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheTripUpdateDelay());

		// получаем рейсы для создания кэша
		RestClient client = ContextProvider.getBean(RestClient.class);
		Object cachedObject = null;
		try {
			List<Journey> journeys = client.getJourneys(fromId, toId, date);
			params.put(RedisMemoryCache.TIME_TO_LIVE, getTimeToLive(journeys));
			cachedObject = journeys;
		} catch (ResponseError e) {

			// ошибку поиска тоже кладем в кэш но с другим временем жизни
			params.put(RedisMemoryCache.TIME_TO_LIVE, Config.getCacheErrorTimeToLive());
			params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheErrorUpdateDelay());
			cachedObject = e;
		}
		try {
			client.getCache().write(cachedObject, params);
		} catch (IOCacheException e) {
		}
	}
	
	// время жизни до момента самого позднего отправления
	private long getTimeToLive(List<Journey> journeys) {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		long max = 0;
		for (Journey journey : journeys) {
			if (journey.getOutbound().getDeparture().getTime() > max) {
				max = journey.getOutbound().getDeparture().getTime();
			}
		}
		return max - System.currentTimeMillis();
	}

}
