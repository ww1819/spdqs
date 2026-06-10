package com.qs.controller;

import com.qs.entity.Ticket;
import com.qs.enums.AttachmentType;
import com.qs.enums.OrderType;
import com.qs.enums.TicketStatus;
import com.qs.service.ArchiveService;
import com.qs.service.ReminderService;
import com.qs.service.TicketAttachmentService;
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

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ArchiveService archiveService;
    private final UserService userService;
    private final ReminderService reminderService;
    private final TicketAttachmentService attachmentService;

    public TicketController(TicketService ticketService, ArchiveService archiveService,
                            UserService userService, ReminderService reminderService,
                            TicketAttachmentService attachmentService) {
        this.ticketService = ticketService;
        this.archiveService = archiveService;
        this.userService = userService;
        this.reminderService = reminderService;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String handler,
                       @RequestParam(required = false) String submitter,
                       @RequestParam(required = false) String keyword,
                       Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        reminderService.checkAndCreateNow();
        model.addAttribute("returnUrl", "/tickets");
        model.addAttribute("tickets", ticketService.search(status, handler, submitter, keyword));
        model.addAttribute("statusFilter", status);
        model.addAttribute("handlerFilter", handler);
        model.addAttribute("submitterFilter", submitter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        return "ticket/list";
    }

    @GetMapping("/new")
    public String createForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        Ticket ticket = new Ticket();
        var user = userService.findByUsername(userDetails.getUsername());
        ticket.setSubmitter(user != null ? user.getDisplayName() : userDetails.getUsername());
        ticket.setStatus(TicketStatus.SUBMITTED.getLabel());
        model.addAttribute("ticket", ticket);
        model.addAttribute("archiveOptions", archiveService.listOptions());
        model.addAttribute("orderTypes", Arrays.asList(OrderType.values()));
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        return "ticket/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("ticket", ticketService.getById(id));
        addAttachmentModel(model, id);
        model.addAttribute("followUps", ticketService.listFollowUps(id));
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        model.addAttribute("attachmentEditable", true);
        return "ticket/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("ticket", ticketService.getById(id));
        addAttachmentModel(model, id);
        model.addAttribute("followUps", ticketService.listFollowUps(id));
        model.addAttribute("archiveOptions", archiveService.listOptions());
        model.addAttribute("orderTypes", Arrays.asList(OrderType.values()));
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        model.addAttribute("attachmentEditable", true);
        return "ticket/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Ticket ticket,
                       @RequestParam String archiveId,
                       @RequestParam(required = false) String newFollowUp,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        if (ticket.getId() != null && !ticket.getId().isBlank()) {
            Ticket existing = ticketService.getById(ticket.getId());
            ticket.setCreateTime(existing.getCreateTime());
        } else {
            ticket.setId(null);
            if (ticket.getStatus() == null || ticket.getStatus().isBlank()) {
                ticket.setStatus(TicketStatus.SUBMITTED.getLabel());
            }
        }
        String createBy = resolveDisplayName(userDetails);
        try {
            Ticket saved = ticketService.save(ticket, archiveId, newFollowUp, createBy);
            redirectAttributes.addFlashAttribute("success", "工单保存成功，可继续上传图片和附件");
            return "redirect:/tickets/" + saved.getId() + "/edit";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("requireFollowUpModal", true);
            if (ticket.getId() != null && !ticket.getId().isBlank()) {
                return "redirect:/tickets/" + ticket.getId() + "/edit";
            }
            return "redirect:/tickets/new";
        }
    }

    @PostMapping("/{id}/attachments")
    public String uploadAttachments(@PathVariable String id,
                                    @RequestParam(value = "images", required = false) List<MultipartFile> images,
                                    @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                    @RequestParam(defaultValue = "edit") String returnTo,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(id);
        String createBy = resolveDisplayName(userDetails);
        try {
            attachmentService.uploadBatch(ticket, images, AttachmentType.IMAGE, createBy);
            attachmentService.uploadBatch(ticket, files, AttachmentType.FILE, createBy);
            redirectAttributes.addFlashAttribute("success", "上传成功");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        if ("detail".equals(returnTo)) {
            return "redirect:/tickets/" + id;
        }
        return "redirect:/tickets/" + id + "/edit";
    }

    @PostMapping("/attachments/{attachmentId}/delete")
    public String deleteAttachment(@PathVariable String attachmentId,
                                   @RequestParam String ticketId,
                                   @RequestParam(defaultValue = "edit") String returnTo,
                                   RedirectAttributes redirectAttributes) {
        try {
            attachmentService.delete(attachmentId);
            redirectAttributes.addFlashAttribute("success", "已删除");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        if ("detail".equals(returnTo)) {
            return "redirect:/tickets/" + ticketId;
        }
        return "redirect:/tickets/" + ticketId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        ticketService.delete(id);
        redirectAttributes.addFlashAttribute("success", "工单已删除");
        return "redirect:/tickets";
    }

    @PostMapping("/{id}/pending-upgrade")
    public String pendingUpgrade(@PathVariable String id, RedirectAttributes redirectAttributes) {
        ticketService.markPendingUpgrade(id);
        redirectAttributes.addFlashAttribute("success", "已设为待升级，目标完成时间为今天");
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/upgraded")
    public String upgraded(@PathVariable String id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        ticketService.markUpgraded(id, resolveDisplayName(userDetails));
        redirectAttributes.addFlashAttribute("success", "已标记为已升级完成");
        return "redirect:/dashboard";
    }

    private void addAttachmentModel(Model model, String ticketId) {
        model.addAttribute("ticketImages", attachmentService.listImages(ticketId));
        model.addAttribute("ticketFiles", attachmentService.listFiles(ticketId));
    }

    private void addUserToModel(Model model, UserDetails userDetails) {
        if (userDetails != null) {
            var user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", user != null ? user.getDisplayName() : userDetails.getUsername());
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
