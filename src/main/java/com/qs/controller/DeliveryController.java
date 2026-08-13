package com.qs.controller;

import com.qs.dto.DeliveryView;
import com.qs.entity.Customer;
import com.qs.entity.Delivery;
import com.qs.enums.DeliveryNodeType;
import com.qs.enums.DeliveryStatus;
import com.qs.service.CustomerService;
import com.qs.service.DeliveryAttachmentService;
import com.qs.service.DeliveryService;
import com.qs.service.PartnerService;
import com.qs.service.PermissionService;
import com.qs.service.ProductService;
import com.qs.service.TicketService;
import com.qs.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/archives")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final CustomerService customerService;
    private final ProductService productService;
    private final PartnerService partnerService;
    private final TicketService ticketService;
    private final UserService userService;
    private final DeliveryAttachmentService attachmentService;
    private final PermissionService permissionService;

    public DeliveryController(DeliveryService deliveryService,
                              CustomerService customerService,
                              ProductService productService,
                              PartnerService partnerService,
                              TicketService ticketService,
                              UserService userService,
                              DeliveryAttachmentService attachmentService,
                              PermissionService permissionService) {
        this.deliveryService = deliveryService;
        this.customerService = customerService;
        this.productService = productService;
        this.partnerService = partnerService;
        this.ticketService = ticketService;
        this.userService = userService;
        this.attachmentService = attachmentService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        Set<String> allowed = permissionService.getAllowedDeliveryIds(userDetails.getUsername());
        model.addAttribute("returnUrl", "/archives");
        model.addAttribute("deliveryGroups", deliveryService.listGroupedByCustomer(status, keyword, allowed));
        model.addAttribute("statusFilter", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("statuses", Arrays.asList(DeliveryStatus.values()));
        model.addAttribute("activeTab", "archives");
        return "archive/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        addFormOptions(model);
        model.addAttribute("delivery", new Delivery());
        model.addAttribute("activeTab", "archives");
        return "archive/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal UserDetails userDetails) {
        ensureDeliveryAccess(userDetails, id);
        addUserToModel(model, userDetails);
        model.addAttribute("view", deliveryService.getView(id));
        model.addAttribute("delivery", deliveryService.getById(id));
        addAttachmentModel(model, id);
        model.addAttribute("tickets", ticketService.listByDeliveryId(id));
        model.addAttribute("activeTab", "archives");
        return "archive/detail";
    }

    @GetMapping("/{id}/nodes")
    public String nodes(@PathVariable String id, Model model,
                        @AuthenticationPrincipal UserDetails userDetails) {
        ensureDeliveryAccess(userDetails, id);
        addUserToModel(model, userDetails);
        model.addAttribute("view", deliveryService.getView(id));
        model.addAttribute("delivery", deliveryService.getById(id));
        model.addAttribute("nodeTypes", Arrays.asList(DeliveryNodeType.values()));
        model.addAttribute("activeTab", "archives");
        return "archive/nodes";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        ensureDeliveryAccess(userDetails, id);
        addUserToModel(model, userDetails);
        addFormOptions(model);
        model.addAttribute("delivery", deliveryService.getById(id));
        addAttachmentModel(model, id);
        model.addAttribute("activeTab", "archives");
        return "archive/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Delivery delivery,
                       @RequestParam(required = false) String newCustomerName,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        if ((delivery.getCustomerId() == null || delivery.getCustomerId().isBlank())
                && newCustomerName != null && !newCustomerName.isBlank()) {
            var user = userService.findByUsername(userDetails.getUsername());
            Customer customer = new Customer();
            customer.setName(newCustomerName.trim());
            customer.setCreateBy(user != null ? user.getDisplayName() : userDetails.getUsername());
            delivery.setCustomerId(customerService.save(customer).getId());
        }
        boolean isNew = delivery.getId() == null || delivery.getId().isBlank();
        if (!isNew) {
            ensureDeliveryAccess(userDetails, delivery.getId());
            Delivery existing = deliveryService.getById(delivery.getId());
            delivery.setCreateBy(existing.getCreateBy());
            delivery.setCreateTime(existing.getCreateTime());
        } else {
            delivery.setId(null);
            var user = userService.findByUsername(userDetails.getUsername());
            delivery.setCreateBy(user != null ? user.getDisplayName() : userDetails.getUsername());
        }
        Delivery saved = deliveryService.save(delivery);
        if (isNew) {
            var user = userService.findByUsername(userDetails.getUsername());
            if (user != null) {
                permissionService.grantDeliveryToUser(user.getId(), saved.getId());
            }
        }
        redirectAttributes.addFlashAttribute("success", "产品交付保存成功，可继续上传附件");
        return "redirect:/archives/" + saved.getId() + "/edit";
    }

    @PostMapping("/{id}/attachments")
    public String uploadAttachments(@PathVariable String id,
                                    @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                    @RequestParam(defaultValue = "edit") String returnTo,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        Delivery delivery = deliveryService.getById(id);
        String createBy = resolveDisplayName(userDetails);
        try {
            attachmentService.uploadBatch(delivery, files, createBy);
            redirectAttributes.addFlashAttribute("success", "上传成功");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        if ("detail".equals(returnTo)) {
            return "redirect:/archives/" + id;
        }
        return "redirect:/archives/" + id + "/edit";
    }

    @PostMapping("/attachments/{attachmentId}/delete")
    public String deleteAttachment(@PathVariable String attachmentId,
                                   @RequestParam String deliveryId,
                                   @RequestParam(defaultValue = "edit") String returnTo,
                                   RedirectAttributes redirectAttributes) {
        try {
            attachmentService.delete(attachmentId);
            redirectAttributes.addFlashAttribute("success", "已删除");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        if ("detail".equals(returnTo)) {
            return "redirect:/archives/" + deliveryId;
        }
        return "redirect:/archives/" + deliveryId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        ensureDeliveryAccess(userDetails, id);
        deliveryService.delete(id);
        redirectAttributes.addFlashAttribute("success", "产品交付已删除");
        return "redirect:/archives";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("customers", customerService.listAll());
        model.addAttribute("products", productService.listEnabled());
        model.addAttribute("partners", partnerService.listAll());
    }

    private void addAttachmentModel(Model model, String deliveryId) {
        model.addAttribute("deliveryAttachments", attachmentService.listByDeliveryId(deliveryId));
    }

    private void addUserToModel(Model model, UserDetails userDetails) {
        if (userDetails != null) {
            var user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", user != null ? user.getDisplayName() : userDetails.getUsername());
        }
    }

    private void ensureDeliveryAccess(UserDetails userDetails, String deliveryId) {
        if (userDetails == null || !permissionService.canAccessDelivery(userDetails.getUsername(), deliveryId)) {
            throw new IllegalArgumentException("无权访问该产品交付");
        }
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
