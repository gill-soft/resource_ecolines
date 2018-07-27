package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.SimpleAbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.client.CancelRule;
import com.gillsoft.client.Journey;
import com.gillsoft.client.Leg;
import com.gillsoft.client.ResponseError;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.TariffType;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.client.Waypoint;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Document;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.Price;
import com.gillsoft.model.RequiredField;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ReturnCondition;
import com.gillsoft.model.Route;
import com.gillsoft.model.RoutePoint;
import com.gillsoft.model.Seat;
import com.gillsoft.model.SeatStatus;
import com.gillsoft.model.SeatType;
import com.gillsoft.model.SeatsScheme;
import com.gillsoft.model.Segment;
import com.gillsoft.model.SimpleTripSearchPackage;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.Trip;
import com.gillsoft.model.TripContainer;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;
import com.gillsoft.util.StringUtil;

@RestController
public class SearchServiceController extends SimpleAbstractTripSearchService<SimpleTripSearchPackage<List<Journey>>> {
	
	@Autowired
	private RestClient client;
	
	@Autowired
	@Qualifier("MemoryCacheHandler")
	private CacheHandler cache;

	@Override
	public TripSearchResponse initSearchResponse(TripSearchRequest request) {
		return simpleInitSearchResponse(cache, request);
	}
	
	@Override
	public void addInitSearchCallables(List<Callable<SimpleTripSearchPackage<List<Journey>>>> callables, String[] pair,
			Date date) {
		callables.add(() -> {
			SimpleTripSearchPackage<List<Journey>> searchPackage = new SimpleTripSearchPackage<>();
			searchPackage.setRequest(TripSearchRequest.createRequest(pair, date));
			searchJourneys(searchPackage);
			return searchPackage;
		});
	}
	
	private void searchJourneys(SimpleTripSearchPackage<List<Journey>> searchPackage) {
		searchPackage.setInProgress(false);
		try {
			TripSearchRequest request = searchPackage.getRequest();
			List<Journey> journeys = searchPackage.getSearchResult();
			if (journeys == null) {
				journeys = client.getCachedJourneys(request.getLocalityPairs().get(0)[0],
						request.getLocalityPairs().get(0)[1], request.getDates().get(0));
				searchPackage.setSearchResult(new CopyOnWriteArrayList<Journey>());
				searchPackage.getSearchResult().addAll(journeys);
			}
			for (Journey journey : journeys) {
				if (journey.getLegs() == null) {
					try {
						journey.setLegs(client.getCachedLegs(journey.getId(), journey.getOutbound().getDeparture()));
					} catch (IOCacheException e) {
						searchPackage.setInProgress(true);
					}
				}
			}
		} catch (IOCacheException e) {
			searchPackage.setInProgress(true);
		} catch (ResponseError e) {
			searchPackage.setException(e);
		}
	}

	@Override
	public TripSearchResponse getSearchResultResponse(String searchId) {
		return simpleGetSearchResponse(cache, searchId);
	}
	
	@Override
	public void addNextGetSearchCallablesAndResult(List<Callable<SimpleTripSearchPackage<List<Journey>>>> callables,
			Map<String, Vehicle> vehicles, Map<String, Locality> localities, Map<String, Organisation> organisations,
			Map<String, Segment> segments, List<TripContainer> containers,
			SimpleTripSearchPackage<List<Journey>> result) {
		if (!result.isInProgress()) {
			addResult(localities, organisations, segments, containers, result);
		} else {
			callables.add(() -> {
				searchJourneys(result);
				return result;
			});
		}
	}
	
	private void addResult(Map<String, Locality> localities, Map<String, Organisation> organisations,
			Map<String, Segment> segments, List<TripContainer> containers,
			SimpleTripSearchPackage<List<Journey>> result) {
		TripContainer container = new TripContainer();
		container.setRequest(result.getRequest());
		if (result.getSearchResult() != null) {
			List<Trip> trips = new ArrayList<>();
			for (int i = result.getSearchResult().size() - 1; i >= 0; i--) {
				Journey journey = result.getSearchResult().get(i);
				if (journey.getLegs() != null
						
						// только прямые рейсы
						&& journey.getLegs().size() == 1) {

					Trip trip = new Trip();
					trip.setId(addSegment(localities, organisations, segments, journey, journey.getLegs().get(0)));
					trips.add(trip);
				}
			}
			container.setTrips(trips);
		}
		if (result.getException() != null) {
			container.setError(new RestError(result.getException().getMessage()));
		}
		containers.add(container);
	}
	
	private String addSegment(Map<String, Locality> localities, Map<String, Organisation> organisations,
			Map<String, Segment> segments, Journey journey, Leg leg) {
		
		TripIdModel idModel = new TripIdModel();
		idModel.setId(journey.getId());
		idModel.setLegId(leg.getId());
		String id = idModel.asString();
		
		Segment segment = new Segment();
		segment.setDepartureDate(leg.getDeparture());
		segment.setArrivalDate(leg.getArrival());
		segment.setDeparture(createLocality(localities, leg.getOrigin()));
		segment.setArrival(createLocality(localities, leg.getDestination()));
		segment.setCarrier(addOrganisation(organisations, journey.getOutbound().getDisplayCarrierTitle()));
		addPrice(journey, segment);
		
		// добавляем маршрут
		try {
			List<Waypoint> waypoints = client.getCachedWaypoints(journey.getId());
			segment.setRoute(createRoute(localities, waypoints));
		} catch (IOCacheException | ResponseError e) {
		}
		segments.put(id, segment);
		return id;
	}
	
