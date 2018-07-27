package com.gillsoft.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.logging.SimpleRequestResponseLoggingInterceptor;
import com.gillsoft.model.Lang;
import com.gillsoft.util.RestTemplateUtil;
import com.gillsoft.util.StringUtil;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class RestClient {
	
	public static final String STOPS_CACHE_KEY = "ecolines.stops.";
	public static final String SEGMENTS_CACHE_KEY = "ecolines.segments";
	public static final String JOURNEYS_CACHE_KEY = "ecolines.journeys.";
	public static final String WAYPOINTS_CACHE_KEY = "ecolines.waypoints.";
	public static final String LEGS_CACHE_KEY = "ecolines.legs.";
	
	public static final String CURR_ID = "31"; // по договору UAH

	private static final String STOP = "stops";
	private static final String SEGMENT = "segments";
	private static final String JOURNEYS = "journeys";
	private static final String FARES = "fares";
	private static final String WAYPOINTS = "waypoints";
	private static final String SEATS = "seats";
	private static final String LEGS = "legs";
	private static final String BOOKING = "bookings";
	private static final String TICKETS = "bookings/{0}/tickets";
	private static final String DEL_BOOKING = "bookings/{0}";
	private static final String CONFIRM_BOOKING = "bookings/{0}/confirmation";
	private static final String CANCELLATIONS = "bookings/{0}/tickets/{1}/cancellations";
	private static final String CONFIRM_CANCELLATIONS = "bookings/{0}/tickets/{1}/cancellations/1/confirmation";
	
	public static final int CHILD_AGE = 12;
	public static final int TEEN_AGE = 18;
	public static final int ADULT_AGE = 59;
	
	public static final String WAITING = "waiting";
	public static final String CONFIRMED = "confirmed";
	public static final String DELETED = "deleted";
	public static final String CANCELED = "canceled";
	
	public static final String TIME_FORMAT = "HH:mm";
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	
	public final static FastDateFormat timeFormat = FastDateFormat.getInstance(TIME_FORMAT);
	public static final FastDateFormat dateFormat = FastDateFormat.getInstance(DATE_FORMAT);
	
	@Autowired
    @Qualifier("RedisMemoryCache")
	private CacheHandler cache;
	
	private RestTemplate template;
	
	// для запросов поиска с меньшим таймаутом
	private RestTemplate searchTemplate;
	
	private MultiValueMap<String, String> headers;
	
	public RestClient() {
		template = createNewPoolingTemplate(Config.getRequestTimeout());
		searchTemplate = createNewPoolingTemplate(Config.getSearchRequestTimeout());
		headers = createHeaders();
	}
	
	public RestTemplate createNewPoolingTemplate(int requestTimeout) {
		RestTemplate template = new RestTemplate(new BufferingClientHttpRequestFactory(
				RestTemplateUtil.createPoolingFactory(Config.getUrl(), 300, requestTimeout)));
		template.setInterceptors(Collections.singletonList(
				new SimpleRequestResponseLoggingInterceptor()));
		return template;
	}
	
	public List<Stop> getCachedStops(Lang lang) throws IOCacheException {
		try {
			return getCachedObject(getStopsCacheKey(lang), new StopsUpdateTask(lang));
		} catch (ResponseError e) {
			return null;
		}
	}
	
	public List<Stop> getStops(Lang lang) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("locale", getLocale(lang));
		return sendRequest(searchTemplate, STOP, HttpMethod.GET, params, new ParameterizedTypeReference<List<Stop>>() {});
	}
	
	public List<Segment> getCachedSegments() throws IOCacheException {
		try {
			return getCachedObject(SEGMENTS_CACHE_KEY, new SegmentsUpdateTask());
		} catch (ResponseError e) {
			return null;
		}
	}
	
	public List<Segment> getSegments() throws ResponseError {
		return sendRequest(searchTemplate, SEGMENT, HttpMethod.GET, null, new ParameterizedTypeReference<List<Segment>>() {});
	}
	
	public List<Journey> getCachedJourneys(String fromId, String toId, Date date)
			throws IOCacheException, ResponseError {
		return getCachedObject(getJourneysCacheKey(fromId, toId, date), new JourneyUpdateTask(fromId, toId, date));
	}
	
	public List<Journey> getJourneys(String fromId, String toId, Date date) throws ResponseError {
		
		// пока не ищем стыковочных рейсов и раундтрипов
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("outboundOrigin", fromId);
		params.add("outboundDestination", toId);
		params.add("outboundDate", dateFormat.format(date));
		params.add("currency", CURR_ID);
		params.add("adults", "1");
		params.add("applyDiscounts", "0");
		params.add("directOnly", "1");
		return sendRequest(searchTemplate, JOURNEYS, HttpMethod.GET, params, new ParameterizedTypeReference<List<Journey>>() {});
	}
	
	public List<Waypoint> getCachedWaypoints(String journeyId)
			throws IOCacheException, ResponseError {
		return getCachedObject(getWaypointsCacheKey(journeyId), new WaypointsUpdateTask(journeyId));
	}
	
	public List<Waypoint> getWaypoints(String journeyId) throws ResponseError {
		return getJourneyDetails(WAYPOINTS, journeyId, new ParameterizedTypeReference<List<Waypoint>>() {});
	}
	
	public List<Leg> getCachedLegs(String journeyId, Date dispatchDate)
			throws IOCacheException, ResponseError {
		return getCachedObject(getLegsCacheKey(journeyId), new LegsUpdateTask(journeyId, dispatchDate));
	}
	
	public List<Leg> getLegs(String journeyId) throws ResponseError {
		return getJourneyDetails(LEGS, journeyId, new ParameterizedTypeReference<List<Leg>>() {});
	}
	
	public List<Seat> getSeats(String leg) throws ResponseError {
		return getJourneyDetails(SEATS, "leg", leg, new ParameterizedTypeReference<List<Seat>>() {}); 
	}
	
	private <T> T getJourneyDetails(String method, String journeyId, ParameterizedTypeReference<T> typeReference)
			throws ResponseError {
		return getJourneyDetails(method, "journey", journeyId, typeReference);
	}
	
	private <T> T getJourneyDetails(String method, String paramKey, String paramValue, ParameterizedTypeReference<T> typeReference)
			throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add(paramKey, paramValue);
		return sendRequest(searchTemplate, method, HttpMethod.GET, params, typeReference);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getCachedObject(String key, Runnable task) throws IOCacheException, ResponseError {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, key);
		params.put(RedisMemoryCache.UPDATE_TASK, task);
		Object cached = cache.read(params);
		if (cached == null) {
			return null;
		}
		if (cached instanceof ResponseError) {
			throw (ResponseError) cached;
		}
		return (T) cached;
	}
	
	private <T> T sendRequest(RestTemplate template, String uriMethod, HttpMethod httpMethod,
			MultiValueMap<String, String> params,  ParameterizedTypeReference<T> typeReference) throws ResponseError {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl() + uriMethod).queryParams(params).build().toUri();
		RequestEntity<Object> requestEntity = new RequestEntity<>(headers, httpMethod, uri);
		try {
			ResponseEntity<T> response = searchTemplate.exchange(requestEntity, typeReference);
			return response.getBody();
		} catch (RestClientException e) {
			throw new ResponseError(e.getMessage());
		}
	}
	
	private MultiValueMap<String, String> createHeaders() {
		MultiValueMap<String, String> headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
		headers.add(HttpHeaders.AUTHORIZATION, "Basic " + StringUtil.toBase64(
				(Config.getLogin() + ":" + Config.getPassword()).getBytes(StandardCharsets.ISO_8859_1)));
		return headers;
	}
	
	public CacheHandler getCache() {
		return cache;
	}
	
	private String getLocale(Lang lang) {
		if (lang == null) {
			return "en";
		}
		switch (lang) {
		case UA:
			return "uk";
		case RU:
			return "ru";
		case PL:
			return "pl";
		case EN:
			return "en";
		default:
			return "en";
		}
	}
	
	public static String getStopsCacheKey(Lang lang) {
		return STOPS_CACHE_KEY + lang.toString();
	}
	
	public static String getWaypointsCacheKey(String journeyId) {
		return WAYPOINTS_CACHE_KEY + journeyId;
	}
	
	public static String getLegsCacheKey(String journeyId) {
		return LEGS_CACHE_KEY + journeyId;
	}
	
	public static String getJourneysCacheKey(String from, String to, Date date) {
		return String.join(".", from, to, dateFormat.format(date));
	}

}
