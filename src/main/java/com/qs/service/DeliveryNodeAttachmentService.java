package com.qs.service;

import com.qs.config.UploadProperties;
import com.qs.entity.DeliveryNode;
import com.qs.entity.DeliveryNodeAttachment;
import com.qs.repository.DeliveryNodeAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryNodeAttachmentService {

    private final DeliveryNodeAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UploadProperties uploadProperties;

    public DeliveryNodeAttachmentService(DeliveryNodeAttachmentRepository attachmentRepository,
                                         FileStorageService fileStorageService,
                                         UploadProperties uploadProperties) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.uploadProperties = uploadProperties;
    }

    public long countByNodeId(String nodeId) {
        return attachmentRepository.countByNodeId(nodeId);
    }

    public List<DeliveryNodeAttachment> listByNodeId(String nodeId) {
        return attachmentRepository.findByNodeIdOrderByCreateTimeDesc(nodeId);
    }

    public DeliveryNodeAttachment getById(String id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("附件不存在"));
    }

    @Transactional
    public DeliveryNodeAttachment upload(DeliveryNode node, MultipartFile file, String createBy) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择文件");
        }
        if (file.getSize() > uploadProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("附件大小不能超过 50MB");
        }
        String relativePath = fileStorageService.storeDeliveryNode(node.getDeliveryId(), node.getId(), file);
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String storedName = relativePath.substring(relativePath.lastIndexOf('/') + 1);

        DeliveryNodeAttachment attachment = new DeliveryNodeAttachment();
        attachment.setNodeId(node.getId());
        attachment.setDeliveryId(node.getDeliveryId());
        attachment.setOriginalName(originalName);
        attachment.setStoredName(storedName);
        attachment.setRelativePath(relativePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setCreateBy(createBy);
        return attachmentRepository.save(attachment);
    }

    @Transactional
    public void uploadBatch(DeliveryNode node, List<MultipartFile> files, String createBy) throws IOException {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                upload(node, file, createBy);
            }
        }
    }

    @Transactional
    public void confirm(String attachmentId, String confirmedBy) {
        DeliveryNodeAttachment attachment = getById(attachmentId);
        if (attachment.isConfirmed()) {
            return;
        }
        attachment.setConfirmed(true);
        attachment.setConfirmedBy(confirmedBy);
        attachment.setConfirmedTime(LocalDateTime.now());
        attachmentRepository.save(attachment);
    }

    @Transactional
    public void delete(String attachmentId) throws IOException {
        DeliveryNodeAttachment attachment = getById(attachmentId);
        if (attachment.isConfirmed()) {
            throw new IllegalArgumentException("已确认存档，暂不支持删除");
        }
        fileStorageService.delete(attachment.getRelativePath());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteByNodeId(String nodeId) throws IOException {
        List<DeliveryNodeAttachment> list = attachmentRepository.findByNodeIdOrderByCreateTimeDesc(nodeId);
        for (DeliveryNodeAttachment attachment : list) {
            fileStorageService.delete(attachment.getRelativePath());
        }
        attachmentRepository.deleteByNodeId(nodeId);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename.replace("\\", "_").replace("/", "_");
    }
}
