package com.qs.controller;

import com.qs.entity.Ticket;
import com.qs.enums.AttachmentType;
import com.qs.enums.OrderType;
import com.qs.enums.TicketStatus;
import com.qs.service.AnalysisService;
import com.qs.service.DeliveryService;
import com.qs.service.ConfirmationReportWordExporter;
import com.qs.service.PermissionService;
import com.qs.service.ReminderService;
import com.qs.service.TicketAttachmentService;
import com.qs.service.TicketProcessService;
import com.qs.service.TicketService;
import com.qs.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final DeliveryService deliveryService;
    private final AnalysisService analysisService;
    private final UserService userService;
    private final ReminderService reminderService;
    private final TicketAttachmentService attachmentService;
    private final TicketProcessService processService;
    private final PermissionService permissionService;
    private final ConfirmationReportWordExporter confirmationReportWordExporter;

    public TicketController(TicketService ticketService, DeliveryService deliveryService,
                            AnalysisService analysisService, UserService userService,
                            ReminderService reminderService, TicketAttachmentService attachmentService,
                            TicketProcessService processService, PermissionService permissionService,
                            ConfirmationReportWordExporter confirmationReportWordExporter) {
        this.ticketService = ticketService;
        this.deliveryService = deliveryService;
        this.analysisService = analysisService;
        this.userService = userService;
        this.reminderService = reminderService;
        this.attachmentService = attachmentService;
        this.processService = processService;
        this.permissionService = permissionService;
        this.confirmationReportWordExporter = confirmationReportWordExporter;
    }

    @GetMapping
    public String list(@RequestParam(required = false) List<String> status,
                       @RequestParam(required = false) List<String> deliveryId,
                       @RequestParam(required = false) List<String> systemId,
                       @RequestParam(required = false) List<String> menu,
                       @RequestParam(required = false) String handler,
                       @RequestParam(required = false) String submitter,
                       @RequestParam(required = false) String keyword,
                       Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        reminderService.checkAndCreateNow();
        Set<String> allowedArchives = permissionService.getAllowedDeliveryIds(userDetails.getUsername());
        List<String> statusFilters = cleanList(status);
        List<String> archiveFilters = cleanList(deliveryId).stream()
                .filter(allowedArchives::contains)
                .toList();
        if (archiveFilters.isEmpty()) {
            // 无授权医院时不可看到任何工单；有授权但未筛选时限定在授权范围内
            archiveFilters = allowedArchives.isEmpty()
                    ? List.of("__NO_ARCHIVE_ACCESS__")
                    : List.copyOf(allowedArchives);
        }
        List<String> systemFilters = cleanList(systemId);
        List<String> menuFilters = cleanList(menu);

        List<String> menuOptions = analysisService.listMenuTitlesByProjectIds(systemFilters);
        List<String> menuAliases;
        if (!menuFilters.isEmpty()) {
            menuAliases = analysisService.resolveMenuAliases(menuFilters);
        } else if (!systemFilters.isEmpty()) {
            menuAliases = menuOptions;
        } else {
            menuAliases = List.of();
        }

        List<Ticket> tickets = ticketService.search(
                statusFilters, handler, submitter, keyword, menuAliases, archiveFilters);
        List<String> ticketIds = tickets.stream().map(Ticket::getId).collect(Collectors.toList());
        Set<String> confirmTicketIds = attachmentService.findTicketIdsWithConfirmation(ticketIds);
        Set<String> planConfirmTicketIds = attachmentService.findTicketIdsWithPlanConfirmation(ticketIds);
        Set<String> lockedConfirmTicketIds = attachmentService.findTicketIdsWithLockedConfirmation(ticketIds);
        Set<String> lockedPlanConfirmTicketIds = attachmentService.findTicketIdsWithLockedPlanConfirmation(ticketIds);
        model.addAttribute("returnUrl", "/tickets");
        model.addAttribute("tickets", tickets);
        model.addAttribute("confirmTicketIds", confirmTicketIds);
        model.addAttribute("planConfirmTicketIds", planConfirmTicketIds);
        model.addAttribute("lockedConfirmTicketIds", lockedConfirmTicketIds);
        model.addAttribute("lockedPlanConfirmTicketIds", lockedPlanConfirmTicketIds);
        Set<String> processTicketIds = processService.findTicketIdsWithAnyProcess(ticketIds);
        Set<String> deletableTicketIds = ticketIds.stream()
                .filter(id -> !processTicketIds.contains(id)
                        && !confirmTicketIds.contains(id)
                        && !planConfirmTicketIds.contains(id))
                .collect(Collectors.toSet());
        model.addAttribute("deletableTicketIds", deletableTicketIds);
        model.addAttribute("statusFilters", statusFilters);
        model.addAttribute("deliveryIdFilters", cleanList(deliveryId));
        model.addAttribute("systemIdFilters", systemFilters);
        model.addAttribute("menuFilters", menuFilters);
        model.addAttribute("handlerFilter", handler);
        model.addAttribute("submitterFilter", submitter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("menuOptions", menuOptions);
        model.addAttribute("deliveryOptions", deliveryService.listOptions(allowedArchives));
        model.addAttribute("systemOptions", analysisService.listProjects());
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        return "ticket/list";
    }

    private static List<String> cleanList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    @GetMapping("/new")
    public String createForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        Set<String> allowedArchives = permissionService.getAllowedDeliveryIds(userDetails.getUsername());
        Ticket ticket = new Ticket();
        var user = userService.findByUsername(userDetails.getUsername());
        ticket.setSubmitter(user != null ? user.getDisplayName() : userDetails.getUsername());
        ticket.setStatus(TicketStatus.SUBMITTED.getLabel());
        model.addAttribute("ticket", ticket);
        model.addAttribute("deliveryOptions", deliveryService.listOptions(allowedArchives));
        model.addAttribute("orderTypes", Arrays.asList(OrderType.values()));
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        return "ticket/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model,
                         @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        addUserToModel(model, userDetails);
        model.addAttribute("ticket", ticket);
        if (ticket.getDelivery() != null) {
            model.addAttribute("deliveryDisplayName", deliveryService.buildDisplayName(ticket.getDelivery()));
        }
        addAttachmentModel(model, id);
        model.addAttribute("followUps", ticketService.listFollowUps(id));
        model.addAttribute("processes", processService.listTreeByTicketId(id));
        model.addAttribute("changeLogs", ticketService.listChangeLogs(id));
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        model.addAttribute("attachmentEditable", true);
        model.addAttribute("hasConfirmation", attachmentService.hasConfirmation(id));
        model.addAttribute("hasPlanConfirmation", attachmentService.hasPlanConfirmation(id));
        model.addAttribute("canDeleteTicket", ticketService.canDelete(id));
        return "ticket/detail";
    }

    /** 列表页报告弹窗：按工单实时拉取方案/完成确认报告 */
    @GetMapping("/{id}/report-attachments")
    @ResponseBody
    public List<Map<String, Object>> listReportAttachments(@PathVariable String id,
                                                           @RequestParam String type,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        AttachmentType attachmentType = resolveReportType(type);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<Map<String, Object>> result = new ArrayList<>();
        var reports = attachmentType == AttachmentType.PLAN_CONFIRM
                ? attachmentService.listPlanConfirmations(id)
                : attachmentService.listConfirmations(id);
        for (var a : reports) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("originalName", a.getOriginalName());
            row.put("createBy", a.getCreateBy());
            row.put("createTime", a.getCreateTime() != null ? a.getCreateTime().format(fmt) : "");
            row.put("confirmed", a.isConfirmed());
            result.add(row);
        }
        return result;
    }

    private static AttachmentType resolveReportType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("报告类型不能为空");
        }
        String t = type.trim().toUpperCase();
        if ("PLAN".equals(t) || "PLAN_CONFIRM".equals(t)) {
            return AttachmentType.PLAN_CONFIRM;
        }
        if ("CONFIRM".equals(t)) {
            return AttachmentType.CONFIRM;
        }
        throw new IllegalArgumentException("不支持的报告类型: " + type);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        addUserToModel(model, userDetails);
        Set<String> allowedArchives = permissionService.getAllowedDeliveryIds(userDetails.getUsername());
        model.addAttribute("ticket", ticket);
        addAttachmentModel(model, id);
        model.addAttribute("followUps", ticketService.listFollowUps(id));
        model.addAttribute("deliveryOptions", deliveryService.listOptions(allowedArchives));
        model.addAttribute("orderTypes", Arrays.asList(OrderType.values()));
        model.addAttribute("ticketStatuses", Arrays.asList(TicketStatus.values()));
        model.addAttribute("activeTab", "tickets");
        model.addAttribute("attachmentEditable", true);
        return "ticket/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Ticket ticket,
                       @RequestParam String deliveryId,
                       @RequestParam(required = false) String newFollowUp,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        if (!permissionService.canAccessDelivery(userDetails.getUsername(), deliveryId)) {
            redirectAttributes.addFlashAttribute("error", "无权操作该医院/项目的工单");
            return "redirect:/tickets";
        }
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
            Ticket saved = ticketService.save(ticket, deliveryId, newFollowUp, createBy);
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
        ensureTicketAccess(userDetails, ticket);
        String createBy = resolveDisplayName(userDetails);
        try {
            attachmentService.uploadBatch(ticket, images, AttachmentType.IMAGE, createBy);
            attachmentService.uploadBatch(ticket, files, AttachmentType.FILE, createBy);
            redirectAttributes.addFlashAttribute("success", "上传成功");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(id, returnTo);
    }

    @PostMapping("/{id}/confirmation")
    public String uploadConfirmation(@PathVariable String id,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam(defaultValue = "detail") String returnTo,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "请选择确认报告文件");
            return resolveTicketReturn(id, returnTo);
        }
        try {
            attachmentService.upload(ticket, file, AttachmentType.CONFIRM, resolveDisplayName(userDetails));
            redirectAttributes.addFlashAttribute("success", "确认报告已上传");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(id, returnTo);
    }

    @PostMapping("/{id}/plan-confirmation")
    public String uploadPlanConfirmation(@PathVariable String id,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "list") String returnTo,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "请选择方案确认报告文件");
            return resolveTicketReturn(id, returnTo);
        }
        try {
            attachmentService.upload(ticket, file, AttachmentType.PLAN_CONFIRM, resolveDisplayName(userDetails));
            redirectAttributes.addFlashAttribute("success", "方案确认报告已上传");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(id, returnTo);
    }

    @PostMapping("/attachments/{attachmentId}/confirm")
    public String confirmAttachment(@PathVariable String attachmentId,
                                    @RequestParam String ticketId,
                                    @RequestParam(defaultValue = "detail") String returnTo,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(ticketId);
        ensureTicketAccess(userDetails, ticket);
        try {
            attachmentService.confirmReport(attachmentId, resolveDisplayName(userDetails));
            redirectAttributes.addFlashAttribute("success", "报告已确认存档");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(ticketId, returnTo);
    }

    @PostMapping("/attachments/{attachmentId}/delete")
    public String deleteAttachment(@PathVariable String attachmentId,
                                   @RequestParam String ticketId,
                                   @RequestParam(defaultValue = "edit") String returnTo,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(ticketId);
        ensureTicketAccess(userDetails, ticket);
        try {
            attachmentService.delete(attachmentId);
            redirectAttributes.addFlashAttribute("success", "已删除");
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(ticketId, returnTo);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            Ticket ticket = ticketService.getById(id);
            ensureTicketAccess(userDetails, ticket);
            ticketService.delete(id);
            redirectAttributes.addFlashAttribute("success", "工单已删除");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable String id,
                           @RequestParam(defaultValue = "list") String returnTo,
                           @RequestParam(required = false) String content,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        try {
            if (content != null && !content.isBlank()) {
                processService.markCompletedWithProcess(id, content, resolveDisplayName(userDetails));
            } else {
                ticketService.markCompleted(id, resolveDisplayName(userDetails));
            }
            redirectAttributes.addFlashAttribute("success", "工单已标记为已完成");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(id, returnTo);
    }

    @PostMapping("/{id}/handled")
    public String markHandled(@PathVariable String id,
                              @RequestParam String handleMethod,
                              @RequestParam String content,
                              @RequestParam(defaultValue = "list") String returnTo,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        try {
            processService.markHandled(id, handleMethod, content, resolveDisplayName(userDetails));
            redirectAttributes.addFlashAttribute("success", "已记录处理进程，状态更新为「已处理」");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(id, returnTo);
    }

    @PostMapping("/{id}/process-reply")
    public String processReply(@PathVariable String id,
                               @RequestParam(required = false) String parentId,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "detail") String returnTo,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        try {
            processService.reply(id, parentId, content, resolveDisplayName(userDetails));
            redirectAttributes.addFlashAttribute("success", "核对回复已保存");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(id, returnTo);
    }

    @PostMapping("/{id}/need-feedback")
    public String needFeedback(@PathVariable String id,
                               @RequestParam(required = false) String content,
                               @RequestParam(defaultValue = "list") String returnTo,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        try {
            processService.markNeedFeedback(id, content, resolveDisplayName(userDetails));
            redirectAttributes.addFlashAttribute("success", "已标记为「待反馈调整」，请开发按反馈继续处理");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return resolveTicketReturn(id, returnTo);
    }

    @GetMapping("/{id}/confirmation-print")
    public String confirmationPrint(@PathVariable String id, Model model,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        model.addAttribute("ticket", ticket);
        if (ticket.getDelivery() != null) {
            model.addAttribute("deliveryDisplayName", deliveryService.buildDisplayName(ticket.getDelivery()));
        }
        model.addAttribute("followUps", ticketService.listFollowUps(id));
        model.addAttribute("printDate", java.time.LocalDate.now());
        return "ticket/confirmation-print";
    }

    @GetMapping("/{id}/confirmation-export")
    public void confirmationExport(@PathVariable String id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   HttpServletResponse response) throws IOException {
        Ticket ticket = ticketService.getById(id);
        ensureTicketAccess(userDetails, ticket);
        byte[] bytes = confirmationReportWordExporter.export(ticket, ticketService.listFollowUps(id));
        String fileName = confirmationReportWordExporter.buildFileName(ticket);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
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

    private String resolveTicketReturn(String ticketId, String returnTo) {
        if ("list".equals(returnTo)) {
            return "redirect:/tickets";
        }
        if ("detail".equals(returnTo)) {
            return "redirect:/tickets/" + ticketId;
        }
        return "redirect:/tickets/" + ticketId + "/edit";
    }

    private void addAttachmentModel(Model model, String ticketId) {
        model.addAttribute("ticketImages", attachmentService.listImages(ticketId));
        model.addAttribute("ticketFiles", attachmentService.listFiles(ticketId));
        model.addAttribute("ticketConfirmations", attachmentService.listConfirmations(ticketId));
        model.addAttribute("ticketPlanConfirmations", attachmentService.listPlanConfirmations(ticketId));
    }

    private void addUserToModel(Model model, UserDetails userDetails) {
        if (userDetails != null) {
            var user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", user != null ? user.getDisplayName() : userDetails.getUsername());
        }
    }

    private void ensureTicketAccess(UserDetails userDetails, Ticket ticket) {
        if (ticket.getDelivery() == null
                || !permissionService.canAccessDelivery(userDetails.getUsername(), ticket.getDelivery().getId())) {
            throw new IllegalArgumentException("无权访问该工单所属医院/项目");
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
