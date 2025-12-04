package com.example.orderservice.service;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.feign.InventoryClient;
import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.repository.OrderDetailRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.response.*;
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
public class BranchAnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final InventoryClient inventoryClient;
    private final ProductClient productClient;

    private static final List<EnumProcessOrder> COMPLETED_STATUSES = Arrays.asList(
            EnumProcessOrder.DELIVERED,
            EnumProcessOrder.FINISHED
    );

    @Transactional(readOnly = true)
    public BranchDailyStatsResponse getDailyStats(String storeId) {
        log.info("Getting daily stats for store: {}", storeId);

        // Get today's date range
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date endOfDay = cal.getTime();

        // Total orders today
        Long totalOrdersToday = orderRepository.countByStoreIdAndDateRange(storeId, startOfDay, endOfDay);
        if (totalOrdersToday == null) totalOrdersToday = 0L;

        // Pending orders
        Long pendingOrders = orderRepository.countOrdersByStoreAndStatus(storeId, EnumProcessOrder.PENDING);
        if (pendingOrders == null) pendingOrders = 0L;

        // Processing orders (SHIPPING, PACKAGED, CONFIRMED)
        Long processingOrders = 0L;
        for (EnumProcessOrder status : Arrays.asList(
                EnumProcessOrder.SHIPPING,
                EnumProcessOrder.PACKAGED,
                EnumProcessOrder.CONFIRMED,
                EnumProcessOrder.MANAGER_ACCEPT,
                EnumProcessOrder.READY_FOR_INVOICE
        )) {
            Long count = orderRepository.countOrdersByStoreAndStatus(storeId, status);
            if (count != null) {
                processingOrders += count;
            }
        }

        // Completed orders today
        Long completedOrders = 0L;
        for (EnumProcessOrder status : COMPLETED_STATUSES) {
            Long count = orderRepository.countOrdersByStoreAndStatusAndDateRange(
                    storeId, status, startOfDay, endOfDay);
            if (count != null) {
                completedOrders += count;
            }
        }

        // Revenue today
        Double revenueToday = orderRepository.getTotalRevenueByStoreAndStatusesAndDateRange(
                storeId, COMPLETED_STATUSES, startOfDay, endOfDay);
        if (revenueToday == null) revenueToday = 0.0;

        // New customers today (users who placed their first order today)
        Long newCustomersToday = orderRepository.countNewCustomersByStoreAndDateRange(storeId, startOfDay, endOfDay);
        if (newCustomersToday == null) newCustomersToday = 0L;

        return BranchDailyStatsResponse.builder()
                .totalOrdersToday(totalOrdersToday)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .completedOrders(completedOrders)
                .revenueToday(revenueToday)
                .newCustomersToday(newCustomersToday)
                .build();
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary(String storeId) {
        log.info("Getting inventory summary for store: {}", storeId);

        try {
            // Get low stock products
            ApiResponse<List<LowStockProductResponse>> lowStockResponse = 
                    inventoryClient.getLowStockProducts(storeId, 10);
            
            List<LowStockProductResponse> lowStockProducts = 
                    (lowStockResponse != null && lowStockResponse.getData() != null) 
                            ? lowStockResponse.getData() 
                            : Collections.emptyList();

            // Calculate summary statistics
            long totalProducts = lowStockProducts.size();
            long lowStockCount = lowStockProducts.stream()
                    .filter(p -> p.getCurrentStock() != null && p.getCurrentStock() > 0)
                    .count();
            long outOfStockCount = lowStockProducts.stream()
                    .filter(p -> p.getCurrentStock() != null && p.getCurrentStock() == 0)
                    .count();
            long inStockCount = totalProducts - lowStockCount - outOfStockCount;

            // Convert to InventoryItemSummary
            List<InventorySummaryResponse.InventoryItemSummary> items = lowStockProducts.stream()
                    .map(p -> {
                        String status = "IN_STOCK";
                        if (p.getCurrentStock() == null || p.getCurrentStock() == 0) {
                            status = "OUT_OF_STOCK";
                        } else if (p.getCurrentStock() <= (p.getThreshold() != null ? p.getThreshold() : 10)) {
                            status = "LOW_STOCK";
                        }

                        return InventorySummaryResponse.InventoryItemSummary.builder()
                                .productId(p.getProductColorId())
                                .productName(p.getProductName())
                                .colorName(p.getColorName())
                                .currentStock(p.getCurrentStock())
                                .minStock(p.getThreshold())
                                .status(status)
                                .build();
                    })
                    .collect(Collectors.toList());

            return InventorySummaryResponse.builder()
                    .totalProducts(totalProducts)
                    .lowStockProducts(lowStockCount)
                    .outOfStockProducts(outOfStockCount)
                    .inStockProducts(inStockCount)
                    .items(items)
                    .build();
        } catch (Exception e) {
            log.error("Error getting inventory summary: {}", e.getMessage(), e);
            return InventorySummaryResponse.builder()
                    .totalProducts(0L)
                    .lowStockProducts(0L)
                    .outOfStockProducts(0L)
                    .inStockProducts(0L)
                    .items(Collections.emptyList())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public ActivityTrendResponse getActivityTrend(String storeId, int days) {
        log.info("Getting activity trend for store: {} for {} days", storeId, days);

        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        Date startDate = cal.getTime();

        try {
            List<Object[]> results = orderRepository.getRevenueChartDataByStore(
                    storeId, COMPLETED_STATUSES, startDate, endDate);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            List<ActivityTrendResponse.TrendDataPoint> dataPoints = results.stream()
                    .map(result -> {
                        Date date = (Date) result[0];
                        Double revenue = ((Number) result[1]).doubleValue();
                        Long orderCount = ((Number) result[2]).longValue();

                        return ActivityTrendResponse.TrendDataPoint.builder()
                                .date(dateFormat.format(date))
                                .revenue(revenue)
                                .orderCount(orderCount)
                                .build();
                    })
                    .collect(Collectors.toList());

            return ActivityTrendResponse.builder()
                    .dataPoints(dataPoints)
                    .build();
        } catch (Exception e) {
            log.error("Error getting activity trend: {}", e.getMessage(), e);
            return ActivityTrendResponse.builder()
                    .dataPoints(Collections.emptyList())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public OrderStatusBreakdownResponse getOrderStatusBreakdown(String storeId) {
        log.info("Getting order status breakdown for store: {}", storeId);

        List<OrderStatusBreakdownResponse.StatusCount> statusCounts = new ArrayList<>();
        long totalOrders = 0;

        // Get counts for each status
        for (EnumProcessOrder status : EnumProcessOrder.values()) {
            Long count = orderRepository.countOrdersByStoreAndStatus(storeId, status);
            if (count == null) count = 0L;
            totalOrders += count;

            statusCounts.add(OrderStatusBreakdownResponse.StatusCount.builder()
                    .status(status.name())
                    .count(count)
                    .percentage(0.0) // Will calculate after we have total
                    .build());
        }

        // Calculate percentages
        for (OrderStatusBreakdownResponse.StatusCount statusCount : statusCounts) {
            double percentage = totalOrders > 0 
                    ? (statusCount.getCount().doubleValue() / totalOrders) * 100.0 
                    : 0.0;
            statusCount.setPercentage(percentage);
        }

        return OrderStatusBreakdownResponse.builder()
                .statusCounts(statusCounts)
                .totalOrders(totalOrders)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(String storeId, int limit) {
        log.info("Getting top {} products for store: {}", limit, storeId);

        try {
            List<Object[]> results = orderDetailRepository.getTopProductsBySalesAndStore(
                    storeId, COMPLETED_STATUSES);

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
}

