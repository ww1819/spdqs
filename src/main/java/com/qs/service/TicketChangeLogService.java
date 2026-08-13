package com.qs.service;

import com.qs.entity.Ticket;
import com.qs.entity.TicketChangeLog;
import com.qs.repository.TicketChangeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TicketChangeLogService {

    private final TicketChangeLogRepository changeLogRepository;

    public TicketChangeLogService(TicketChangeLogRepository changeLogRepository) {
        this.changeLogRepository = changeLogRepository;
    }

    public List<TicketChangeLog> listByTicketId(String ticketId) {
        return changeLogRepository.findByTicketIdOrderByChangeTimeDesc(ticketId);
    }

    @Transactional
    public void recordUpdates(Ticket before, Ticket after, String changeBy) {
        if (before == null || after == null || before.getId() == null) {
            return;
        }
        List<TicketChangeLog> logs = new ArrayList<>();
        addIfChanged(logs, before.getId(), "orderType", "工单类型", before.getOrderType(), after.getOrderType(), changeBy);
        addIfChanged(logs, before.getId(), "status", "状态", before.getStatus(), after.getStatus(), changeBy);
        addIfChanged(logs, before.getId(), "submitter", "提交人", before.getSubmitter(), after.getSubmitter(), changeBy);
        addIfChanged(logs, before.getId(), "handler", "处理人", before.getHandler(), after.getHandler(), changeBy);
        addIfChanged(logs, before.getId(), "content", "工单内容", before.getContent(), after.getContent(), changeBy);
        addIfChanged(logs, before.getId(), "contactInfo", "联系方式", before.getContactInfo(), after.getContactInfo(), changeBy);
        addIfChanged(logs, before.getId(), "attentionNote", "注意事项", before.getAttentionNote(), after.getAttentionNote(), changeBy);
        addIfChanged(logs, before.getId(), "expectedCompleteDate", "预计完成",
                formatDate(before.getExpectedCompleteDate()), formatDate(after.getExpectedCompleteDate()), changeBy);
        addIfChanged(logs, before.getId(), "targetCompleteDate", "目标完成",
                formatDate(before.getTargetCompleteDate()), formatDate(after.getTargetCompleteDate()), changeBy);
        String oldProject = before.getArchive() != null ? before.getArchive().getProjectName() : null;
        String newProject = after.getArchive() != null ? after.getArchive().getProjectName() : null;
        String oldArchiveId = before.getArchive() != null ? before.getArchive().getId() : null;
        String newArchiveId = after.getArchive() != null ? after.getArchive().getId() : null;
        if (!Objects.equals(oldArchiveId, newArchiveId)) {
            addIfChanged(logs, before.getId(), "archive", "项目", oldProject, newProject, changeBy);
        }
        if (!logs.isEmpty()) {
            changeLogRepository.saveAll(logs);
        }
    }

    @Transactional
    public void deleteByTicketId(String ticketId) {
        changeLogRepository.deleteByTicketId(ticketId);
    }

    private void addIfChanged(List<TicketChangeLog> logs, String ticketId, String fieldName, String fieldLabel,
                              String oldVal, String newVal, String changeBy) {
        String o = normalize(oldVal);
        String n = normalize(newVal);
        if (Objects.equals(o, n)) {
            return;
        }
        TicketChangeLog log = new TicketChangeLog();
        log.setTicketId(ticketId);
        log.setFieldName(fieldName);
        log.setFieldLabel(fieldLabel);
        log.setOldValue(o);
        log.setNewValue(n);
        log.setChangeBy(changeBy);
        logs.add(log);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }
}
