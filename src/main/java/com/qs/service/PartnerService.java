package com.qs.service;

import com.qs.entity.Partner;
import com.qs.entity.PartnerDeliveryPerm;
import com.qs.repository.DeliveryRepository;
import com.qs.repository.PartnerDeliveryPermRepository;
import com.qs.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PartnerDeliveryPermRepository partnerDeliveryPermRepository;
    private final DeliveryRepository deliveryRepository;

    public PartnerService(PartnerRepository partnerRepository,
                          PartnerDeliveryPermRepository partnerDeliveryPermRepository,
                          DeliveryRepository deliveryRepository) {
        this.partnerRepository = partnerRepository;
        this.partnerDeliveryPermRepository = partnerDeliveryPermRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public List<Partner> listAll() {
        return partnerRepository.findAllByOrderByNameAsc();
    }

    public List<Partner> search(String keyword) {
        String kw = normalize(keyword);
        if (kw == null) {
            return listAll();
        }
        return listAll().stream()
                .filter(p -> contains(p.getName(), kw)
                        || contains(p.getNamePy(), kw)
                        || contains(p.getCode(), kw)
                        || contains(p.getContact(), kw))
                .toList();
    }

    public Partner getById(String id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("服务商不存在"));
    }

    @Transactional
    public Partner save(Partner partner) {
        if (partner.getName() == null || partner.getName().isBlank()) {
            throw new IllegalArgumentException("服务商名称不能为空");
        }
        return partnerRepository.save(partner);
    }

    @Transactional
    public void delete(String id) {
        partnerDeliveryPermRepository.deleteByPartnerId(id);
        partnerRepository.deleteById(id);
    }

    public Set<String> getAssignedDeliveryIds(String partnerId) {
        return partnerDeliveryPermRepository.findByPartnerId(partnerId).stream()
                .map(PartnerDeliveryPerm::getDeliveryId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Transactional
    public void saveDeliveryPermissions(String partnerId, List<String> deliveryIds) {
        getById(partnerId);
        partnerDeliveryPermRepository.deleteByPartnerId(partnerId);
        Set<String> ids = deliveryIds == null ? Set.of() : new HashSet<>(deliveryIds);
        for (String deliveryId : ids) {
            if (deliveryId != null && !deliveryId.isBlank() && deliveryRepository.existsById(deliveryId)) {
                PartnerDeliveryPerm perm = new PartnerDeliveryPerm();
                perm.setPartnerId(partnerId);
                perm.setDeliveryId(deliveryId);
                partnerDeliveryPermRepository.save(perm);
            }
        }
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
