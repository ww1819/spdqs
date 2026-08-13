package com.qs.service;

import com.qs.config.UploadProperties;
import com.qs.entity.Ticket;
import com.qs.entity.TicketAttachment;
import com.qs.enums.AttachmentType;
import com.qs.repository.TicketAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TicketAttachmentService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    private final TicketAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UploadProperties uploadProperties;

    public TicketAttachmentService(TicketAttachmentRepository attachmentRepository,
                                   FileStorageService fileStorageService,
                                   UploadProperties uploadProperties) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.uploadProperties = uploadProperties;
    }

    public List<TicketAttachment> listByTicketId(String ticketId) {
        return attachmentRepository.findByTicketIdOrderByCreateTimeDesc(ticketId);
    }

    public List<TicketAttachment> listImages(String ticketId) {
        return attachmentRepository.findByTicketIdAndType(ticketId, AttachmentType.IMAGE);
    }

    public List<TicketAttachment> listFiles(String ticketId) {
        return attachmentRepository.findByTicketIdAndType(ticketId, AttachmentType.FILE);
    }

    public List<TicketAttachment> listConfirmations(String ticketId) {
        return attachmentRepository.findByTicketIdAndType(ticketId, AttachmentType.CONFIRM);
    }

    public Set<String> findTicketIdsWithConfirmation(Collection<String> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(attachmentRepository.findTicketIdsByType(ticketIds, AttachmentType.CONFIRM));
    }

    public boolean hasConfirmation(String ticketId) {
        return !listConfirmations(ticketId).isEmpty();
    }

    public List<TicketAttachment> listPlanConfirmations(String ticketId) {
        return attachmentRepository.findByTicketIdAndType(ticketId, AttachmentType.PLAN_CONFIRM);
    }

    public Set<String> findTicketIdsWithPlanConfirmation(Collection<String> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(attachmentRepository.findTicketIdsByType(ticketIds, AttachmentType.PLAN_CONFIRM));
    }

    public Set<String> findTicketIdsWithLockedConfirmation(Collection<String> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(attachmentRepository.findTicketIdsByTypeAndConfirmed(ticketIds, AttachmentType.CONFIRM));
    }

    public Set<String> findTicketIdsWithLockedPlanConfirmation(Collection<String> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(attachmentRepository.findTicketIdsByTypeAndConfirmed(ticketIds, AttachmentType.PLAN_CONFIRM));
    }

    @Transactional(readOnly = true)
    public Map<String, List<TicketAttachment>> mapReportsByTicketIds(Collection<String> ticketIds, AttachmentType type) {
        if (ticketIds == null || ticketIds.isEmpty() || type == null) {
            return Collections.emptyMap();
        }
        Map<String, List<TicketAttachment>> result = new LinkedHashMap<>();
        for (TicketAttachment attachment : attachmentRepository.findByTicketIdsAndType(ticketIds, type)) {
            String tid = attachment.getTicket() != null ? attachment.getTicket().getId() : null;
            if (tid == null || tid.isBlank()) {
                continue;
            }
            result.computeIfAbsent(tid, k -> new java.util.ArrayList<>()).add(attachment);
        }
        return result;
    }

    public boolean hasPlanConfirmation(String ticketId) {
        return !listPlanConfirmations(ticketId).isEmpty();
    }

    public boolean hasAnyReport(String ticketId) {
        return attachmentRepository.countByTicketIdAndTypes(ticketId,
                List.of(AttachmentType.CONFIRM, AttachmentType.PLAN_CONFIRM)) > 0;
    }

    public TicketAttachment getById(String id) {
        return attachmentRepository.findByIdWithTicket(id)
                .orElseThrow(() -> new IllegalArgumentException("附件不存在"));
    }

    @Transactional
    public void upload(Ticket ticket, MultipartFile file, AttachmentType type, String createBy) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        validateFile(file, type);
        String relativePath = fileStorageService.store(ticket.getId(), file);
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String storedName = relativePath.substring(relativePath.lastIndexOf('/') + 1);

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticket);
        attachment.setAttachmentType(type);
        attachment.setOriginalName(originalName);
        attachment.setStoredName(storedName);
        attachment.setRelativePath(relativePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setCreateBy(createBy);
        attachment.setConfirmed(false);
        attachmentRepository.save(attachment);
    }

    @Transactional
    public void uploadBatch(Ticket ticket, List<MultipartFile> files, AttachmentType type, String createBy)
            throws IOException {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            upload(ticket, file, type, createBy);
        }
    }

    @Transactional
    public void confirmReport(String attachmentId, String confirmedBy) {
        TicketAttachment attachment = getById(attachmentId);
        if (!attachment.isReportType()) {
            throw new IllegalArgumentException("仅方案确认报告或完成确认报告可执行确认存档");
        }
        if (attachment.isConfirmed()) {
            return;
        }
        attachment.setConfirmed(true);
        attachment.setConfirmedBy(confirmedBy);
        attachment.setConfirmedTime(java.time.LocalDateTime.now());
        attachmentRepository.save(attachment);
    }

    @Transactional
    public void delete(String attachmentId) throws IOException {
        TicketAttachment attachment = getById(attachmentId);
        if (attachment.isReportType() && attachment.isConfirmed()) {
            throw new IllegalArgumentException("该报告已确认存档，暂不支持删除");
        }
        fileStorageService.delete(attachment.getRelativePath());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteByTicketId(String ticketId) throws IOException {
        List<TicketAttachment> attachments = attachmentRepository.findByTicketIdOrderByCreateTimeDesc(ticketId);
        for (TicketAttachment attachment : attachments) {
            fileStorageService.delete(attachment.getRelativePath());
        }
        attachmentRepository.deleteByTicketId(ticketId);
        fileStorageService.deleteTicketDir(ticketId);
    }

    private void validateFile(MultipartFile file, AttachmentType type) {
        long maxSize = type == AttachmentType.IMAGE
                ? uploadProperties.getMaxImageSize()
                : uploadProperties.getMaxFileSize();
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(type == AttachmentType.IMAGE
                    ? "图片大小不能超过 10MB"
                    : "附件大小不能超过 50MB");
        }
        if (type == AttachmentType.IMAGE && !isImage(file)) {
            throw new IllegalArgumentException("仅支持上传图片文件（jpg、png、gif、webp 等）");
        }
        if ((type == AttachmentType.CONFIRM || type == AttachmentType.PLAN_CONFIRM) && !isConfirmationFile(file)) {
            throw new IllegalArgumentException(
                    (type == AttachmentType.PLAN_CONFIRM ? "方案确认报告" : "确认报告")
                            + "仅支持 PDF 或图片（jpg、png、gif、webp 等）");
        }
    }

    private boolean isConfirmationFile(MultipartFile file) {
        if (isImage(file)) {
            return true;
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        return lower.endsWith(".pdf")
                || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("pdf"));
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        return filename.replace("\\", "_").replace("/", "_");
    }
}
