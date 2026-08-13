package com.qs.service;

import com.qs.dto.CustomerDeliveryGroup;
import com.qs.dto.DeliveryBriefDto;
import com.qs.dto.DeliveryOptionDto;
import com.qs.dto.DeliveryView;
import com.qs.entity.Customer;
import com.qs.entity.Delivery;
import com.qs.entity.Product;
import com.qs.enums.DeliveryStatus;
import com.qs.repository.CustomerRepository;
import com.qs.repository.DeliveryNodeRepository;
import com.qs.repository.DeliveryRepository;
import com.qs.repository.ProductRepository;
import com.qs.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final TicketRepository ticketRepository;
    private final DeliveryAttachmentService attachmentService;
    private final DeliveryNodeRepository deliveryNodeRepository;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           CustomerRepository customerRepository,
                           ProductRepository productRepository,
                           TicketRepository ticketRepository,
                           DeliveryAttachmentService attachmentService,
                           DeliveryNodeRepository deliveryNodeRepository) {
        this.deliveryRepository = deliveryRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ticketRepository = ticketRepository;
        this.attachmentService = attachmentService;
        this.deliveryNodeRepository = deliveryNodeRepository;
    }

    public List<DeliveryView> listAll(String statusFilter, String keyword, Set<String> allowedDeliveryIds) {
        String kw = normalize(keyword);
        Map<String, String> customerNames = loadCustomerNames();
        Map<String, String> productNames = loadProductNames();
        return deliveryRepository.findAllByOrderByCreateTimeDesc().stream()
                .filter(d -> allowedDeliveryIds == null || allowedDeliveryIds.contains(d.getId()))
                .map(d -> toView(d, customerNames, productNames))
                .filter(view -> statusFilter == null || statusFilter.isBlank()
                        || view.getStatusLabel().equals(statusFilter))
                .filter(view -> kw == null || matchesKeyword(view, kw))
                .toList();
    }

    /** 按使用单位折叠分组（医院名升序） */
    public List<CustomerDeliveryGroup> listGroupedByCustomer(String statusFilter, String keyword,
                                                             Set<String> allowedDeliveryIds) {
        List<DeliveryView> all = listAll(statusFilter, keyword, allowedDeliveryIds);
        Map<String, List<DeliveryView>> grouped = new LinkedHashMap<>();
        Map<String, String> names = new HashMap<>();
        for (DeliveryView view : all) {
            String cid = view.getCustomerId() == null ? "" : view.getCustomerId();
            String cname = view.getCustomerName() == null || view.getCustomerName().isBlank()
                    ? "未指定使用单位" : view.getCustomerName();
            names.putIfAbsent(cid, cname);
            grouped.computeIfAbsent(cid, k -> new ArrayList<>()).add(view);
        }
        return grouped.entrySet().stream()
                .sorted(Comparator.comparing(e -> names.getOrDefault(e.getKey(), ""),
                        String.CASE_INSENSITIVE_ORDER))
                .map(e -> new CustomerDeliveryGroup(e.getKey(), names.get(e.getKey()), e.getValue()))
                .toList();
    }

    public List<DeliveryOptionDto> listOptions() {
        return listOptions(null);
    }

    public List<DeliveryOptionDto> listOptions(Set<String> allowedDeliveryIds) {
        Map<String, String> customerNames = loadCustomerNames();
        Map<String, String> productNames = loadProductNames();
        return deliveryRepository.findAllByOrderByCreateTimeDesc().stream()
                .filter(d -> allowedDeliveryIds == null || allowedDeliveryIds.contains(d.getId()))
                .map(d -> {
                    DeliveryStatus status = calculateStatus(d);
                    long days = calculateDaysToExpire(d.getMaintExpireDate());
                    String customerName = customerNames.getOrDefault(d.getCustomerId(), "—");
                    String productName = productNames.getOrDefault(d.getProductId(), "—");
                    String displayName = buildDisplayName(customerName, productName, d.getDeliveryName());
                    return new DeliveryOptionDto(
                            d.getId(), displayName, customerName, productName, d.getDeliveryName(),
                            d.getMaintExpireDate(), status, days);
                })
                .toList();
    }

    public DeliveryView getView(String id) {
        Delivery delivery = getById(id);
        return toView(delivery, loadCustomerNames(), loadProductNames());
    }

    public Delivery getById(String id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("产品交付不存在"));
    }

    @Transactional
    public Delivery save(Delivery delivery) {
        if (delivery.getDeliveryName() == null || delivery.getDeliveryName().isBlank()) {
            throw new IllegalArgumentException("请填写模块/批次名称");
        }
        if (delivery.getCustomerId() == null || delivery.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("请选择使用单位");
        }
        if (delivery.getProductId() == null || delivery.getProductId().isBlank()) {
            throw new IllegalArgumentException("请选择产品");
        }
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public void delete(String id) {
        try {
            attachmentService.deleteByDeliveryId(id);
        } catch (IOException ex) {
            throw new IllegalStateException("删除交付附件失败", ex);
        }
        deliveryNodeRepository.deleteByDeliveryId(id);
        ticketRepository.findAllWithDelivery().stream()
                .filter(t -> t.getDelivery().getId().equals(id))
                .forEach(t -> ticketRepository.deleteById(t.getId()));
        deliveryRepository.deleteById(id);
    }

    public DeliveryStatus calculateStatus(Delivery delivery) {
        LocalDate now = LocalDate.now();
        if (delivery.getLaunchDate() != null && now.isBefore(delivery.getLaunchDate())) {
            return DeliveryStatus.LAUNCHING;
        }
        if (delivery.getMaintExpireDate() == null) {
            return DeliveryStatus.MAINTAINING;
        }
        if (now.isAfter(delivery.getMaintExpireDate())) {
            return DeliveryStatus.EXPIRED;
        }
        long days = ChronoUnit.DAYS.between(now, delivery.getMaintExpireDate());
        if (days <= 90) {
            return DeliveryStatus.EXPIRING_SOON;
        }
        return DeliveryStatus.MAINTAINING;
    }

    public long calculateDaysToExpire(LocalDate maintExpireDate) {
        if (maintExpireDate == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), maintExpireDate);
    }

    public DeliveryBriefDto getBrief(String id) {
        DeliveryView view = getView(id);
        Delivery delivery = view.getDelivery();
        return new DeliveryBriefDto(
                delivery.getId(),
                view.getDisplayName(),
                delivery.getContactInfo(),
                delivery.getRemoteMethod(),
                delivery.getSpecialProcess(),
                delivery.getLaunchPlan(),
                delivery.getOnsiteManager(),
                delivery.getImplManager()
        );
    }

    public String buildDisplayName(Delivery delivery) {
        String customerName = customerRepository.findById(delivery.getCustomerId())
                .map(Customer::getName).orElse("—");
        String productName = productRepository.findById(delivery.getProductId())
                .map(Product::getName).orElse("—");
        return buildDisplayName(customerName, productName, delivery.getDeliveryName());
    }

    private DeliveryView toView(Delivery delivery, Map<String, String> customerNames, Map<String, String> productNames) {
        DeliveryStatus status = calculateStatus(delivery);
        long days = calculateDaysToExpire(delivery.getMaintExpireDate());
        String customerName = customerNames.getOrDefault(delivery.getCustomerId(), "—");
        String productName = productNames.getOrDefault(delivery.getProductId(), "—");
        return new DeliveryView(delivery, status, days, customerName, productName);
    }

    private Map<String, String> loadCustomerNames() {
        Map<String, String> map = new HashMap<>();
        customerRepository.findAll().forEach(c -> map.put(c.getId(), c.getName()));
        return map;
    }

    private Map<String, String> loadProductNames() {
        Map<String, String> map = new HashMap<>();
        productRepository.findAll().forEach(p -> map.put(p.getId(), p.getName()));
        return map;
    }

    private boolean matchesKeyword(DeliveryView view, String kw) {
        return contains(view.getDisplayName(), kw)
                || contains(view.getCustomerName(), kw)
                || contains(view.getProductName(), kw)
                || contains(view.getDeliveryName(), kw)
                || contains(view.getOnsiteManager(), kw)
                || contains(view.getImplManager(), kw);
    }

    private boolean contains(String text, String kw) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(kw);
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String buildDisplayName(String customerName, String productName, String deliveryName) {
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
}
