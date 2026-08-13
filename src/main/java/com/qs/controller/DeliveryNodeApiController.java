package com.qs.controller;

import com.qs.dto.DeliveryNodeDto;
import com.qs.dto.DeliveryNodeRequest;
import com.qs.service.DeliveryNodeService;
import com.qs.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/archives")
public class DeliveryNodeApiController {

    private final DeliveryNodeService deliveryNodeService;
    private final UserService userService;

    public DeliveryNodeApiController(DeliveryNodeService deliveryNodeService, UserService userService) {
        this.deliveryNodeService = deliveryNodeService;
        this.userService = userService;
    }

    @GetMapping("/{deliveryId}/nodes")
    public List<DeliveryNodeDto> list(@PathVariable String deliveryId) {
        return deliveryNodeService.listByDeliveryId(deliveryId);
    }

    @PostMapping("/{deliveryId}/nodes")
    public List<DeliveryNodeDto> create(@PathVariable String deliveryId,
                                       @RequestBody DeliveryNodeRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        return deliveryNodeService.create(deliveryId, request, resolveDisplayName(userDetails));
    }

    @PutMapping("/{deliveryId}/nodes/{nodeId}")
    public List<DeliveryNodeDto> update(@PathVariable String deliveryId,
                                       @PathVariable String nodeId,
                                       @RequestBody DeliveryNodeRequest request) {
        return deliveryNodeService.update(deliveryId, nodeId, request);
    }

    @DeleteMapping("/{deliveryId}/nodes/{nodeId}")
    public List<DeliveryNodeDto> delete(@PathVariable String deliveryId,
                                       @PathVariable String nodeId) {
        return deliveryNodeService.delete(deliveryId, nodeId);
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return "";
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
