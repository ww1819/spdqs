package com.qs.controller;

import com.qs.entity.Partner;
import com.qs.service.DeliveryService;
import com.qs.service.PartnerService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/partners")
public class PartnerController {

    private final PartnerService partnerService;
    private final DeliveryService deliveryService;
    private final UserService userService;

    public PartnerController(PartnerService partnerService, DeliveryService deliveryService,
                             UserService userService) {
        this.partnerService = partnerService;
        this.deliveryService = deliveryService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("partners", partnerService.search(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeTab", "partners");
        return "partner/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("partner", new Partner());
        model.addAttribute("activeTab", "partners");
        return "partner/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("partner", partnerService.getById(id));
        model.addAttribute("activeTab", "partners");
        return "partner/form";
    }

    @GetMapping("/{id}/deliveries")
    public String deliveryPermForm(@PathVariable String id, Model model,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        Partner partner = partnerService.getById(id);
        model.addAttribute("partner", partner);
        model.addAttribute("deliveries", deliveryService.listOptions());
        model.addAttribute("selectedDeliveries", partnerService.getAssignedDeliveryIds(id));
        model.addAttribute("activeTab", "partners");
        return "partner/deliveries";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Partner partner,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        boolean isNew = partner.getId() == null || partner.getId().isBlank();
        if (!isNew) {
            Partner existing = partnerService.getById(partner.getId());
            partner.setCreateBy(existing.getCreateBy());
            partner.setCreateTime(existing.getCreateTime());
        } else {
            partner.setId(null);
            var user = userService.findByUsername(userDetails.getUsername());
            partner.setCreateBy(user != null ? user.getDisplayName() : userDetails.getUsername());
        }
        try {
            Partner saved = partnerService.save(partner);
            redirectAttributes.addFlashAttribute("success", isNew ? "服务商已创建" : "服务商已保存");
            return "redirect:/partners/" + saved.getId() + "/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return isNew ? "redirect:/partners/new" : "redirect:/partners/" + partner.getId() + "/edit";
        }
    }

    @PostMapping("/{id}/deliveries")
    public String saveDeliveryPerms(@PathVariable String id,
                                    @RequestParam(required = false) List<String> deliveries,
                                    RedirectAttributes redirectAttributes) {
        try {
            partnerService.saveDeliveryPermissions(id, deliveries);
            redirectAttributes.addFlashAttribute("success", "服务商交付授权已保存");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/partners/" + id + "/deliveries";
        }
        return "redirect:/partners";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            partnerService.delete(id);
            redirectAttributes.addFlashAttribute("success", "服务商已删除");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/partners";
    }

    private void addUserToModel(Model model, UserDetails userDetails) {
        if (userDetails != null) {
            var user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", user != null ? user.getDisplayName() : userDetails.getUsername());
        }
    }
}
