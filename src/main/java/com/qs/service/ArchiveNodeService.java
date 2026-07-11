package com.qs.service;

import com.qs.dto.ArchiveNodeDto;
import com.qs.dto.ArchiveNodeRequest;
import com.qs.entity.ArchiveNode;
import com.qs.enums.ArchiveNodeType;
import com.qs.repository.ArchiveNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ArchiveNodeService {

    private final ArchiveNodeRepository archiveNodeRepository;
    private final ArchiveService archiveService;
    private final ArchiveNodeStageService stageService;

    public ArchiveNodeService(ArchiveNodeRepository archiveNodeRepository, ArchiveService archiveService,
                              ArchiveNodeStageService stageService) {
        this.archiveNodeRepository = archiveNodeRepository;
        this.archiveService = archiveService;
        this.stageService = stageService;
    }

    public List<ArchiveNodeDto> listByArchiveId(String archiveId) {
        archiveService.getById(archiveId);
        return archiveNodeRepository.findByArchiveIdOrderByStartDateAscSortOrderAsc(archiveId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<ArchiveNodeDto> create(String archiveId, ArchiveNodeRequest request, String createBy) {
        archiveService.getById(archiveId);
        ArchiveNode node = new ArchiveNode();
        node.setArchiveId(archiveId);
        applyRequest(node, request);
        node.setSortOrder((int) archiveNodeRepository.countByArchiveId(archiveId));
        node.setCreateBy(createBy);
        archiveNodeRepository.save(node);
        return listByArchiveId(archiveId);
    }

    @Transactional
    public List<ArchiveNodeDto> update(String archiveId, String nodeId, ArchiveNodeRequest request) {
        ArchiveNode node = getOwnedNode(archiveId, nodeId);
        applyRequest(node, request);
        archiveNodeRepository.save(node);
        return listByArchiveId(archiveId);
    }

    @Transactional
    public List<ArchiveNodeDto> delete(String archiveId, String nodeId) {
        ArchiveNode node = getOwnedNode(archiveId, nodeId);
        archiveNodeRepository.delete(node);
        return listByArchiveId(archiveId);
    }

    @Transactional
    public void deleteByArchiveId(String archiveId) {
        archiveNodeRepository.deleteByArchiveId(archiveId);
    }

    private ArchiveNode getOwnedNode(String archiveId, String nodeId) {
        archiveService.getById(archiveId);
        ArchiveNode node = archiveNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        if (!archiveId.equals(node.getArchiveId())) {
            throw new IllegalArgumentException("节点不属于该档案");
        }
        return node;
    }

    private void applyRequest(ArchiveNode node, ArchiveNodeRequest request) {
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
        ArchiveNodeType type = ArchiveNodeType.fromLabel(request.getNodeType());
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
        if (type == ArchiveNodeType.RANGE) {
            if (request.getEndDate() == null) {
                throw new IllegalArgumentException("时间段节点须填写结束日期");
            }
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("结束日期不能早于开始日期");
            }
            node.setEndDate(request.getEndDate());
        } else {
            node.setEndDate(null);
        }
        node.setRemark(request.getRemark() != null ? request.getRemark().trim() : null);
    }

    private ArchiveNodeDto toDto(ArchiveNode node) {
        ArchiveNodeDto dto = new ArchiveNodeDto();
        dto.setId(node.getId());
        dto.setArchiveId(node.getArchiveId());
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
        return dto;
    }

    private String formatDateLabel(ArchiveNode node) {
        if (node.isRange() && node.getEndDate() != null) {
            return node.getStartDate() + " ~ " + node.getEndDate();
        }
        return String.valueOf(node.getStartDate());
    }

    private String resolveStatus(ArchiveNode node) {
        LocalDate today = LocalDate.now();
        if (node.isRange() && node.getEndDate() != null) {
            if (today.isBefore(node.getStartDate())) {
                return "未开始";
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
