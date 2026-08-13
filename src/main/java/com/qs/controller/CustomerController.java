package com.qs.controller;

import com.qs.entity.Customer;
import com.qs.service.CustomerService;
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

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final UserService userService;

    public CustomerController(CustomerService customerService, UserService userService) {
        this.customerService = customerService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("customers", customerService.search(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeTab", "customers");
        return "customer/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("customer", new Customer());
        model.addAttribute("activeTab", "customers");
        return "customer/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("deliveryCount", customerService.countDeliveries(id));
        model.addAttribute("activeTab", "customers");
        return "customer/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Customer customer,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        boolean isNew = customer.getId() == null || customer.getId().isBlank();
        if (!isNew) {
            Customer existing = customerService.getById(customer.getId());
            customer.setCreateBy(existing.getCreateBy());
            customer.setCreateTime(existing.getCreateTime());
        } else {
            customer.setId(null);
            var user = userService.findByUsername(userDetails.getUsername());
            customer.setCreateBy(user != null ? user.getDisplayName() : userDetails.getUsername());
        }
        try {
            Customer saved = customerService.save(customer);
            redirectAttributes.addFlashAttribute("success", isNew ? "使用单位已创建" : "使用单位已保存");
            return "redirect:/customers/" + saved.getId() + "/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return isNew ? "redirect:/customers/new" : "redirect:/customers/" + customer.getId() + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            customerService.delete(id);
            redirectAttributes.addFlashAttribute("success", "使用单位已删除");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customers";
    }

    private void addUserToModel(Model model, UserDetails userDetails) {
        if (userDetails != null) {
            var user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", user != null ? user.getDisplayName() : userDetails.getUsername());
        }
    }
}
