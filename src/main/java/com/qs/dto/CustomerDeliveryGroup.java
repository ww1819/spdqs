package com.qs.dto;

import java.util.List;

/** 产品交付列表按使用单位折叠分组 */
public class CustomerDeliveryGroup {

    private final String customerId;
    private final String customerName;
    private final List<DeliveryView> deliveries;

    public CustomerDeliveryGroup(String customerId, String customerName, List<DeliveryView> deliveries) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.deliveries = deliveries;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public List<DeliveryView> getDeliveries() {
        return deliveries;
    }

    public int getCount() {
        return deliveries == null ? 0 : deliveries.size();
    }
}
