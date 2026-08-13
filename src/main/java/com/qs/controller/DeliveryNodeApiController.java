package com.qs.controller;

import com.qs.dto.DeliveryNodeDto;
import com.qs.dto.DeliveryNodeRequest;
import com.qs.entity.DeliveryNode;
import com.qs.entity.DeliveryNodeAttachment;
import com.qs.entity.DeliveryNodeChangeLog;
import com.qs.entity.DeliveryNodeMemo;
import com.qs.service.DeliveryNodeAttachmentService;
import com.qs.service.DeliveryNodeChangeLogService;
import com.qs.service.DeliveryNodeMemoService;
import com.qs.service.DeliveryNodeService;
import com.qs.service.FileStorageService;
import com.qs.service.UserService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/archives")
public class DeliveryNodeApiController {

    private final DeliveryNodeService deliveryNodeService;
    private final DeliveryNodeChangeLogService changeLogService;
    private final DeliveryNodeAttachmentService attachmentService;
    private final DeliveryNodeMemoService memoService;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    public DeliveryNodeApiController(DeliveryNodeService deliveryNodeService,
                                     DeliveryNodeChangeLogService changeLogService,
                                     DeliveryNodeAttachmentService attachmentService,
                                     DeliveryNodeMemoService memoService,
                                     FileStorageService fileStorageService,
                                     UserService userService) {
        this.deliveryNodeService = deliveryNodeService;
        this.changeLogService = changeLogService;
        this.attachmentService = attachmentService;
        this.memoService = memoService;
        this.fileStorageService = fileStorageService;
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
                                        @RequestBody DeliveryNodeRequest request,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        return deliveryNodeService.update(deliveryId, nodeId, request, resolveDisplayName(userDetails));
    }

    @PostMapping("/{deliveryId}/nodes/{nodeId}/confirm")
    public List<DeliveryNodeDto> confirm(@PathVariable String deliveryId,
                                         @PathVariable String nodeId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        return deliveryNodeService.confirm(deliveryId, nodeId, resolveDisplayName(userDetails));
    }

    @DeleteMapping("/{deliveryId}/nodes/{nodeId}")
    public List<DeliveryNodeDto> delete(@PathVariable String deliveryId,
                                        @PathVariable String nodeId) {
        return deliveryNodeService.delete(deliveryId, nodeId);
    }

    @GetMapping("/{deliveryId}/nodes/{nodeId}/changes")
    public List<Map<String, Object>> changes(@PathVariable String deliveryId,
                                             @PathVariable String nodeId) {
        deliveryNodeService.getOwnedNode(deliveryId, nodeId);
        return changeLogService.listByNodeId(nodeId).stream().map(this::toChangeMap).toList();
    }

    @GetMapping("/{deliveryId}/nodes/{nodeId}/attachments")
    public List<Map<String, Object>> attachments(@PathVariable String deliveryId,
                                                 @PathVariable String nodeId) {
        deliveryNodeService.getOwnedNode(deliveryId, nodeId);
        return attachmentService.listByNodeId(nodeId).stream().map(this::toAttachmentMap).toList();
    }

    @PostMapping("/{deliveryId}/nodes/{nodeId}/attachments")
    public List<Map<String, Object>> uploadAttachments(@PathVariable String deliveryId,
                                                       @PathVariable String nodeId,
                                                       @RequestParam(value = "files", required = false)
                                                       List<MultipartFile> files,
                                                       @AuthenticationPrincipal UserDetails userDetails)
            throws IOException {
        DeliveryNode node = deliveryNodeService.getOwnedNode(deliveryId, nodeId);
        attachmentService.uploadBatch(node, files, resolveDisplayName(userDetails));
        return attachmentService.listByNodeId(nodeId).stream().map(this::toAttachmentMap).toList();
    }

    @PostMapping("/{deliveryId}/nodes/{nodeId}/attachments/{attachmentId}/confirm")
    public List<Map<String, Object>> confirmAttachment(@PathVariable String deliveryId,
                                                       @PathVariable String nodeId,
                                                       @PathVariable String attachmentId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        DeliveryNodeAttachment attachment = ensureOwnedAttachment(deliveryId, nodeId, attachmentId);
        attachmentService.confirm(attachment.getId(), resolveDisplayName(userDetails));
        return attachmentService.listByNodeId(nodeId).stream().map(this::toAttachmentMap).toList();
    }

    @DeleteMapping("/{deliveryId}/nodes/{nodeId}/attachments/{attachmentId}")
    public List<Map<String, Object>> deleteAttachment(@PathVariable String deliveryId,
                                                      @PathVariable String nodeId,
                                                      @PathVariable String attachmentId) throws IOException {
        ensureOwnedAttachment(deliveryId, nodeId, attachmentId);
        attachmentService.delete(attachmentId);
        return attachmentService.listByNodeId(nodeId).stream().map(this::toAttachmentMap).toList();
    }

