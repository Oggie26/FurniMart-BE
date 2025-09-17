package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderDetail;
import com.example.orderservice.entity.ProcessOrder;
import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.event.OrderPlacedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.repository.OrderDetailRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.ProcessOrderRepository;
import com.example.orderservice.request.OrderRequest;
import com.example.orderservice.response.OrderDetailResponse;
import com.example.orderservice.response.OrderResponse;
import com.example.orderservice.response.ProcessOrderResponse;
import com.example.orderservice.service.inteface.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProcessOrderRepository processOrderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Override
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setUserId(orderRequest.getUserId());
        order.setStoreId(orderRequest.getStoreId());
        order.setAddressId(orderRequest.getAddressId());
        order.setQuantity(orderRequest.getQuantity());
        order.setTotal(orderRequest.getTotal());
        order.setNote(orderRequest.getNote());
        order.setOrderDate(new Date());

        List<OrderDetail> details = orderRequest.getOrderDetails().stream().map(req ->
                new OrderDetail(null, req.getProductId(), req.getQuantity(), req.getPrice(), order)
        ).toList();
        order.setOrderDetails(details);

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.PENDING);
        process.setCreatedAt(new Date());

        order.setProcessOrders(List.of(process));

        orderRepository.save(order);
        orderDetailRepository.saveAll(details);
        processOrderRepository.save(process);

        return mapToResponse(order);
    }

//    @Override
//    public PageResponse<OrderResponse> searchOrdersByCustomer(String userId, int page, int size) {
//        Page<Order> orders = orderRepository.findByUserId(userId, PageRequest.of(page, size));
//        List<OrderResponse> data = orders.getContent().stream().map(this::mapToResponse).toList();
//
//
//    }
//
//    @Override
//    public PageResponse searchOrdersByManager(String request, String storeId, int page, int size) {
//        PageRequest pageable = PageRequest.of(page, size);
//        Page<Order> orderPage = orderRepository.searchByKeywordNative(keyword, pageable);
//
//        List<OrderResponse> data = orderPage.getContent().stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//
//        return new PageResponse<>(
//                data,
//                orderPage.getNumber(),
//                orderPage.getSize(),
//                orderPage.getTotalElements(),
//                orderPage.getTotalPages()
//        );
//
//    }


    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->  new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToResponse(order);
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() ->  new AppException(ErrorCode.ORDER_NOT_FOUND));

        ProcessOrder process = new ProcessOrder();
        process.setOrder(order);
        process.setStatus(EnumProcessOrder.valueOf(status));
        process.setCreatedAt(new Date());

        order.getProcessOrders().add(process);

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .addressId(order.getAddressId())
                .quantity(order.getQuantity())
                .total(order.getTotal())
                .note(order.getNote())
                .orderDate(order.getOrderDate())
                .orderDetails(
                        order.getOrderDetails() != null
                                ? order.getOrderDetails().stream()
                                .map(detail -> OrderDetailResponse.builder()
                                        .id(detail.getId())
                                        .productId(detail.getProductId())
                                        .quantity(detail.getQuantity())
                                        .price(detail.getPrice())
                                        .build())
                                .toList()
                                : List.of()
                )
                .processOrders(
                        order.getProcessOrders() != null
                                ? order.getProcessOrders().stream()
                                .map(process -> ProcessOrderResponse.builder()
                                        .id(process.getId())
                                        .status(process.getStatus())
                                        .createdAt(process.getCreatedAt())
                                        .build())
                                .toList()
                                : List.of()
                )
                .build();
    }
}