	private Route createRoute(Map<String, Locality> localities, List<Waypoint> waypoints) {
		if (waypoints != null
				&& !waypoints.isEmpty()) {
			Route tripRoute = new Route();
			tripRoute.setNumber(String.valueOf(waypoints.get(0).getLine()));
			tripRoute.setPath(new ArrayList<>());
			LocalDate first = new LocalDate(DateUtils.truncate(waypoints.get(0).getDeparture(), Calendar.DATE));
			for (Waypoint waypoint : waypoints) {
				RoutePoint point = new RoutePoint();
				point.setLocality(createLocality(localities, waypoint.getStop()));
				if (waypoint.getArrival() != null) {
					point.setArrivalDay(Days.daysBetween(first,
							new LocalDate(waypoint.getArrival())).getDays());
					point.setArrivalTime(RestClient.timeFormat.format(waypoint.getArrival()));
				}
				if (waypoint.getDeparture() != null) {
					point.setDepartureTime(RestClient.timeFormat.format(waypoint.getDeparture()));
				}
				point.setPlatform(waypoint.getPlatform());
				tripRoute.getPath().add(point);
			}
			return tripRoute;
		}
		return null;
	}
	
	private void addPrice(Journey journey, Segment segment) {
		
		// тариф
		Tariff tariff = new Tariff();
		tariff.setId(TariffType.ADULT.getCode());
		tariff.setValue(new BigDecimal(journey.getFare()).multiply(new BigDecimal("0.01")));
		
		if (journey.getOutbound().getCancelRules() != null) {
			tariff.setReturnConditions(new ArrayList<>(journey.getOutbound().getCancelRules().size()));
			
			// условия возврата
			for (CancelRule rule : journey.getOutbound().getCancelRules()) {
				ReturnCondition condition = new ReturnCondition();
				condition.setMinutesBeforeDepart(rule.getTimeTill());
				condition.setReturnPercent(rule.getReturnPercent());
				tariff.getReturnConditions().add(condition);
			}
		}
		// стоимость
		Price tripPrice = new Price();
		tripPrice.setCurrency(Currency.UAH);
		tripPrice.setAmount(tariff.getValue());
		tripPrice.setTariff(tariff);
		
		segment.setPrice(tripPrice);
	}
	
	public Organisation addOrganisation(Map<String, Organisation> organisations, String name) {
		if (name == null) {
			return null;
		}
		String key = StringUtil.md5(name);
		Organisation organisation = organisations.get(key);
		if (organisation == null) {
			organisation = new Organisation();
			organisation.setName(Lang.UA, name);
			organisations.put(key, organisation);
		}
		return new Organisation(key);
	}
	
	private Locality createLocality(Map<String, Locality> localities, int id) {
		String key = String.valueOf(id);
		Locality fromDict = LocalityServiceController.getLocality(key);
		if (fromDict == null) {
			return null;
		}
		if (localities == null) {
			return fromDict;
		}
		String fromDictId = fromDict.getId();
		try {
			fromDict = fromDict.clone();
			fromDict.setId(null);
		} catch (CloneNotSupportedException e) {
		}
		Locality locality = localities.get(fromDictId);
		if (locality == null) {
			localities.put(fromDictId, fromDict);
		}
		return new Locality(fromDictId);
	}

	@Override
	public Route getRouteResponse(String tripId) {
		TripIdModel idModel = new TripIdModel().create(tripId);
		try {
			List<Waypoint> waypoints = client.getCachedWaypoints(idModel.getId());
			return createRoute(null, waypoints);
		} catch (IOCacheException | ResponseError e) {
			return null;
		}
	}

	@Override
	public SeatsScheme getSeatsSchemeResponse(String tripId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Seat> getSeatsResponse(String tripId) {
		TripIdModel idModel = new TripIdModel().create(tripId);
		try {
			List<com.gillsoft.client.Seat> seats = client.getSeats(idModel.getLegId());
			List<Seat> newSeats = new ArrayList<>(seats.size());
			for (com.gillsoft.client.Seat seat : seats) {
				Seat newSeat = new Seat();
				newSeat.setType(seat.getId() != 0 ? SeatType.SEAT : SeatType.FLOOR);
				if (seat.getId() != 0) {
					newSeat.setId(String.valueOf(seat.getId()));
					newSeat.setNumber(newSeat.getId());
					newSeat.setStatus(seat.isBusy() ? SeatStatus.SALED : SeatStatus.FREE);
				}
				newSeats.add(newSeat);
			}
			return newSeats;
		} catch (ResponseError e) {
		}
		return null;
	}

	@Override
	public List<Tariff> getTariffsResponse(String tripId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RequiredField> getRequiredFieldsResponse(String tripId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Seat> updateSeatsResponse(String tripId, List<Seat> seats) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ReturnCondition> getConditionsResponse(String tripId, String tariffId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Document> getDocumentsResponse(String tripId) {
		// TODO Auto-generated method stub
		return null;
	}

}