    @GetMapping("/{deliveryId}/nodes/{nodeId}/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String deliveryId,
                                                        @PathVariable String nodeId,
                                                        @PathVariable String attachmentId) throws IOException {
        DeliveryNodeAttachment attachment = ensureOwnedAttachment(deliveryId, nodeId, attachmentId);
        Path path = fileStorageService.load(attachment.getRelativePath());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(attachment.getContentType());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(mediaType)
                .contentLength(Files.size(path))
                .body(new InputStreamResource(Files.newInputStream(path)));
    }

    @GetMapping("/{deliveryId}/nodes/{nodeId}/memos")
    public List<Map<String, Object>> memos(@PathVariable String deliveryId,
                                           @PathVariable String nodeId) {
        deliveryNodeService.getOwnedNode(deliveryId, nodeId);
        return memoService.listByNodeId(nodeId).stream().map(this::toMemoMap).toList();
    }

    @PostMapping("/{deliveryId}/nodes/{nodeId}/memos")
    public List<Map<String, Object>> createMemo(@PathVariable String deliveryId,
                                                @PathVariable String nodeId,
                                                @RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        DeliveryNode node = deliveryNodeService.getOwnedNode(deliveryId, nodeId);
        String content = body != null ? body.get("content") : null;
        memoService.create(node, content, resolveDisplayName(userDetails));
        return memoService.listByNodeId(nodeId).stream().map(this::toMemoMap).toList();
    }

    @PostMapping("/{deliveryId}/nodes/{nodeId}/memos/{memoId}/confirm")
    public List<Map<String, Object>> confirmMemo(@PathVariable String deliveryId,
                                                 @PathVariable String nodeId,
                                                 @PathVariable String memoId,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        ensureOwnedMemo(deliveryId, nodeId, memoId);
        memoService.confirm(memoId, resolveDisplayName(userDetails));
        return memoService.listByNodeId(nodeId).stream().map(this::toMemoMap).toList();
    }

    @DeleteMapping("/{deliveryId}/nodes/{nodeId}/memos/{memoId}")
    public List<Map<String, Object>> deleteMemo(@PathVariable String deliveryId,
                                                @PathVariable String nodeId,
                                                @PathVariable String memoId) {
        ensureOwnedMemo(deliveryId, nodeId, memoId);
        memoService.delete(memoId);
        return memoService.listByNodeId(nodeId).stream().map(this::toMemoMap).toList();
    }

    private DeliveryNodeAttachment ensureOwnedAttachment(String deliveryId, String nodeId, String attachmentId) {
        deliveryNodeService.getOwnedNode(deliveryId, nodeId);
        DeliveryNodeAttachment attachment = attachmentService.getById(attachmentId);
        if (!nodeId.equals(attachment.getNodeId()) || !deliveryId.equals(attachment.getDeliveryId())) {
            throw new IllegalArgumentException("附件不属于该节点");
        }
        return attachment;
    }

    private DeliveryNodeMemo ensureOwnedMemo(String deliveryId, String nodeId, String memoId) {
        deliveryNodeService.getOwnedNode(deliveryId, nodeId);
        DeliveryNodeMemo memo = memoService.getById(memoId);
        if (!nodeId.equals(memo.getNodeId()) || !deliveryId.equals(memo.getDeliveryId())) {
            throw new IllegalArgumentException("备忘录不属于该节点");
        }
        return memo;
    }

    private Map<String, Object> toChangeMap(DeliveryNodeChangeLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("fieldName", log.getFieldName());
        map.put("fieldLabel", log.getFieldLabel());
        map.put("oldValue", log.getOldValue());
        map.put("newValue", log.getNewValue());
        map.put("changeBy", log.getChangeBy());
        map.put("changeTime", log.getChangeTime());
        return map;
    }

    private Map<String, Object> toAttachmentMap(DeliveryNodeAttachment attachment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", attachment.getId());
        map.put("originalName", attachment.getOriginalName());
        map.put("fileSize", attachment.getFileSize());
        map.put("createBy", attachment.getCreateBy());
        map.put("createTime", attachment.getCreateTime());
        map.put("confirmed", attachment.isConfirmed());
        map.put("confirmedBy", attachment.getConfirmedBy());
        map.put("confirmedTime", attachment.getConfirmedTime());
        return map;
    }

    private Map<String, Object> toMemoMap(DeliveryNodeMemo memo) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", memo.getId());
        map.put("content", memo.getContent());
        map.put("createBy", memo.getCreateBy());
        map.put("createTime", memo.getCreateTime());
        map.put("confirmed", memo.isConfirmed());
        map.put("confirmedBy", memo.getConfirmedBy());
        map.put("confirmedTime", memo.getConfirmedTime());
        return map;
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return "";
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
