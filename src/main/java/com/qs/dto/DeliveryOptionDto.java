package com.qs.dto;

import com.qs.enums.DeliveryStatus;

import java.time.LocalDate;

public class DeliveryOptionDto {

    private final String id;
    private final String displayName;
    private final String customerName;
    private final String productName;
    private final String deliveryName;
    private final LocalDate maintExpireDate;
    private final DeliveryStatus status;
    private final long daysToExpire;

    public DeliveryOptionDto(String id, String displayName, String customerName, String productName,
                             String deliveryName, LocalDate maintExpireDate,
                             DeliveryStatus status, long daysToExpire) {
        this.id = id;
        this.displayName = displayName;
        this.customerName = customerName;
        this.productName = productName;
        this.deliveryName = deliveryName;
        this.maintExpireDate = maintExpireDate;
        this.status = status;
        this.daysToExpire = daysToExpire;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public LocalDate getMaintExpireDate() {
        return maintExpireDate;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public long getDaysToExpire() {
        return daysToExpire;
    }
}
