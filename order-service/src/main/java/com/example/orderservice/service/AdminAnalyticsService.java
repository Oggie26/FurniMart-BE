package com.example.orderservice.service;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.feign.StoreClient;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderDetailRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StoreClient storeClient;
    private final UserClient userClient;
    private final ProductClient productClient;

    private static final List<EnumProcessOrder> COMPLETED_STATUSES = Arrays.asList(
            EnumProcessOrder.DELIVERED,
            EnumProcessOrder.FINISHED
    );

    @Transactional(readOnly = true)
    public OverviewStatsResponse getOverviewStats() {
        log.info("Getting overview statistics");

        // 1. Total Revenue (Net Revenue = Gross Revenue - Refunded Amount)
        Double grossRevenue = orderRepository.getTotalRevenueByStatuses(COMPLETED_STATUSES);
        if (grossRevenue == null) grossRevenue = 0.0;
        
        Double totalRefunded = orderRepository.getTotalRefundedAmountByStatuses(COMPLETED_STATUSES);
        if (totalRefunded == null) totalRefunded = 0.0;
        
        Double totalRevenue = grossRevenue - totalRefunded;
        if (totalRevenue < 0) totalRevenue = 0.0; // Ensure non-negative

        // 2. Total Orders
        long totalOrders = orderRepository.count();

        // 3. Total Active Stores
        Long totalActiveStores = 0L;
        try {
            ApiResponse<Long> storesResponse = storeClient.getActiveStoresCount();
            if (storesResponse != null && storesResponse.getData() != null) {
                totalActiveStores = storesResponse.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching active stores count: {}", e.getMessage());
        }

        // 4. Total Users
        Long totalUsers = 0L;
        try {
            ApiResponse<Long> usersResponse = userClient.getTotalUsersCount();
            if (usersResponse != null && usersResponse.getData() != null) {
                totalUsers = usersResponse.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching users count: {}", e.getMessage());
        }

        return OverviewStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalActiveStores(totalActiveStores)
                .totalUsers(totalUsers)
                .build();
    }

    @Transactional(readOnly = true)
    public RevenueByBranchResponse getRevenueByBranch() {
        log.info("Getting revenue by branch");

        List<RevenueByBranchResponse.BranchRevenueData> branchRevenues = new ArrayList<>();

        try {
            // Get all stores
            ApiResponse<List<StoreResponse>> storesResponse = storeClient.getAllStores();
            if (storesResponse != null && storesResponse.getData() != null) {
                for (StoreResponse store : storesResponse.getData()) {
                    // Get revenue for this store (Net Revenue = Gross Revenue - Refunded Amount)
                    Double grossRevenue = orderRepository.getTotalRevenueByStoreAndStatuses(
                            store.getId(), COMPLETED_STATUSES);
                    if (grossRevenue == null) grossRevenue = 0.0;
                    
                    Double totalRefunded = orderRepository.getTotalRefundedAmountByStoreAndStatuses(
                            store.getId(), COMPLETED_STATUSES);
                    if (totalRefunded == null) totalRefunded = 0.0;
                    
                    Double revenue = grossRevenue - totalRefunded;
                    if (revenue < 0) revenue = 0.0; // Ensure non-negative

                    // Get order count for this store
                    Long orderCount = orderRepository.countByStoreId(store.getId());
                    if (orderCount == null) orderCount = 0L;

                    branchRevenues.add(RevenueByBranchResponse.BranchRevenueData.builder()
                            .branchId(store.getId())
                            .branchName(store.getName())
                            .revenue(revenue)
                            .orderCount(orderCount)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Error getting revenue by branch: {}", e.getMessage(), e);
        }

        // Sort by revenue descending
        branchRevenues.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));

        return RevenueByBranchResponse.builder()
                .branches(branchRevenues)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(int limit) {
        log.info("Getting top {} products", limit);

        try {
            List<Object[]> results = orderDetailRepository.getTopProductsBySales(COMPLETED_STATUSES);

            return results.stream()
                    .limit(limit)
                    .map(result -> {
                        String productColorId = (String) result[0];
                        Long totalQuantity = ((Number) result[1]).longValue();
                        Double totalRevenue = ((Number) result[2]).doubleValue();

                        // Fetch product details from ProductClient
                        String productName = "N/A";
                        String colorName = "N/A";
                        try {
                            ProductColorResponse productColor = getProductColor(productColorId);
                            if (productColor != null) {
                                if (productColor.getProduct() != null) {
                                    productName = productColor.getProduct().getName();
                                }
                                if (productColor.getColor() != null) {
                                    colorName = productColor.getColor().getColorName();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error fetching product details for productColorId {}: {}", productColorId, e.getMessage());
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

    private ProductColorResponse getProductColor(String productColorId) {
        try {
            ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching product color for id {}: {}", productColorId, e.getMessage());
        }
        return null;
    }

    @Transactional(readOnly = true)
    public DeliveryPerformanceResponse getDeliveryPerformance() {
        log.info("Getting delivery performance statistics");

        List<DeliveryPerformanceResponse.DeliveryStatusData> statusDataList = new ArrayList<>();
        long totalDeliveries = 0;

        // Get counts for each delivery status
        Map<EnumProcessOrder, Long> statusCounts = new HashMap<>();
        for (EnumProcessOrder status : EnumProcessOrder.values()) {
            Long count = orderRepository.countByStatus(status);
            if (count == null) count = 0L;
            statusCounts.put(status, count);
            totalDeliveries += count;
        }

        // Calculate percentages
        for (Map.Entry<EnumProcessOrder, Long> entry : statusCounts.entrySet()) {
            double percentage = totalDeliveries > 0 
                    ? (entry.getValue().doubleValue() / totalDeliveries) * 100.0 
                    : 0.0;

            statusDataList.add(DeliveryPerformanceResponse.DeliveryStatusData.builder()
                    .status(entry.getKey().name())
                    .count(entry.getValue())
                    .percentage(percentage)
                    .build());
        }

        return DeliveryPerformanceResponse.builder()
                .statuses(statusDataList)
                .build();
    }
}

