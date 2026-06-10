package com.qs.service;

import com.qs.config.UploadProperties;
import com.qs.entity.Archive;
import com.qs.entity.ArchiveAttachment;
import com.qs.repository.ArchiveAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ArchiveAttachmentService {

    private final ArchiveAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UploadProperties uploadProperties;

    public ArchiveAttachmentService(ArchiveAttachmentRepository attachmentRepository,
                                    FileStorageService fileStorageService,
                                    UploadProperties uploadProperties) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.uploadProperties = uploadProperties;
    }

    public List<ArchiveAttachment> listByArchiveId(String archiveId) {
        return attachmentRepository.findByArchiveIdOrderByCreateTimeDesc(archiveId);
    }

    public ArchiveAttachment getById(String id) {
        return attachmentRepository.findByIdWithArchive(id)
                .orElseThrow(() -> new IllegalArgumentException("附件不存在"));
    }

    @Transactional
    public void upload(Archive archive, MultipartFile file, String createBy) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        validateFile(file);
        String relativePath = fileStorageService.storeArchive(archive.getId(), file);
        String originalName = sanitizeFilename(file.getOriginalFilename());
        String storedName = relativePath.substring(relativePath.lastIndexOf('/') + 1);

        ArchiveAttachment attachment = new ArchiveAttachment();
        attachment.setArchive(archive);
        attachment.setOriginalName(originalName);
        attachment.setStoredName(storedName);
        attachment.setRelativePath(relativePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setCreateBy(createBy);
        attachmentRepository.save(attachment);
    }

    @Transactional
    public void uploadBatch(Archive archive, List<MultipartFile> files, String createBy) throws IOException {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            upload(archive, file, createBy);
        }
    }

    @Transactional
    public void delete(String attachmentId) throws IOException {
        ArchiveAttachment attachment = getById(attachmentId);
        fileStorageService.delete(attachment.getRelativePath());
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteByArchiveId(String archiveId) throws IOException {
        List<ArchiveAttachment> attachments = attachmentRepository.findByArchiveIdOrderByCreateTimeDesc(archiveId);
        for (ArchiveAttachment attachment : attachments) {
            fileStorageService.delete(attachment.getRelativePath());
        }
        attachmentRepository.deleteByArchiveId(archiveId);
        fileStorageService.deleteArchiveDir(archiveId);
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
