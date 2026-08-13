package com.qs.service;

import com.qs.config.UploadProperties;
import com.qs.entity.Delivery;
import com.qs.entity.DeliveryAttachment;
import com.qs.repository.DeliveryAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DeliveryAttachmentService {

    private final DeliveryAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UploadProperties uploadProperties;

    public DeliveryAttachmentService(DeliveryAttachmentRepository attachmentRepository,
                                     FileStorageService fileStorageService,
                                     UploadProperties uploadProperties) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.uploadProperties = uploadProperties;
    }

    public List<DeliveryAttachment> listByDeliveryId(String deliveryId) {
        return attachmentRepository.findByDeliveryIdOrderByCreateTimeDesc(deliveryId);
    }

    public DeliveryAttachment getById(String id) {
        return attachmentRepository.findByIdWithDelivery(id)
                .orElseThrow(() -> new IllegalArgumentException("附件不存在"));
    }

    @Transactional
    public void upload(Delivery delivery, MultipartFile file, String createBy) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        validateFile(file);
        String relativePath = fileStorageService.storeDelivery(delivery.getId(), file);
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String storedName = relativePath.substring(relativePath.lastIndexOf('/') + 1);

        DeliveryAttachment attachment = new DeliveryAttachment();
        attachment.setDelivery(delivery);
        attachment.setOriginalName(originalName);
        attachment.setStoredName(storedName);
        attachment.setRelativePath(relativePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setCreateBy(createBy);
        attachmentRepository.save(attachment);
    }

    @Transactional
    public void uploadBatch(Delivery delivery, List<MultipartFile> files, String createBy) throws IOException {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            upload(delivery, file, createBy);
        }
    }

    @Transactional
    public void delete(String attachmentId) throws IOException {
        DeliveryAttachment attachment = getById(attachmentId);
        fileStorageService.delete(attachment.getRelativePath());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteByDeliveryId(String deliveryId) throws IOException {
        List<DeliveryAttachment> attachments = attachmentRepository.findByDeliveryIdOrderByCreateTimeDesc(deliveryId);
        for (DeliveryAttachment attachment : attachments) {
            fileStorageService.delete(attachment.getRelativePath());
        }
        attachmentRepository.deleteByDeliveryId(deliveryId);
        fileStorageService.deleteDeliveryDir(deliveryId);
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > uploadProperties.getMaxFileSize()) {
            throw new IllegalArgumentException("附件大小不能超过 50MB");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename.replace("\\", "_").replace("/", "_");
    }
}
