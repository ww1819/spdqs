package com.qs.service;

import com.qs.dto.ArchiveBriefDto;
import com.qs.dto.ArchiveOptionDto;
import com.qs.dto.ArchiveView;
import com.qs.entity.Archive;
import com.qs.enums.ArchiveStatus;
import com.qs.repository.ArchiveRepository;
import com.qs.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    private final TicketRepository ticketRepository;
    private final ArchiveAttachmentService attachmentService;

    public ArchiveService(ArchiveRepository archiveRepository, TicketRepository ticketRepository,
                          ArchiveAttachmentService attachmentService) {
        this.archiveRepository = archiveRepository;
        this.ticketRepository = ticketRepository;
        this.attachmentService = attachmentService;
    }

    public List<ArchiveView> listAll(String statusFilter, String keyword) {
        String kw = normalize(keyword);
        return archiveRepository.findAllByOrderByCreateTimeDesc().stream()
                .map(this::toView)
                .filter(view -> statusFilter == null || statusFilter.isBlank()
                        || view.getStatusLabel().equals(statusFilter))
                .filter(view -> kw == null || matchesKeyword(view, kw))
                .toList();
    }

    public List<ArchiveView> listAll(String statusFilter) {
        return listAll(statusFilter, null);
    }

    public List<ArchiveOptionDto> listOptions() {
        return archiveRepository.findAllByOrderByCreateTimeDesc().stream()
                .map(archive -> {
                    ArchiveStatus status = calculateStatus(archive);
                    long days = calculateDaysToExpire(archive.getMaintExpireDate());
                    return new ArchiveOptionDto(
                            archive.getId(),
                            archive.getProjectName(),
                            archive.getMaintExpireDate(),
                            status,
                            days
                    );
                })
                .toList();
    }

    public ArchiveView getView(String id) {
        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("档案不存在"));
        return toView(archive);
    }

    public Archive getById(String id) {
        return archiveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("档案不存在"));
    }

    @Transactional
    public Archive save(Archive archive) {
        return archiveRepository.save(archive);
    }

    @Transactional
    public void delete(String id) {
        try {
            attachmentService.deleteByArchiveId(id);
        } catch (IOException ex) {
            throw new IllegalStateException("删除档案附件失败", ex);
        }
        ticketRepository.findAllWithArchive().stream()
                .filter(t -> t.getArchive().getId().equals(id))
                .forEach(t -> ticketRepository.deleteById(t.getId()));
        archiveRepository.deleteById(id);
    }

    public ArchiveStatus calculateStatus(Archive archive) {
        LocalDate now = LocalDate.now();
        if (archive.getLaunchDate() != null && now.isBefore(archive.getLaunchDate())) {
            return ArchiveStatus.LAUNCHING;
        }
        if (archive.getMaintExpireDate() == null) {
            return ArchiveStatus.MAINTAINING;
        }
        if (now.isAfter(archive.getMaintExpireDate())) {
            return ArchiveStatus.EXPIRED;
        }
        long days = ChronoUnit.DAYS.between(now, archive.getMaintExpireDate());
        if (days <= 90) {
            return ArchiveStatus.EXPIRING_SOON;
        }
        return ArchiveStatus.MAINTAINING;
    }

    public long calculateDaysToExpire(LocalDate maintExpireDate) {
        if (maintExpireDate == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), maintExpireDate);
    }

    public ArchiveBriefDto getBrief(String id) {
        Archive archive = getById(id);
        return new ArchiveBriefDto(
                archive.getId(),
                archive.getProjectName(),
                archive.getContactInfo(),
                archive.getRemoteMethod(),
                archive.getSpecialProcess(),
                archive.getLaunchPlan(),
                archive.getOnsiteManager(),
                archive.getImplManager()
        );
    }

    private boolean matchesKeyword(ArchiveView view, String kw) {
        return contains(view.getProjectName(), kw)
                || contains(view.getProjectType(), kw)
                || contains(view.getOnsiteManager(), kw)
                || contains(view.getImplManager(), kw);
    }

    private boolean contains(String text, String kw) {
        return text != null && text.toLowerCase(java.util.Locale.ROOT).contains(kw);
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private ArchiveView toView(Archive archive) {
        ArchiveStatus status = calculateStatus(archive);
        long days = calculateDaysToExpire(archive.getMaintExpireDate());
        return new ArchiveView(archive, status, days);
    }
}
