package com.gillsoft.client;

import java.util.Date;
import java.util.List;

import com.gillsoft.cache.AbstractUpdateTask;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.model.Currency;
import com.gillsoft.util.ContextProvider;

public class JourneyUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = 6325078282507603948L;
	
	private String fromId;
	private String toId;
	private Date date;
	private Date backDate;
	private Currency currency;
	
	public JourneyUpdateTask() {
		
	}

	public JourneyUpdateTask(String fromId, String toId, Date date, Date backDate, Currency currency) {
		this.fromId = fromId;
		this.toId = toId;
		this.date = date;
		this.backDate = backDate;
		this.currency = currency;
	}

	@Override
	public void run() {

		// получаем рейсы для создания кэша
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			List<Journey> journeys = client.getJourneys(fromId, toId, date, backDate, currency);
			if (journeys != null) {
				for (Journey journey : journeys) {
					
					// для каждого рейса дергаем получение маршрута
					try {
						client.getCachedWaypoints(journey.getId());
					} catch (IOCacheException | ResponseError e) {
					}
					// и сегментов
					try {
						client.getCachedLegs(journey.getId(), journey.getOutbound().getDeparture());
					} catch (IOCacheException | ResponseError e) {
					}
				}
			}
			writeObject(client.getCache(), RestClient.getJourneysCacheKey(fromId, toId, date, backDate, client.getCurrency(currency)),
					journeys, getTimeToLive(journeys), Config.getCacheTripUpdateDelay());
		} catch (ResponseError e) {

			// ошибку поиска тоже кладем в кэш но с другим временем жизни
			writeObject(client.getCache(), RestClient.getJourneysCacheKey(fromId, toId, date, backDate, client.getCurrency(currency)),
					e, Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}
	
	// время жизни до момента самого позднего отправления
	private long getTimeToLive(List<Journey> journeys) throws ResponseError {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		long max = 0;
		for (Journey journey : journeys) {
			if (journey.getOutbound().getDeparture().getTime() > max) {
				max = journey.getOutbound().getDeparture().getTime();
			}
		}
		if (max == 0) {
			throw new ResponseError("Invalid time to live");
		}
		return max - System.currentTimeMillis();
	}

}
