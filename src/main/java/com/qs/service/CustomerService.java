package com.qs.service;

import com.qs.entity.Customer;
import com.qs.repository.CustomerRepository;
import com.qs.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final DeliveryRepository deliveryRepository;

    public CustomerService(CustomerRepository customerRepository, DeliveryRepository deliveryRepository) {
        this.customerRepository = customerRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public List<Customer> listAll() {
        return customerRepository.findAllByOrderByNameAsc();
    }

    public List<Customer> search(String keyword) {
        String kw = normalize(keyword);
        if (kw == null) {
            return listAll();
        }
        return listAll().stream()
                .filter(c -> contains(c.getName(), kw)
                        || contains(c.getNamePy(), kw)
                        || contains(c.getCode(), kw)
                        || contains(c.getContact(), kw))
                .toList();
    }

    public Customer getById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("使用单位不存在"));
    }

    public long countDeliveries(String customerId) {
        return deliveryRepository.findByCustomerIdOrderByCreateTimeDesc(customerId).size();
    }

    @Transactional
    public Customer save(Customer customer) {
        if (customer.getName() == null || customer.getName().isBlank()) {
            throw new IllegalArgumentException("使用单位名称不能为空");
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(String id) {
        if (countDeliveries(id) > 0) {
            throw new IllegalArgumentException("该使用单位下仍有产品交付，请先删除或转移交付后再删");
        }
        customerRepository.deleteById(id);
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String text, String kw) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(kw);
    }
}
