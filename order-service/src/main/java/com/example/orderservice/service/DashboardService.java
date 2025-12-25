package com.example.orderservice.service;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.feign.DeliveryClient;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderDetailRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.response.*;
import com.example.orderservice.response.StaffDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StoreClient storeClient;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final DeliveryClient deliveryClient;

    // Statuses that count as completed/revenue-generating orders
    private static final List<EnumProcessOrder> COMPLETED_STATUSES = Arrays.asList(
            EnumProcessOrder.DELIVERED,
            EnumProcessOrder.FINISHED
    );

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        log.info("Getting admin dashboard data");

        // 1. Total Revenue
        Double totalRevenue = orderRepository.getTotalRevenueByStatuses(COMPLETED_STATUSES);
        if (totalRevenue == null) totalRevenue = 0.0;

        // 2. Total Active Stores
        Long totalActiveStores = 0L;
        try {
            ApiResponse<Long> storesResponse = storeClient.getActiveStoresCount();
            if (storesResponse != null && storesResponse.getData() != null) {
                totalActiveStores = storesResponse.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching active stores count: {}", e.getMessage());
        }

        // 3. Total Users
        Long totalUsers = 0L;
        try {
            ApiResponse<Long> usersResponse = userClient.getTotalUsersCount();
            if (usersResponse != null && usersResponse.getData() != null) {
                totalUsers = usersResponse.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching users count: {}", e.getMessage());
        }

        // 4. Top Products
        List<TopProductResponse> topProducts = getTopProducts(10);

        // 5. Revenue Chart (last 30 days)
        List<RevenueChartData> revenueChart = getRevenueChart(30);

        return AdminDashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .totalActiveStores(totalActiveStores)
                .totalUsers(totalUsers)
                .topProducts(topProducts)
                .revenueChart(revenueChart)
                .build();
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(String storeId) {
        log.info("Getting manager dashboard data for store: {}", storeId);

        // 1. Branch Revenue
        Double branchRevenue = orderRepository.getTotalRevenueByStoreAndStatuses(storeId, COMPLETED_STATUSES);
        if (branchRevenue == null) branchRevenue = 0.0;

        // 2. Pending Orders Count
        Long pendingOrdersCount = orderRepository.countOrdersByStoreAndStatus(storeId, EnumProcessOrder.PAYMENT);

        // 3. Shipping Orders Count
        Long shippingOrdersCount = orderRepository.countOrdersByStoreAndStatus(storeId, EnumProcessOrder.SHIPPING);

        // 4. Low Stock Products
        List<LowStockProductResponse> lowStockProducts = getLowStockProducts(storeId, 10);

        // 5. Orders for Shipper
        List<OrderForShipperResponse> ordersForShipper = getOrdersForShipper(storeId);

        return ManagerDashboardResponse.builder()
                .branchRevenue(branchRevenue)
                .pendingOrdersCount(pendingOrdersCount)
                .shippingOrdersCount(shippingOrdersCount)
                .lowStockProducts(lowStockProducts)
                .ordersForShipper(ordersForShipper)
                .build();
    }

    private List<TopProductResponse> getTopProducts(int limit) {
        try {
            List<Object[]> results = orderDetailRepository.getTopProductsBySales(COMPLETED_STATUSES);
            
            return results.stream()
                    .limit(limit)
                    .map(result -> {
                        String productColorId = (String) result[0];
                        Long totalQuantity = ((Number) result[1]).longValue();
                        Double totalRevenue = ((Number) result[2]).doubleValue();

                        // Get product details
                        String productName = "N/A";
                        String colorName = "N/A";
                        try {
                            ApiResponse<ProductColorResponse> productResponse = productClient.getProductColor(productColorId);
                            if (productResponse != null && productResponse.getData() != null) {
                                ProductColorResponse productColor = productResponse.getData();
                                if (productColor.getProduct() != null) {
                                    productName = productColor.getProduct().getName();
                                }
                                if (productColor.getColor() != null) {
                                    colorName = productColor.getColor().getColorName();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error fetching product details for {}: {}", productColorId, e.getMessage());
                        }

                        return TopProductResponse.builder()
                                .productColorId(productColorId)
                                .productName(productName)
                                .colorName(colorName)
                                .totalQuantitySold(totalQuantity)
                                .totalRevenue(totalRevenue)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting top products: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<RevenueChartData> getRevenueChart(int days) {
        try {
            Calendar cal = Calendar.getInstance();
            Date endDate = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, -days);
            Date startDate = cal.getTime();

            List<Object[]> results = orderRepository.getRevenueChartData(COMPLETED_STATUSES, startDate, endDate);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            return results.stream()
                    .map(result -> {
                        Date date = (Date) result[0];
                        Double revenue = ((Number) result[1]).doubleValue();
                        Long orderCount = ((Number) result[2]).longValue();

                        return RevenueChartData.builder()
                                .date(dateFormat.format(date))
                                .revenue(revenue)
                                .orderCount(orderCount)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting revenue chart data: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<LowStockProductResponse> getLowStockProducts(String storeId, int threshold) {
        try {
            ApiResponse<List<LowStockProductResponse>> response = inventoryClient.getLowStockProducts(storeId, threshold);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching low stock products: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<OrderForShipperResponse> getOrdersForShipper(String storeId) {
        try {
            // Get orders that are ready for shipping (MANAGER_ACCEPT, READY_FOR_INVOICE, CONFIRMED, PACKAGED)
            List<EnumProcessOrder> readyForShippingStatuses = Arrays.asList(
                    EnumProcessOrder.MANAGER_ACCEPT,
                    EnumProcessOrder.READY_FOR_INVOICE,
                    EnumProcessOrder.CONFIRMED,
                    EnumProcessOrder.PACKAGED
            );

            // Get delivery assignments for this store
            ApiResponse<List<DeliveryAssignmentResponse>> deliveryResponse = deliveryClient.getDeliveryAssignmentsByStore(storeId);
            Map<Long, DeliveryAssignmentResponse> deliveryMap = new HashMap<>();
            if (deliveryResponse != null && deliveryResponse.getData() != null) {
                deliveryResponse.getData().forEach(da -> {
                    if (da.getOrderId() != null) {
                        deliveryMap.put(da.getOrderId(), da);
                    }
                });
            }

            // Get orders by store and status - fetch actual orders
            List<OrderForShipperResponse> ordersForShipper = new ArrayList<>();
            for (EnumProcessOrder status : readyForShippingStatuses) {
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 50);
                org.springframework.data.domain.Page<com.example.orderservice.entity.Order> ordersPage = 
                    orderRepository.findByStoreIdAndStatusAndIsDeletedFalse(storeId, status, pageable);
                
                for (com.example.orderservice.entity.Order order : ordersPage.getContent()) {
                    DeliveryAssignmentResponse deliveryAssignment = deliveryMap.get(order.getId());
                    
                    // Get customer info
                    String customerName = "N/A";
                    String customerPhone = "N/A";
                    AddressResponse deliveryAddress = null;
                    try {
                        ApiResponse<UserResponse> userResponse = userClient.getUserById(order.getUserId());
                        if (userResponse != null && userResponse.getData() != null) {
                            UserResponse user = userResponse.getData();
                            customerName = user.getFullName() != null ? user.getFullName() : "N/A";
                            customerPhone = user.getPhone() != null ? user.getPhone() : "N/A";
                        }
                        
                        ApiResponse<AddressResponse> addressResponse = userClient.getAddressById(order.getAddressId());
                        if (addressResponse != null && addressResponse.getData() != null) {
                            deliveryAddress = addressResponse.getData();
                        }
                    } catch (Exception e) {
                        log.warn("Error fetching user/address for order {}: {}", order.getId(), e.getMessage());
                    }
                    
                    // Get shipper info if assigned
                    String assignedShipperId = null;
                    String assignedShipperName = null;
                    if (deliveryAssignment != null) {
                        assignedShipperId = deliveryAssignment.getDeliveryStaffId();
                        if (assignedShipperId != null) {
                            try {
                                ApiResponse<UserResponse> shipperResponse = userClient.getUserById(assignedShipperId);
                                if (shipperResponse != null && shipperResponse.getData() != null) {
                                    assignedShipperName = shipperResponse.getData().getFullName();
                                }
                            } catch (Exception e) {
                                log.warn("Error fetching shipper info: {}", e.getMessage());
                            }
                        }
                    }
                    
                    ordersForShipper.add(OrderForShipperResponse.builder()
                            .orderId(order.getId())
                            .customerName(customerName)
                            .customerPhone(customerPhone)
                            .deliveryAddress(deliveryAddress)
                            .total(order.getTotal())
                            .orderDate(order.getOrderDate())
                            .status(order.getStatus())
                            .deliveryStatus(deliveryAssignment != null ? deliveryAssignment.getStatus() : null)
                            .assignedShipperId(assignedShipperId)
                            .assignedShipperName(assignedShipperName)
                            .estimatedDeliveryDate(deliveryAssignment != null && deliveryAssignment.getEstimatedDeliveryDate() != null 
                                ? java.sql.Timestamp.valueOf(deliveryAssignment.getEstimatedDeliveryDate()) : null)
                            .build());
                }
            }

            return ordersForShipper;
        } catch (Exception e) {
            log.error("Error getting orders for shipper: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    public StaffDashboardResponse getStaffDashboard(String staffId) {
        log.info("Getting staff dashboard data for staff: {}", staffId);

        // Get staff info to find storeId
        String storeId = null;
        try {
            // Assuming userClient has a way to get user info by ID or username (staffId is username/id)
            // If staffId is username, use getUserByUsername, if ID use getUserById
            // Here assuming staffId is ID as per other methods
            ApiResponse<UserResponse> userResponse = userClient.getUserById(staffId);
            if (userResponse != null && userResponse.getData() != null) {
                UserResponse user = userResponse.getData();
                if (user.getStoreIds() != null && !user.getStoreIds().isEmpty()) {
                    storeId = user.getStoreIds().get(0);
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching staff info: {}", e.getMessage());
        }

        // Date range: Today
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDate = cal.getTime();
        
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        Date endDate = cal.getTime();

        // 1. Personal Revenue (Today)
        Double personalRevenue = orderRepository.getTotalRevenueByCreatedByAndDateRange(staffId, COMPLETED_STATUSES, startDate, endDate);
        if (personalRevenue == null) personalRevenue = 0.0;

        // 2. Created Orders Count (Today)
        Long createdOrdersCount = orderRepository.countByCreatedByAndDateRange(staffId, startDate, endDate);

        // 3. Pending Orders at Store (Total backlog, not just today)
        Long pendingStoreOrdersCount = 0L;
        if (storeId != null) {
            pendingStoreOrdersCount = orderRepository.countOrdersByStoreAndStatus(storeId, EnumProcessOrder.PENDING); // Or PAYMENT/CONFIRMED depending on workflow
            // "Chờ xử lý" usually means PENDING or MANAGER_ACCEPT
            // Let's assume PENDING for now or MANAGER_ACCEPT
             pendingStoreOrdersCount = orderRepository.countOrdersByStoreAndStatus(storeId, EnumProcessOrder.MANAGER_ACCEPT);
        }

        return StaffDashboardResponse.builder()
                .personalRevenue(personalRevenue)
                .createdOrdersCount(createdOrdersCount)
                .pendingStoreOrdersCount(pendingStoreOrdersCount)
                .build();
    }
}

