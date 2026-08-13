package com.qs.service;

import com.qs.dto.DeliveryNodeDto;
import com.qs.dto.DeliveryNodeRequest;
import com.qs.entity.DeliveryNode;
import com.qs.enums.DeliveryNodeType;
import com.qs.repository.DeliveryNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeliveryNodeService {

    private final DeliveryNodeRepository deliveryNodeRepository;
    private final DeliveryService deliveryService;
    private final ArchiveNodeStageService stageService;
    private final DeliveryNodeChangeLogService changeLogService;
    private final DeliveryNodeAttachmentService attachmentService;
    private final DeliveryNodeMemoService memoService;

    public DeliveryNodeService(DeliveryNodeRepository deliveryNodeRepository, DeliveryService deliveryService,
                               ArchiveNodeStageService stageService,
                               DeliveryNodeChangeLogService changeLogService,
                               DeliveryNodeAttachmentService attachmentService,
                               DeliveryNodeMemoService memoService) {
        this.deliveryNodeRepository = deliveryNodeRepository;
        this.deliveryService = deliveryService;
        this.stageService = stageService;
        this.changeLogService = changeLogService;
        this.attachmentService = attachmentService;
        this.memoService = memoService;
    }

    public List<DeliveryNodeDto> listByDeliveryId(String deliveryId) {
        deliveryService.getById(deliveryId);
        return deliveryNodeRepository.findByDeliveryIdOrderByStartDateAscSortOrderAsc(deliveryId).stream()
                .map(this::toDto)
                .toList();
    }

    public DeliveryNode getOwnedNode(String deliveryId, String nodeId) {
        deliveryService.getById(deliveryId);
        DeliveryNode node = deliveryNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        if (!deliveryId.equals(node.getDeliveryId())) {
            throw new IllegalArgumentException("节点不属于该产品交付");
        }
        return node;
    }

    @Transactional
    public List<DeliveryNodeDto> create(String deliveryId, DeliveryNodeRequest request, String createBy) {
        deliveryService.getById(deliveryId);
        DeliveryNode node = new DeliveryNode();
        node.setDeliveryId(deliveryId);
        applyRequest(node, request);
        node.setSortOrder((int) deliveryNodeRepository.countByDeliveryId(deliveryId));
        node.setCreateBy(createBy);
        deliveryNodeRepository.save(node);
        return listByDeliveryId(deliveryId);
    }

    @Transactional
    public List<DeliveryNodeDto> update(String deliveryId, String nodeId, DeliveryNodeRequest request,
                                        String changeBy) {
        DeliveryNode node = getOwnedNode(deliveryId, nodeId);
        DeliveryNode before = snapshot(node);
        applyRequest(node, request);
        changeLogService.recordUpdates(before, node, changeBy);
        deliveryNodeRepository.save(node);
        return listByDeliveryId(deliveryId);
    }

    @Transactional
    public List<DeliveryNodeDto> confirm(String deliveryId, String nodeId, String confirmedBy) {
        DeliveryNode node = getOwnedNode(deliveryId, nodeId);
        if (!node.isConfirmed()) {
            node.setConfirmed(true);
            node.setConfirmedBy(confirmedBy);
            node.setConfirmedTime(LocalDateTime.now());
            deliveryNodeRepository.save(node);
        }
        return listByDeliveryId(deliveryId);
    }

    @Transactional
    public List<DeliveryNodeDto> delete(String deliveryId, String nodeId) {
        DeliveryNode node = getOwnedNode(deliveryId, nodeId);
        if (node.isConfirmed()) {
            throw new IllegalArgumentException("节点已确认，不可删除");
        }
        try {
            attachmentService.deleteByNodeId(nodeId);
        } catch (IOException ex) {
            throw new IllegalStateException("删除节点附件失败", ex);
        }
        memoService.deleteByNodeId(nodeId);
        changeLogService.deleteByNodeId(nodeId);
        deliveryNodeRepository.delete(node);
        return listByDeliveryId(deliveryId);
    }

    @Transactional
    public void deleteByDeliveryId(String deliveryId) {
        deliveryNodeRepository.findByDeliveryIdOrderByStartDateAscSortOrderAsc(deliveryId).forEach(node -> {
            try {
                attachmentService.deleteByNodeId(node.getId());
            } catch (IOException ignored) {
                // best-effort cleanup
            }
            memoService.deleteByNodeId(node.getId());
            changeLogService.deleteByNodeId(node.getId());
        });
        deliveryNodeRepository.deleteByDeliveryId(deliveryId);
    }

    private DeliveryNode snapshot(DeliveryNode source) {
        DeliveryNode copy = new DeliveryNode();
        copy.setId(source.getId());
        copy.setDeliveryId(source.getDeliveryId());
        copy.setStage(source.getStage());
        copy.setTitle(source.getTitle());
        copy.setNodeType(source.getNodeType());
        copy.setStartDate(source.getStartDate());
        copy.setEndDate(source.getEndDate());
        copy.setRemark(source.getRemark());
        return copy;
    }

    private void applyRequest(DeliveryNode node, DeliveryNodeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getStage() == null || request.getStage().isBlank()) {
            throw new IllegalArgumentException("阶段不能为空");
        }
        String stageName = request.getStage().trim();
        boolean stageExists = stageService.listActive().stream()
                .anyMatch(s -> stageName.equals(s.getName()));
        if (!stageExists) {
            throw new IllegalArgumentException("阶段不存在，请先在阶段维护中添加：" + stageName);
        }
        DeliveryNodeType type = DeliveryNodeType.fromLabel(request.getNodeType());
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = stageName;
        }
        node.setStage(stageName);
        node.setTitle(title.trim());
        node.setNodeType(type.getLabel());
        node.setStartDate(request.getStartDate());
        if (type == DeliveryNodeType.RANGE) {
            if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("结束日期不能早于开始日期");
            }
            node.setEndDate(request.getEndDate());
        } else {
            node.setEndDate(null);
        }
        node.setRemark(request.getRemark() != null ? request.getRemark().trim() : null);
    }

    private DeliveryNodeDto toDto(DeliveryNode node) {
        DeliveryNodeDto dto = new DeliveryNodeDto();
        dto.setId(node.getId());
        dto.setDeliveryId(node.getDeliveryId());
        dto.setStage(node.getStage());
        dto.setTitle(node.getTitle());
        dto.setNodeType(node.getNodeType());
        dto.setStartDate(node.getStartDate());
        dto.setEndDate(node.getEndDate());
        dto.setRemark(node.getRemark());
        dto.setSortOrder(node.getSortOrder());
        dto.setRange(node.isRange());
        dto.setDateLabel(formatDateLabel(node));
        dto.setStatusLabel(resolveStatus(node));
        dto.setConfirmed(node.isConfirmed());
        dto.setConfirmedBy(node.getConfirmedBy());
        dto.setConfirmedTime(node.getConfirmedTime());
        dto.setAttachmentCount((int) attachmentService.countByNodeId(node.getId()));
        dto.setMemoCount((int) memoService.countByNodeId(node.getId()));
        return dto;
    }

    private String formatDateLabel(DeliveryNode node) {
        if (node.isRange()) {
            String end = node.getEndDate() == null ? "至今" : String.valueOf(node.getEndDate());
            return node.getStartDate() + " ~ " + end;
        }
        return String.valueOf(node.getStartDate());
    }

    private String resolveStatus(DeliveryNode node) {
        LocalDate today = LocalDate.now();
        if (node.isRange()) {
            if (today.isBefore(node.getStartDate())) {
                return "未开始";
            }
            if (node.getEndDate() == null) {
                return "进行中";
            }
            if (today.isAfter(node.getEndDate())) {
                return "已结束";
            }
            return "进行中";
        }
        if (today.isBefore(node.getStartDate())) {
            return "未到达";
        }
        if (today.isEqual(node.getStartDate())) {
            return "今天";
        }
        return "已过";
    }
}
