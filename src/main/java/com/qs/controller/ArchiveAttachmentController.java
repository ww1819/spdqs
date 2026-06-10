package com.qs.controller;

import com.qs.entity.ArchiveAttachment;
import com.qs.service.ArchiveAttachmentService;
import com.qs.service.FileStorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequestMapping("/archives/attachments")
public class ArchiveAttachmentController {

    private final ArchiveAttachmentService attachmentService;
    private final FileStorageService fileStorageService;

    public ArchiveAttachmentController(ArchiveAttachmentService attachmentService,
                                       FileStorageService fileStorageService) {
        this.attachmentService = attachmentService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) throws IOException {
        ArchiveAttachment attachment;
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
        String disposition = "attachment; filename*=UTF-8''" + encodedName;

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
