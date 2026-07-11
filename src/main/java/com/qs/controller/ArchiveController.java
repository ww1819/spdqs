package com.qs.controller;



import com.qs.dto.ArchiveView;

import com.qs.entity.Archive;

import com.qs.enums.ArchiveStatus;

import com.qs.service.ArchiveAttachmentService;

import com.qs.service.ArchiveService;

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

@RequestMapping("/archives")

public class ArchiveController {



    private final ArchiveService archiveService;

    private final TicketService ticketService;

    private final UserService userService;

    private final ArchiveAttachmentService attachmentService;



    public ArchiveController(ArchiveService archiveService, TicketService ticketService,

                             UserService userService, ArchiveAttachmentService attachmentService) {

        this.archiveService = archiveService;

        this.ticketService = ticketService;

        this.userService = userService;

        this.attachmentService = attachmentService;

    }



    @GetMapping

    public String list(@RequestParam(required = false) String status,

                       @RequestParam(required = false) String keyword,

                       Model model,

                       @AuthenticationPrincipal UserDetails userDetails) {

        addUserToModel(model, userDetails);

        model.addAttribute("returnUrl", "/archives");

        model.addAttribute("archives", archiveService.listAll(status, keyword));

        model.addAttribute("statusFilter", status);

        model.addAttribute("keyword", keyword);

        model.addAttribute("statuses", Arrays.asList(ArchiveStatus.values()));

        model.addAttribute("activeTab", "archives");

        return "archive/list";

    }



    @GetMapping("/new")

    public String createForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {

        addUserToModel(model, userDetails);

        model.addAttribute("archive", new Archive());

        model.addAttribute("activeTab", "archives");

        return "archive/form";

    }



    @GetMapping("/{id}")

    public String detail(@PathVariable String id, Model model,

                         @AuthenticationPrincipal UserDetails userDetails) {

        addUserToModel(model, userDetails);

        model.addAttribute("view", archiveService.getView(id));

        model.addAttribute("archive", archiveService.getById(id));

        addAttachmentModel(model, id);

        model.addAttribute("tickets", ticketService.listByArchiveId(id));

        model.addAttribute("activeTab", "archives");

        return "archive/detail";

    }



    @GetMapping("/{id}/nodes")

    public String nodes(@PathVariable String id, Model model,

                        @AuthenticationPrincipal UserDetails userDetails) {

        addUserToModel(model, userDetails);

        model.addAttribute("view", archiveService.getView(id));

        model.addAttribute("archive", archiveService.getById(id));

        model.addAttribute("nodeTypes", Arrays.asList(com.qs.enums.ArchiveNodeType.values()));

        model.addAttribute("activeTab", "archives");

        return "archive/nodes";

    }



    @GetMapping("/{id}/edit")

    public String editForm(@PathVariable String id, Model model,

                           @AuthenticationPrincipal UserDetails userDetails) {

        addUserToModel(model, userDetails);

        model.addAttribute("archive", archiveService.getById(id));

        addAttachmentModel(model, id);

        model.addAttribute("activeTab", "archives");

        return "archive/form";

    }



    @PostMapping("/save")

    public String save(@ModelAttribute Archive archive,

                       @AuthenticationPrincipal UserDetails userDetails,

                       RedirectAttributes redirectAttributes) {

        if (archive.getId() != null && !archive.getId().isBlank()) {

            Archive existing = archiveService.getById(archive.getId());

            archive.setCreateBy(existing.getCreateBy());

            archive.setCreateTime(existing.getCreateTime());

        } else {

            archive.setId(null);

            var user = userService.findByUsername(userDetails.getUsername());

            archive.setCreateBy(user != null ? user.getDisplayName() : userDetails.getUsername());

        }

        Archive saved = archiveService.save(archive);

        redirectAttributes.addFlashAttribute("success", "档案保存成功，可继续上传附件");

        return "redirect:/archives/" + saved.getId() + "/edit";

    }



    @PostMapping("/{id}/attachments")

    public String uploadAttachments(@PathVariable String id,

                                    @RequestParam(value = "files", required = false) List<MultipartFile> files,

                                    @RequestParam(defaultValue = "edit") String returnTo,

                                    @AuthenticationPrincipal UserDetails userDetails,

                                    RedirectAttributes redirectAttributes) {

        Archive archive = archiveService.getById(id);

        String createBy = resolveDisplayName(userDetails);

        try {

            attachmentService.uploadBatch(archive, files, createBy);

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

                                   @RequestParam String archiveId,

                                   @RequestParam(defaultValue = "edit") String returnTo,

                                   RedirectAttributes redirectAttributes) {

        try {

            attachmentService.delete(attachmentId);

            redirectAttributes.addFlashAttribute("success", "已删除");

        } catch (IllegalArgumentException | IOException ex) {

            redirectAttributes.addFlashAttribute("error", ex.getMessage());

        }

        if ("detail".equals(returnTo)) {

            return "redirect:/archives/" + archiveId;

        }

        return "redirect:/archives/" + archiveId + "/edit";

    }



    @PostMapping("/{id}/delete")

    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {

        archiveService.delete(id);

        redirectAttributes.addFlashAttribute("success", "档案已删除");

        return "redirect:/archives";

    }



    private void addAttachmentModel(Model model, String archiveId) {

        model.addAttribute("archiveAttachments", attachmentService.listByArchiveId(archiveId));

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

