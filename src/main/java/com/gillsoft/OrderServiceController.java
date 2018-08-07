package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractOrderService;
import com.gillsoft.client.Booking;
import com.gillsoft.client.OrderIdModel;
import com.gillsoft.client.Passenger;
import com.gillsoft.client.ResponseError;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.Ticket;
import com.gillsoft.client.TicketIdModel;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Price;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ServiceItem;
import com.gillsoft.model.request.OrderRequest;
import com.gillsoft.model.response.OrderResponse;
import com.gillsoft.util.RestTemplateUtil;
import com.gillsoft.util.StringUtil;

@RestController
public class OrderServiceController extends AbstractOrderService {
	
	@Autowired
	private RestClient client;

	@Override
	public OrderResponse createResponse(OrderRequest request) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();
		response.setCustomers(request.getCustomers());
		
		List<ServiceItem> resultItems = new ArrayList<>();
		
		OrderIdModel orderId = new OrderIdModel();
		for (Entry<String, List<ServiceItem>> order : groupeByTripId(request).entrySet()) {
			try {
				// создаем бронирование
				Booking booking = client.createBooking(order.getKey(), request.getCustomers(), order.getValue());
				
				// получаем бронирование и обновляем стоимость
				List<Ticket> tickets = client.getBooking(booking.getId());
				for (Ticket ticket : tickets) {
					for (ServiceItem item : order.getValue()) {
						if (Objects.equals(ticket.getNote(), item.getCustomer().getId())) {
							item.getPrice().getTariff().setValue(ticket.getPrice().multiply(new BigDecimal("0.01")));
							item.getPrice().setAmount(item.getPrice().getTariff().getValue());
							item.getPrice().setCurrency(Currency.UAH);
							item.setId(new TicketIdModel(booking.getId(), ticket.getId()).asString());
							item.setNumber(ticket.getId());
							orderId.getTickets(booking.getId()).add(ticket.getId());
						}
					}
				}
				orderId.getIds().add(booking.getId());
				resultItems.addAll(order.getValue());
			} catch (ResponseError e) {
				for (ServiceItem item : order.getValue()) {
					item.setError(new RestError(e.getMessage()));
					resultItems.add(item);
				}
			}
		}
		response.setOrderId(orderId.asString());
		response.setServices(resultItems);
		return response;
	}
	
	private Map<String, List<ServiceItem>> groupeByTripId(OrderRequest request) {
		Map<String, List<ServiceItem>> trips = new HashMap<>();
		for (ServiceItem item : request.getServices()) {
			String tripId = item.getSegment().getId();
			List<ServiceItem> items = trips.get(tripId);
			if (items == null) {
				items = new ArrayList<>();
				trips.put(tripId, items);
			}
			if (items.size() == 10) {
				trips.put(String.join(";", tripId, StringUtil.generateUUID()), trips.get(tripId));
				items = new ArrayList<>();
				trips.put(tripId, items);
			}
			items.add(item);
		}
		return trips;
	}

	@Override
	public OrderResponse addServicesResponse(OrderRequest request) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

	@Override
	public OrderResponse removeServicesResponse(OrderRequest request) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

	@Override
	public OrderResponse updateCustomersResponse(OrderRequest request) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

	@Override
	public OrderResponse getResponse(String orderId) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

	@Override
	public OrderResponse getServiceResponse(String serviceId) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

	@Override
	public OrderResponse bookingResponse(String orderId) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

	@Override
	public OrderResponse confirmResponse(String orderId) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();

		// преобразовываем ид заказа в объкт
		OrderIdModel orderIdModel = new OrderIdModel().create(orderId);
		List<ServiceItem> resultItems = new ArrayList<>(orderIdModel.getTickets().size());
		
		// выкупаем заказы и формируем ответ
		for (String id : orderIdModel.getIds()) {
			try {
				client.confirm(id);
				addServiceItems(resultItems, id, orderIdModel.getTickets(id), true, null);
			} catch (ResponseError e) {
				addServiceItems(resultItems, id, orderIdModel.getTickets(id), false, new RestError(e.getMessage()));
			}
		}
		response.setOrderId(orderId);
		response.setServices(resultItems);
		return response;
	}
	
	private void addServiceItems(List<ServiceItem> resultItems, String orderId, List<String> ticketIds,
			boolean confirmed, RestError error) {
		for (String id : ticketIds) {
			ServiceItem serviceItem = new ServiceItem();
			serviceItem.setId(new TicketIdModel(orderId, id).asString());
			serviceItem.setConfirmed(confirmed);
			serviceItem.setError(error);
			resultItems.add(serviceItem);
		}
	}

	@Override
	public OrderResponse cancelResponse(String orderId) {
		// формируем ответ
		OrderResponse response = new OrderResponse();

		// преобразовываем ид заказа в объкт
		OrderIdModel orderIdModel = new OrderIdModel().create(orderId);
		List<ServiceItem> resultItems = new ArrayList<>(orderIdModel.getTickets().size());
		
		// выкупаем заказы и формируем ответ
		for (String id : orderIdModel.getIds()) {
			try {
				// проверяем статус заказа
				List<Ticket> tickets = client.getBooking(id);
				if (RestClient.DELETED.equals(tickets.get(0).getStatus())
						|| RestClient.CANCELED.equals(tickets.get(0).getStatus())) {
					addServiceItems(resultItems, id, orderIdModel.getTickets(id), true, null);
					
				// если статус WAITING, то можно удалять
				} else if (RestClient.WAITING.equals(tickets.get(0).getStatus())) {
					client.delete(id);
					addServiceItems(resultItems, id, orderIdModel.getTickets(id), true, null);
					
				// иначе проверяем возможность 100% возврата
				} else {
					for (String ticketId : orderIdModel.getTickets(id)) {
						List<Passenger> passengers = client.preCancel(id, ticketId);
						for (Passenger passenger : passengers) {
							if (!passenger.isNullifyEnable()
									|| passenger.getReturnAmount().compareTo(passenger.getTotalAmount()) != 0) {
								throw new ResponseError("Cancellation is disabled");
							}
						}
						// возвращаем 100%
						ServiceItem serviceItem = new ServiceItem();
						serviceItem.setId(new TicketIdModel(id, ticketId).asString());
						try {
							client.confirmCancel(id, ticketId);
							serviceItem.setConfirmed(true);
						} catch (ResponseError e) {
							serviceItem.setError(new RestError(e.getMessage()));
						}
						resultItems.add(serviceItem);
					}
				}
			} catch (ResponseError e) {
				addServiceItems(resultItems, id, orderIdModel.getTickets(id), false, new RestError(e.getMessage()));
			}
		}
		response.setOrderId(orderId);
		response.setServices(resultItems);
		return response;
	}

	@Override
	public OrderResponse prepareReturnServicesResponse(OrderRequest request) {
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>(request.getServices().size()));
		for (ServiceItem serviceItem : request.getServices()) {
			TicketIdModel idModel = new TicketIdModel().create(serviceItem.getId());
			try {
				List<Passenger> passengers = client.preCancel(idModel.getOrderId(), idModel.getId());
				if (passengers.isEmpty()
						|| !passengers.get(0).isNullifyEnable()) {
					throw new ResponseError("Return is disabled");
				}
				Price price = new Price();
				price.setCurrency(Currency.UAH);
				price.setAmount(passengers.get(0).getReturnAmount().multiply(new BigDecimal("0.01")));
				serviceItem.setPrice(price);
			} catch (ResponseError e) {
				serviceItem.setError(new RestError(e.getMessage()));
			}
			response.getServices().add(serviceItem);
		}
		return response;
	}

	@Override
	public OrderResponse returnServicesResponse(OrderRequest request) {
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>(request.getServices().size()));
		for (ServiceItem serviceItem : request.getServices()) {
			TicketIdModel idModel = new TicketIdModel().create(serviceItem.getId());
			try {
				client.confirmCancel(idModel.getOrderId(), idModel.getId());
				serviceItem.setConfirmed(true);
			} catch (ResponseError e) {
				serviceItem.setError(new RestError(e.getMessage()));
			}
			response.getServices().add(serviceItem);
		}
		return response;
	}

	@Override
	public OrderResponse getPdfDocumentsResponse(OrderRequest request) {
		throw RestTemplateUtil.createUnavailableMethod();
	}

}
