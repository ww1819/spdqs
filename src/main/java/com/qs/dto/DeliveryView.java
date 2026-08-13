package com.qs.dto;

import com.qs.entity.Delivery;
import com.qs.enums.DeliveryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeliveryView {

    private final Delivery delivery;
    private final DeliveryStatus status;
    private final long daysToExpire;
    private final String customerName;
    private final String productName;
    private final String displayName;

    public DeliveryView(Delivery delivery, DeliveryStatus status, long daysToExpire,
                        String customerName, String productName) {
        this.delivery = delivery;
        this.status = status;
        this.daysToExpire = daysToExpire;
        this.customerName = customerName;
        this.productName = productName;
        this.displayName = buildDisplayName(customerName, productName, delivery.getDeliveryName());
    }

    private static String buildDisplayName(String customerName, String productName, String deliveryName) {
        StringBuilder sb = new StringBuilder();
        if (customerName != null && !customerName.isBlank()) {
            sb.append(customerName);
        }
        if (productName != null && !productName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(productName);
        }
        if (deliveryName != null && !deliveryName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(deliveryName);
        }
        return sb.isEmpty() ? "—" : sb.toString();
    }

    public String getId() {
        return delivery.getId();
    }

    public String getCustomerId() {
        return delivery.getCustomerId();
    }

    public String getProductId() {
        return delivery.getProductId();
    }

    public String getDeliveryName() {
        return delivery.getDeliveryName();
    }

    public String getDeliveryCode() {
        return delivery.getDeliveryCode();
    }

    public String getPartnerId() {
        return delivery.getPartnerId();
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDate getLaunchDate() {
        return delivery.getLaunchDate();
    }

    public LocalDate getMaintExpireDate() {
        return delivery.getMaintExpireDate();
    }

    public String getLaunchPlan() {
        return delivery.getLaunchPlan();
    }

    public String getSpecialProcess() {
        return delivery.getSpecialProcess();
    }

    public String getContactInfo() {
        return delivery.getContactInfo();
    }

    public String getRemoteMethod() {
        return delivery.getRemoteMethod();
    }

    public String getOnsiteManager() {
        return delivery.getOnsiteManager();
    }

    public String getImplManager() {
        return delivery.getImplManager();
    }

    public String getCreateBy() {
        return delivery.getCreateBy();
    }

    public LocalDateTime getCreateTime() {
        return delivery.getCreateTime();
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return status.getLabel();
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case LAUNCHING -> "bg-primary";
            case MAINTAINING -> "bg-success";
            case EXPIRING_SOON -> "bg-warning text-dark";
            case EXPIRED -> "bg-danger";
        };
    }

    public long getDaysToExpire() {
        return daysToExpire;
    }
}
