package com.gillsoft.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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
	
	private static final String DATE_FORMAT = "yyyy-MM-dd";
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
	
	@SuppressWarnings("unchecked")
	public List<Stop> getCachedStops(Lang lang) throws IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getStopsCacheKey(lang));
		params.put(RedisMemoryCache.UPDATE_TASK, new StopsUpdateTask(lang));
		return (List<Stop>) cache.read(params);
	}
	
	public List<Stop> getStops(Lang lang) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("locale", getLocale(lang));
		return sendRequest(searchTemplate, STOP, HttpMethod.GET, params, new ParameterizedTypeReference<List<Stop>>() {});
	}
	
	@SuppressWarnings("unchecked")
	public List<Segment> getCachedSegments() throws IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, SEGMENTS_CACHE_KEY);
		params.put(RedisMemoryCache.UPDATE_TASK, new SegmentsUpdateTask());
		return (List<Segment>) cache.read(params);
	}
	
	public List<Segment> getSegments() {
		return sendRequest(searchTemplate, SEGMENT, HttpMethod.GET, null, new ParameterizedTypeReference<List<Segment>>() {});
	}
	
	public List<Leg> getLegs(String fromId, String toId, Date date) {
		Map<String, String> params = new HashMap<>();
		params.put("outboundOrigin", fromId);
		params.put("outboundDestination", toId);
		params.put("outboundDate", dateFormat.format(date));
		params.put("currency", CURR_ID);
		params.put("adults", "1");
		params.put("applyDiscounts", "0");
		return null;
	}
	
	private <T> T sendRequest(RestTemplate template, String uriMethod, HttpMethod httpMethod,
			MultiValueMap<String, String> params,  ParameterizedTypeReference<T> typeReference) {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl() + uriMethod).queryParams(params).build().toUri();
		RequestEntity<Object> requestEntity = new RequestEntity<>(headers, httpMethod, uri);
		ResponseEntity<T> response = searchTemplate.exchange(requestEntity, typeReference);
		return response.getBody();
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

	public static void main(String[] args) {
		RestClient client = new RestClient();
		System.out.println(client.getStops(null).size());
	}

}
