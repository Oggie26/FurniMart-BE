package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Voucher;
import com.example.orderservice.enums.VoucherType;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.VoucherRepository;
import com.example.orderservice.request.VoucherRequest;
import com.example.orderservice.response.VoucherResponse;
import com.example.orderservice.service.inteface.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;

    @Override
    public VoucherResponse createVoucher(VoucherRequest request) {
        log.info("Creating voucher with code: {}", request.getCode());

        // Check if voucher code already exists
        if (voucherRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.VOUCHER_CODE_EXISTS);
        }

        Voucher voucher = Voucher.builder()
                .name(request.getName())
                .code(request.getCode())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .amount(request.getAmount())
                .description(request.getDescription())
                .point(request.getPoint())
                .type(request.getType())
                .status(request.getStatus())
                .usageLimit(request.getUsageLimit())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .build();

        voucher = voucherRepository.save(voucher);
        log.info("Voucher created successfully with ID: {}", voucher.getId());

        return mapToVoucherResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Integer id) {
        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        
        return mapToVoucherResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherByCode(String code) {
        Voucher voucher = voucherRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        
        return mapToVoucherResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAllVouchers() {
        List<Voucher> vouchers = voucherRepository.findAll()
                .stream()
                .filter(voucher -> !voucher.getIsDeleted())
                .toList();

        return vouchers.stream()
                .map(this::mapToVoucherResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getActiveVouchers() {
        List<Voucher> vouchers = voucherRepository.findActiveVouchers(LocalDateTime.now());
        
        return vouchers.stream()
                .map(this::mapToVoucherResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getVouchersByType(VoucherType type) {
        List<Voucher> vouchers = voucherRepository.findByTypeAndStatusTrueAndIsDeletedFalse(type);
        
        return vouchers.stream()
                .map(this::mapToVoucherResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getVouchersByOrderId(Long orderId) {
        List<Voucher> vouchers = voucherRepository.findByOrderIdAndIsDeletedFalse(orderId);
        
        return vouchers.stream()
                .map(this::mapToVoucherResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getApplicableVouchers(Double orderAmount) {
        List<Voucher> vouchers = voucherRepository.findApplicableVouchers(
                LocalDateTime.now(), orderAmount.floatValue());
        
        return vouchers.stream()
                .map(this::mapToVoucherResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VoucherResponse updateVoucher(Integer id, VoucherRequest request) {
        log.info("Updating voucher with ID: {}", id);

        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        if (!voucher.getCode().equals(request.getCode()) &&
            voucherRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new AppException(ErrorCode.VOUCHER_CODE_EXISTS);
        }


        voucher.setName(request.getName());
        voucher.setCode(request.getCode());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setAmount(request.getAmount());
        voucher.setDescription(request.getDescription());
        voucher.setPoint(request.getPoint());
        voucher.setType(request.getType());
        voucher.setStatus(request.getStatus());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setMinimumOrderAmount(request.getMinimumOrderAmount());

        voucher = voucherRepository.save(voucher);
        log.info("Voucher updated successfully with ID: {}", voucher.getId());

        return mapToVoucherResponse(voucher);
    }

    @Override
    public void deleteVoucher(Integer id) {
        log.info("Deleting voucher with ID: {}", id);

        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        voucher.setIsDeleted(true);
        voucherRepository.save(voucher);

        log.info("Voucher deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse validateVoucher(String code, Double orderAmount) {
        log.info("Validating voucher with code: {} for order amount: {}", code, orderAmount);

        Voucher voucher = voucherRepository.findActiveVoucherByCode(code, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_INVALID));

        if (!voucher.canBeUsedForOrder(orderAmount)) {
            throw new AppException(ErrorCode.VOUCHER_NOT_APPLICABLE);
        }

        return mapToVoucherResponse(voucher);
    }

    @Override
    public VoucherResponse applyVoucher(String code, Long orderId) {
        log.info("Applying voucher with code: {} to order: {}", code, orderId);

        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        VoucherResponse voucherResponse = validateVoucher(code, order.getTotal());
        
        incrementUsageCount(voucherResponse.getId());

        log.info("Voucher applied successfully to order: {}", orderId);

        return voucherResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateDiscount(String voucherCode, Double orderAmount) {
        Voucher voucher = voucherRepository.findActiveVoucherByCode(voucherCode, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_INVALID));

        if (!voucher.canBeUsedForOrder(orderAmount)) {
            throw new AppException(ErrorCode.VOUCHER_NOT_APPLICABLE);
        }

        double discount = 0.0;

        switch (voucher.getType()) {
            case PERCENTAGE:
                discount = orderAmount * (voucher.getAmount() / 100.0);
                break;
            case FIXED_AMOUNT:
                discount = Math.min(voucher.getAmount(), orderAmount);
                break;
            case FREE_SHIPPING:
                // This would typically be a fixed shipping cost
                discount = 0.0; // Implement based on your shipping logic
                break;
            case CASHBACK:
                discount = Math.min(voucher.getAmount(), orderAmount * 0.1); // 10% max cashback
                break;
            default:
                discount = 0.0;
                break;
        }

        return discount;
    }

    @Override
    public void incrementUsageCount(Integer voucherId) {
        Voucher voucher = voucherRepository.findByIdAndIsDeletedFalse(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);

        log.info("Incremented usage count for voucher ID: {} to {}", voucherId, voucher.getUsedCount());
    }

    @Override
    public void expireVouchers() {
        log.info("Expiring vouchers that have passed their end date");

        List<Voucher> expiredVouchers = voucherRepository.findExpiredVouchers(LocalDateTime.now());
        
        for (Voucher voucher : expiredVouchers) {
            voucher.setStatus(false);
        }
        
        if (!expiredVouchers.isEmpty()) {
            voucherRepository.saveAll(expiredVouchers);
            log.info("Expired {} vouchers", expiredVouchers.size());
        }
    }

    private VoucherResponse mapToVoucherResponse(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        
        return VoucherResponse.builder()
                .id(voucher.getId())
                .name(voucher.getName())
                .code(voucher.getCode())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .amount(voucher.getAmount())
                .description(voucher.getDescription())
                .point(voucher.getPoint())
                .type(voucher.getType())
                .status(voucher.getStatus())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .minimumOrderAmount(voucher.getMinimumOrderAmount())
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .isActive(voucher.isActive())
                .isExpired(voucher.getEndDate().isBefore(now))
                .build();
    }
}
