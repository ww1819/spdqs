package com.qs.controller;

import com.qs.dto.DeliveryBriefDto;
import com.qs.dto.DeliveryOptionDto;
import com.qs.service.DeliveryService;
import com.qs.service.PermissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/archives")
public class DeliveryApiController {

    private final DeliveryService deliveryService;
    private final PermissionService permissionService;

    public DeliveryApiController(DeliveryService deliveryService, PermissionService permissionService) {
        this.deliveryService = deliveryService;
        this.permissionService = permissionService;
    }

    @GetMapping("/options")
    public List<DeliveryOptionDto> options(@AuthenticationPrincipal UserDetails userDetails) {
        Set<String> allowed = userDetails == null
                ? Set.of()
                : permissionService.getAllowedDeliveryIds(userDetails.getUsername());
        return deliveryService.listOptions(allowed);
    }

    @GetMapping("/{id}/brief")
    public DeliveryBriefDto brief(@PathVariable String id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || !permissionService.canAccessDelivery(userDetails.getUsername(), id)) {
            throw new IllegalArgumentException("无权访问该医院/项目档案");
        }
        return deliveryService.getBrief(id);
    }
}
