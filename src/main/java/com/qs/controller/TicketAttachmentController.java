package com.qs.controller;

import com.qs.entity.TicketAttachment;
import com.qs.service.TicketAttachmentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequestMapping("/tickets/attachments")
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;
    private final com.qs.service.FileStorageService fileStorageService;

    public TicketAttachmentController(TicketAttachmentService attachmentService,
                                      com.qs.service.FileStorageService fileStorageService) {
        this.attachmentService = attachmentService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id,
                                                        @RequestParam(defaultValue = "false") boolean inline)
            throws IOException {
        TicketAttachment attachment;
        try {
            attachment = attachmentService.getById(id);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
        Path path = fileStorageService.load(attachment.getRelativePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = (inline && attachment.isImage() ? "inline" : "attachment")
                + "; filename*=UTF-8''" + encodedName;

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(attachment.getContentType());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType)
                .contentLength(Files.size(path))
                .body(new InputStreamResource(Files.newInputStream(path)));
    }
}
