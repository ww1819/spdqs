package com.qs.service;

import com.qs.entity.DeliveryNode;
import com.qs.entity.DeliveryNodeChangeLog;
import com.qs.repository.DeliveryNodeChangeLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DeliveryNodeChangeLogService {

    private final DeliveryNodeChangeLogRepository changeLogRepository;

    public DeliveryNodeChangeLogService(DeliveryNodeChangeLogRepository changeLogRepository) {
        this.changeLogRepository = changeLogRepository;
    }

    public List<DeliveryNodeChangeLog> listByNodeId(String nodeId) {
        return changeLogRepository.findByNodeIdOrderByChangeTimeDesc(nodeId);
    }

    @Transactional
    public void recordUpdates(DeliveryNode before, DeliveryNode after, String changeBy) {
        if (before == null || after == null || before.getId() == null) {
            return;
        }
        List<DeliveryNodeChangeLog> logs = new ArrayList<>();
        addIfChanged(logs, before, "stage", "阶段", before.getStage(), after.getStage(), changeBy);
        addIfChanged(logs, before, "title", "标题", before.getTitle(), after.getTitle(), changeBy);
        addIfChanged(logs, before, "nodeType", "类型", before.getNodeType(), after.getNodeType(), changeBy);
        addIfChanged(logs, before, "startDate", "开始日期",
                formatDate(before.getStartDate()), formatDate(after.getStartDate()), changeBy);
        addIfChanged(logs, before, "endDate", "结束日期",
                formatEndDate(before), formatEndDate(after), changeBy);
        addIfChanged(logs, before, "remark", "备注", before.getRemark(), after.getRemark(), changeBy);
        if (!logs.isEmpty()) {
            changeLogRepository.saveAll(logs);
        }
    }

    @Transactional
    public void deleteByNodeId(String nodeId) {
        changeLogRepository.deleteByNodeId(nodeId);
    }

    private void addIfChanged(List<DeliveryNodeChangeLog> logs, DeliveryNode node,
                              String fieldName, String fieldLabel,
                              String oldValue, String newValue, String changeBy) {
        String oldNorm = normalize(oldValue);
        String newNorm = normalize(newValue);
        if (Objects.equals(oldNorm, newNorm)) {
            return;
        }
        DeliveryNodeChangeLog log = new DeliveryNodeChangeLog();
        log.setNodeId(node.getId());
        log.setDeliveryId(node.getDeliveryId());
        log.setFieldName(fieldName);
        log.setFieldLabel(fieldLabel);
        log.setOldValue(oldNorm);
        log.setNewValue(newNorm);
        log.setChangeBy(changeBy);
        logs.add(log);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private String formatEndDate(DeliveryNode node) {
        if (!node.isRange()) {
            return null;
        }
        return node.getEndDate() == null ? "至今" : node.getEndDate().toString();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
