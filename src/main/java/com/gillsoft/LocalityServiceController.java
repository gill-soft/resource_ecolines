package com.gillsoft;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractLocalityService;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.Stop;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.request.LocalityRequest;
import com.google.common.base.Objects;

@RestController
public class LocalityServiceController extends AbstractLocalityService {
	
	public static List<Locality> all;
	
	@Autowired
	private RestClient client;

	@Override
	public List<Locality> getAllResponse(LocalityRequest request) {
		createLocalities();
		return all;
	}

	@Override
	public Map<String, List<String>> getBindingResponse(LocalityRequest request) {
		return null;
	}

	@Override
	public List<Locality> getUsedResponse(LocalityRequest request) {
		createLocalities();
		return all;
	}
	
	@Scheduled(initialDelay = 60000, fixedDelay = 900000)
	public void createLocalities() {
		if (LocalityServiceController.all == null) {
			synchronized (LocalityServiceController.class) {
				if (LocalityServiceController.all == null) {
					Map<String, Locality> localities = new HashMap<>();
					for (Lang lang : Lang.values()) {
						addLocalities(lang, localities);
					}
					List<Locality> all = new CopyOnWriteArrayList<>();
					all.addAll(localities.values());
					LocalityServiceController.all = all;
				}
			}
		}
	}
	
	private void addLocalities(Lang lang, Map<String, Locality> localities) {
		boolean cacheError = true;
		do {
			try {
				List<Stop> stops = client.getCachedStops(lang);
				if (stops != null) {
					List<Locality> all = new CopyOnWriteArrayList<>();
					for (Stop stop : stops) {
						String key = String.valueOf(stop.getId());
						Locality locality = localities.get(key);
						if (locality == null) {
							locality = new Locality();
							locality.setId(key);
							localities.put(key, locality);
							locality.setDetails(stop.getState());
						}
						locality.setName(lang, stop.getTitle());
						locality.setAddress(lang, stop.getDescription());
						locality.setLatitude(getDecimal(stop.getLocation().getLatitude()));
						locality.setLongitude(getDecimal(stop.getLocation().getLongitude()));
						all.add(locality);
					}
				}
				cacheError = false;
			} catch (IOCacheException e) {
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException ie) {
				}
			}
		} while (cacheError);
	}
	
	private BigDecimal getDecimal(String value) {
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public static Locality getLocality(String id) {
		if (all == null) {
			return null;
		}
		for (Locality locality : all) {
			if (Objects.equal(id, locality.getId())) {
				return locality;
			}
		}
		return null;
	}

}
