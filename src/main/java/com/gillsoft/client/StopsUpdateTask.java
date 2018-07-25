package com.gillsoft.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.model.Lang;
import com.gillsoft.util.ContextProvider;

public class StopsUpdateTask implements Runnable, Serializable {

	private static final long serialVersionUID = 798640510151220337L;
	
	private Lang lang;
	
	public StopsUpdateTask() {
		
	}

	public StopsUpdateTask(Lang lang) {
		this.lang = lang;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.getStopsCacheKey(lang));
		params.put(RedisMemoryCache.IGNORE_AGE, true);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheStationsUpdateDelay());
		
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			List<Stop> stops = client.getStops(lang);
			if (stops == null) {
				stops = (List<Stop>) client.getCache().read(params);
			}
			params.put(RedisMemoryCache.UPDATE_TASK, this);
			client.getCache().write(stops, params);
		} catch (IOCacheException e) {
		}
	}

}
