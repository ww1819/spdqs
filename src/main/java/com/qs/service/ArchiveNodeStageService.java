package com.qs.service;

import com.qs.dto.ArchiveNodeStageDto;
import com.qs.dto.ArchiveNodeStageRequest;
import com.qs.entity.ArchiveNode;
import com.qs.entity.ArchiveNodeStageDef;
import com.qs.repository.ArchiveNodeRepository;
import com.qs.repository.ArchiveNodeStageDefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ArchiveNodeStageService {

    private static final String[] DEFAULT_STAGES = {
            "商务阶段", "调研阶段", "上线阶段", "质保阶段", "维保阶段"
    };
    private static final String[] DEFAULT_COLORS = {
            "business", "research", "launch", "warranty", "maint"
    };

    private final ArchiveNodeStageDefRepository stageRepository;
    private final ArchiveNodeRepository archiveNodeRepository;

    public ArchiveNodeStageService(ArchiveNodeStageDefRepository stageRepository,
                                   ArchiveNodeRepository archiveNodeRepository) {
        this.stageRepository = stageRepository;
        this.archiveNodeRepository = archiveNodeRepository;
    }

    public List<ArchiveNodeStageDto> listActive() {
        ensureDefaults();
        return stageRepository.findByDeletedFalseOrderBySortOrderAscCreateTimeAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<ArchiveNodeStageDto> create(ArchiveNodeStageRequest request, String createBy) {
        String name = requireName(request);
        if (stageRepository.findByNameAndDeletedFalse(name).isPresent()) {
            throw new IllegalArgumentException("阶段名称已存在：" + name);
        }
        ArchiveNodeStageDef stage = new ArchiveNodeStageDef();
        stage.setName(name);
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : nextSortOrder();
        if (sortOrder < 0) {
            throw new IllegalArgumentException("序号不能小于 0");
        }
        stage.setSortOrder(sortOrder);
        stage.setColorKey(resolveColorKey(request.getColorKey(), stage.getSortOrder()));
        stage.setCreateBy(createBy);
        stageRepository.save(stage);
        return listActive();
    }

    @Transactional
    public List<ArchiveNodeStageDto> update(String id, ArchiveNodeStageRequest request) {
        ArchiveNodeStageDef stage = requireActive(id);
        String oldName = stage.getName();
        String newName = requireName(request);

        if (!Objects.equals(oldName, newName)) {
            stageRepository.findByNameAndDeletedFalse(newName).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("阶段名称已存在：" + newName);
                }
            });
            List<ArchiveNode> nodes = archiveNodeRepository.findByStage(oldName);
            for (ArchiveNode node : nodes) {
                node.setStage(newName);
                if (oldName.equals(node.getTitle())) {
                    node.setTitle(newName);
                }
                archiveNodeRepository.save(node);
            }
            stage.setName(newName);
        }
        if (request.getSortOrder() != null) {
            if (request.getSortOrder() < 0) {
                throw new IllegalArgumentException("序号不能小于 0");
            }
            stage.setSortOrder(request.getSortOrder());
        }
        if (request.getColorKey() != null && !request.getColorKey().isBlank()) {
            stage.setColorKey(request.getColorKey().trim());
        }
        stageRepository.save(stage);
        return listActive();
    }

    @Transactional
    public List<ArchiveNodeStageDto> delete(String id) {
        ArchiveNodeStageDef stage = requireActive(id);
        long used = archiveNodeRepository.countByStage(stage.getName());
        if (used > 0) {
            throw new IllegalArgumentException("该阶段已被 " + used + " 个节点使用，请先修改相关节点后再删除");
        }
        stage.setDeleted(true);
        stageRepository.save(stage);
        return listActive();
    }

    @Transactional
    public List<ArchiveNodeStageDto> reorder(List<String> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("排序列表不能为空");
        }
        List<ArchiveNodeStageDef> active = stageRepository.findByDeletedFalseOrderBySortOrderAscCreateTimeAsc();
        if (orderedIds.size() != active.size()) {
            throw new IllegalArgumentException("排序列表与当前阶段数量不一致，请刷新后重试");
        }
        java.util.Map<String, ArchiveNodeStageDef> map = new java.util.HashMap<>();
        for (ArchiveNodeStageDef stage : active) {
            map.put(stage.getId(), stage);
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            String id = orderedIds.get(i);
            ArchiveNodeStageDef stage = map.get(id);
            if (stage == null) {
                throw new IllegalArgumentException("存在无效阶段，请刷新后重试");
            }
            stage.setSortOrder(i);
            stageRepository.save(stage);
        }
        return listActive();
    }

    @Transactional
    public void ensureDefaults() {
        if (stageRepository.countByDeletedFalse() > 0) {
            return;
        }
        for (int i = 0; i < DEFAULT_STAGES.length; i++) {
            ArchiveNodeStageDef stage = new ArchiveNodeStageDef();
            stage.setName(DEFAULT_STAGES[i]);
            stage.setSortOrder(i);
            stage.setColorKey(DEFAULT_COLORS[i]);
            stage.setCreateBy("system");
            stageRepository.save(stage);
        }
    }

    private ArchiveNodeStageDef requireActive(String id) {
        ArchiveNodeStageDef stage = stageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("阶段不存在"));
        if (stage.isDeleted()) {
            throw new IllegalArgumentException("阶段已删除");
        }
        return stage;
    }

    private String requireName(ArchiveNodeStageRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("阶段名称不能为空");
        }
        return request.getName().trim();
    }

    private int nextSortOrder() {
        return stageRepository.findByDeletedFalseOrderBySortOrderAscCreateTimeAsc().stream()
                .mapToInt(ArchiveNodeStageDef::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private String resolveColorKey(String colorKey, int sortOrder) {
        if (colorKey != null && !colorKey.isBlank()) {
            return colorKey.trim();
        }
        String[] keys = {"business", "research", "launch", "warranty", "maint", "other"};
        return keys[Math.floorMod(sortOrder, keys.length)];
    }

    private ArchiveNodeStageDto toDto(ArchiveNodeStageDef stage) {
        return new ArchiveNodeStageDto(stage.getId(), stage.getName(), stage.getSortOrder(), stage.getColorKey());
    }
}
