package com.gillsoft.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.util.ContextProvider;

public class SegmentsUpdateTask implements Runnable, Serializable {

	private static final long serialVersionUID = -4939189315074394001L;

	public SegmentsUpdateTask() {
		
	}

	@Override
	public void run() {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getCacheKey());
		params.put(RedisMemoryCache.IGNORE_AGE, true);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheStationsUpdateDelay());
		
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			Object cachedObject = getCachedObject(client);
			if (cachedObject == null) {
				cachedObject = client.getCache().read(params);
			}
			params.put(RedisMemoryCache.UPDATE_TASK, this);
			client.getCache().write(cachedObject, params);
		} catch (IOCacheException e) {
		}
	}
	
	protected String getCacheKey() {
		return RestClient.SEGMENTS_CACHE_KEY;
	}
	
	protected Object getCachedObject(RestClient client) {
		return client.getSegments();
	}

}
