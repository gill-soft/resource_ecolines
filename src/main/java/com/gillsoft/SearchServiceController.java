package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.gillsoft.abstract_rest_service.SimpleAbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.client.Bound;
import com.gillsoft.client.CancelRule;
import com.gillsoft.client.Fare;
import com.gillsoft.client.Journey;
import com.gillsoft.client.Leg;
import com.gillsoft.client.ResponseError;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.TariffIdModel;
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
import com.gillsoft.util.RestTemplateUtil;
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
	public void addInitSearchCallables(List<Callable<SimpleTripSearchPackage<List<Journey>>>> callables, TripSearchRequest request) {
		callables.add(() -> {
			SimpleTripSearchPackage<List<Journey>> searchPackage = new SimpleTripSearchPackage<>();
			searchPackage.setRequest(request);
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
						request.getLocalityPairs().get(0)[1], request.getDates().get(0),
						request.getBackDates() != null && !request.getBackDates().isEmpty() ? request.getBackDates().get(0) : null,
						request.getCurrency());
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
			addInitSearchCallables(callables, result.getRequest());
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
				if (journey.getLegs() != null) {

					Trip trip = new Trip();
					
					if (journey.getInbound() != null) {
						BigDecimal halfFare = new BigDecimal(journey.getFare()).multiply(new BigDecimal("0.005"));
						trip.setId(addSegment(localities, organisations, segments, journey.getId(),
								halfFare, journey.getOutbound(), journey.getLegs().get(0), 1, result.getRequest().getCurrency()));
						trip.setBackId(addSegment(localities, organisations, segments, journey.getId(),
								halfFare, journey.getInbound(), journey.getLegs().get(1), 2, result.getRequest().getCurrency()));
					} else {
						trip.setId(addSegment(localities, organisations, segments, journey.getId(),
								new BigDecimal(journey.getFare()).multiply(new BigDecimal("0.01")),
								journey.getOutbound(), journey.getLegs().get(0), 1, result.getRequest().getCurrency()));
					}
					trips.add(trip);
					
					result.getSearchResult().remove(i);
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
			Map<String, Segment> segments, String journeyId, BigDecimal fare, Bound bound, Leg leg, int part,
			Currency currency) {
		
		TripIdModel idModel = new TripIdModel();
		idModel.setId(journeyId);
		idModel.setLegId(leg.getId());
		idModel.setFrom(bound.getOrigin());
		idModel.setTo(bound.getDestination());
		idModel.setPart(part);
		idModel.setCurrency(client.getCurrency(currency));
		String id = idModel.asString();
		
		Segment segment = new Segment();
		segment.setDepartureDate(leg.getDeparture());
		segment.setArrivalDate(leg.getArrival());
		segment.setDeparture(createLocality(localities, leg.getOrigin()));
		segment.setArrival(createLocality(localities, leg.getDestination()));
		segment.setCarrier(addOrganisation(organisations, bound.getDisplayCarrierTitle()));
		addPrice(fare, bound, segment, idModel.getCurrency());
		
		// добавляем маршрут
		try {
			List<Waypoint> waypoints = client.getCachedWaypoints(journeyId);
			segment.setRoute(createRoute(localities, waypoints, bound.getOrigin(), bound.getDestination()));
		} catch (IOCacheException | ResponseError e) {
		}
		segments.put(id, segment);
		return id;
	}
	
	private Route createRoute(Map<String, Locality> localities, List<Waypoint> waypoints, int from, int to) {
		if (waypoints != null
				&& !waypoints.isEmpty()) {
			
			// получаем первый и последний индекс пунктов маршрута
			int fromIndex = 0;
			int toIndex = 0;
			if (waypoints.get(0).getStop() == from) {
				fromIndex = 0;
				for (int i = 1; i < waypoints.size(); i++) {
					if (waypoints.get(i).getStop() == to) {
						toIndex = i;
						break;
					}
				}
			} else if (waypoints.get(waypoints.size() - 1).getStop() == to) {
				toIndex = waypoints.size() - 1;
				for (int i = waypoints.size() - 1; i > 0; i--) {
					if (waypoints.get(i).getStop() == from) {
						fromIndex = i;
						break;
					}
				}
			}
			Route tripRoute = new Route();
			tripRoute.setNumber(String.valueOf(waypoints.get(0).getLine()));
			List<RoutePoint> path = new ArrayList<>();
			LocalDate first = new LocalDate(DateUtils.truncate(waypoints.get(0).getDeparture(), Calendar.DATE));
			for (int i = fromIndex; i <= toIndex; i++) {
				Waypoint waypoint = waypoints.get(i);
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
				path.add(point);
			}
			tripRoute.setPath(path);
			return tripRoute;
		}
		return null;
	}
	
	private void addPrice(BigDecimal fare, Bound bound, Segment segment, String currCode) {
		
		// тариф
		Tariff tariff = new Tariff();
		TariffIdModel idModel = new TariffIdModel();
		idModel.setId(TariffType.ADULT.getCode());
		tariff.setId(idModel.asString());
		tariff.setValue(fare);
		
		if (bound.getCancelRules() != null) {
			tariff.setReturnConditions(new ArrayList<>(bound.getCancelRules().size()));
			
			// условия возврата
			for (CancelRule rule : bound.getCancelRules()) {
				ReturnCondition condition = new ReturnCondition();
				condition.setMinutesBeforeDepart(rule.getTimeTill());
				condition.setReturnPercent(rule.getReturnPercent());
				tariff.getReturnConditions().add(condition);
			}
		}
		// стоимость
		Price tripPrice = new Price();
		tripPrice.setCurrency(client.getCurrency(currCode));
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
			return createRoute(null, waypoints, idModel.getFrom(), idModel.getTo());
		} catch (IOCacheException | ResponseError e) {
			throw new RestClientException(e.getMessage());
		}
	}

	@Override
	public SeatsScheme getSeatsSchemeResponse(String tripId) {
		TripIdModel idModel = new TripIdModel().create(tripId);
		try {
			List<com.gillsoft.client.Seat> seats = client.getSeats(idModel.getLegId());
			Map<String, String> seatsKeys = new HashMap<>();
			int maxRow = 0;
			int maxCol = 0;
			for (com.gillsoft.client.Seat seat : seats) {
				seatsKeys.put(seat.getColumn() + ";" + seat.getRow(), String.valueOf(seat.getId()));
				if (maxRow < seat.getRow()) {
					maxRow = seat.getRow();
				}
				if (maxCol < seat.getColumn()) {
					maxCol = seat.getColumn();
				}
			}
			SeatsScheme seatsScheme = new SeatsScheme();
			seatsScheme.setScheme(new HashMap<>());
			
			// список ид мест
			// первый list строки, второй - колонки
			List<List<String>> scheme = new ArrayList<>(maxRow);
			for (int i = 1; i <= maxRow; i++) {
				List<String> col = new ArrayList<>(maxCol);
				scheme.add(col);
				for (int j = 1; j <= maxCol; j++) {
					String id = seatsKeys.get(j + ";" + i);
					col.add(id == null ? "" : id);
				}
			}
			seatsScheme.getScheme().put(1, scheme);
			return seatsScheme;
		} catch (ResponseError e) {
			throw new RestClientException(e.getMessage());
		}
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
			throw new RestClientException(e.getMessage());
		}
	}

	@Override
	public List<Tariff> getTariffsResponse(String tripId) {
		TripIdModel idModel = new TripIdModel().create(tripId);
		try {
			List<Fare> fares = client.getFares(idModel.getId(), idModel.getCurrency());
			List<Tariff> tariffs = new ArrayList<>(fares.size());
			for (Fare fare : fares) {
				TariffIdModel tariffIdModel = new TariffIdModel();
				tariffIdModel.setId(String.valueOf(fare.getTariff()));
				tariffIdModel.setDiscountId(String.valueOf(fare.getDiscount()));
				Tariff tariff = new Tariff();
				tariff.setId(tariffIdModel.asString());
				tariff.setCancellable(fare.isAllowCancel());
				tariff.setValue(new BigDecimal(fare.getAmount()).multiply(new BigDecimal("0.01")));
				tariff.setAvailableCount(fare.getLimit());
				
				TariffType type = TariffType.getType(tariff.getId());
				if (type != null) {
					tariff.setMinAge(type.getMinAge());
					tariff.setMaxAge(type.getMaxAge());
				}
				tariffs.add(tariff);
			}
			return tariffs;
		} catch (ResponseError e) {
			throw new RestClientException(e.getMessage());
		}
	}

	@Override
	public List<RequiredField> getRequiredFieldsResponse(String tripId) {
		List<RequiredField> requiredFields = new ArrayList<>();
		requiredFields.add(RequiredField.NAME);
		requiredFields.add(RequiredField.SURNAME);
		requiredFields.add(RequiredField.PHONE);
		requiredFields.add(RequiredField.EMAIL);
		requiredFields.add(RequiredField.TARIFF);
		requiredFields.add(RequiredField.SEAT);
		
		// проверяем есть остановки на территории РФ
		TripIdModel idModel = new TripIdModel().create(tripId);
		try {
			List<Waypoint> waypoints = client.getCachedWaypoints(idModel.getId());
			for (Waypoint waypoint : waypoints) {
				String key = String.valueOf(waypoint.getStop());
				Locality fromDict = LocalityServiceController.getLocality(key);
				if (fromDict != null
						&& Objects.equals("RU", fromDict.getDetails())) {
					requiredFields.add(RequiredField.GENDER);
					requiredFields.add(RequiredField.CITIZENSHIP);
					requiredFields.add(RequiredField.DOCUMENT_TYPE);
					requiredFields.add(RequiredField.DOCUMENT_NUMBER);
					requiredFields.add(RequiredField.DOCUMENT_SERIES);
					requiredFields.add(RequiredField.PATRONYMIC);
				}
			}
		} catch (IOCacheException | ResponseError e) {
			throw new RestClientException(e.getMessage());
		}
		return requiredFields;
	}

	@Override
	public List<Seat> updateSeatsResponse(String tripId, List<Seat> seats) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

	@Override
	public List<ReturnCondition> getConditionsResponse(String tripId, String tariffId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Document> getDocumentsResponse(String tripId) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

}
