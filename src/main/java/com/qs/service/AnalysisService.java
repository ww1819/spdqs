package com.qs.service;

import com.qs.dto.FlowNodeChangeLogDto;
import com.qs.dto.FlowNodeTreeDto;
import com.qs.entity.AnalysisProject;
import com.qs.entity.FlowNode;
import com.qs.entity.FlowNodeChangeLog;
import com.qs.repository.AnalysisProjectRepository;
import com.qs.repository.FlowNodeChangeLogRepository;
import com.qs.repository.FlowNodeRepository;
import com.qs.util.PinyinCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AnalysisService {

    private final AnalysisProjectRepository projectRepository;
    private final FlowNodeRepository flowNodeRepository;
    private final FlowNodeChangeLogRepository changeLogRepository;
    private final AnalysisTextExporter textExporter;
    private final AnalysisMergeExcelExporter mergeExcelExporter;

    public AnalysisService(AnalysisProjectRepository projectRepository,
                           FlowNodeRepository flowNodeRepository,
                           FlowNodeChangeLogRepository changeLogRepository,
                           AnalysisTextExporter textExporter,
                           AnalysisMergeExcelExporter mergeExcelExporter) {
        this.projectRepository = projectRepository;
        this.flowNodeRepository = flowNodeRepository;
        this.changeLogRepository = changeLogRepository;
        this.textExporter = textExporter;
        this.mergeExcelExporter = mergeExcelExporter;
    }

    public List<AnalysisProject> listProjects() {
        return projectRepository.findAllByOrderByCreateTimeDesc();
    }

    public AnalysisProject getProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
    }

    @Transactional
    public AnalysisProject createProject(String name, String description, String rootTitle, String createBy) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        String root = (rootTitle == null || rootTitle.isBlank()) ? "登录" : rootTitle.trim();

        AnalysisProject project = new AnalysisProject();
        project.setName(name.trim());
        project.setDescription(description != null ? description.trim() : null);
        project.setCreateBy(createBy);
        project = projectRepository.save(project);

        FlowNode rootNode = new FlowNode();
        rootNode.setProjectId(project.getId());
        rootNode.setParentId(null);
        rootNode.setTitle(root);
        rootNode.setPinyinCode(PinyinCodeUtil.toJianpin(root));
        rootNode.setSortOrder(0);
        rootNode.setCreateBy(createBy);
        flowNodeRepository.save(rootNode);

        return project;
    }

    @Transactional
    public void deleteProject(String id) {
        getProject(id);
        flowNodeRepository.deleteByProjectId(id);
        projectRepository.deleteById(id);
    }

    public FlowNodeTreeDto getProjectTree(String projectId) {
        getProject(projectId);
        List<FlowNode> nodes = flowNodeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAsc(projectId);
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("项目缺少根流程节点");
        }
        return buildTree(nodes);
    }

    @Transactional
    public FlowNodeTreeDto createChildNode(String parentId, String title, String createBy) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("流程标题不能为空");
        }
        FlowNode parent = requireActiveNode(parentId);

        List<FlowNode> siblings = flowNodeRepository.findByParentIdAndDeletedFalseOrderBySortOrderAsc(parentId);
        int nextOrder = siblings.stream()
                .mapToInt(FlowNode::getSortOrder)
                .max()
                .orElse(-1) + 1;

        FlowNode node = new FlowNode();
        node.setProjectId(parent.getProjectId());
        node.setParentId(parentId);
        node.setTitle(title.trim());
        node.setPinyinCode(PinyinCodeUtil.toJianpin(title.trim()));
        node.setSortOrder(nextOrder);
        node.setCreateBy(createBy);
        flowNodeRepository.save(node);

        return getProjectTree(parent.getProjectId());
    }

    @Transactional
    public FlowNodeTreeDto updateNode(String nodeId, String title, String description, String changeBy) {
        FlowNode node = requireActiveNode(nodeId);

        String oldTitle = node.getTitle();
        String oldDescription = node.getDescription();
        boolean titleChanged = false;
        boolean descriptionChanged = false;

        if (title != null && !title.isBlank()) {
            String newTitle = title.trim();
            if (!Objects.equals(oldTitle, newTitle)) {
                node.setTitle(newTitle);
                node.setPinyinCode(PinyinCodeUtil.toJianpin(newTitle));
                titleChanged = true;
            }
        }
        if (description != null) {
            String newDescription = description.trim();
            if (!Objects.equals(normalizeNullable(oldDescription), normalizeNullable(newDescription))) {
                node.setDescription(newDescription);
                descriptionChanged = true;
            }
        }

        if (titleChanged || descriptionChanged) {
            FlowNodeChangeLog log = new FlowNodeChangeLog();
            log.setNodeId(node.getId());
            log.setProjectId(node.getProjectId());
            log.setOldTitle(oldTitle);
            log.setNewTitle(node.getTitle());
            log.setOldDescription(oldDescription);
            log.setNewDescription(node.getDescription());
            log.setChangeBy(changeBy);
            changeLogRepository.save(log);
            flowNodeRepository.save(node);
        }

        return getProjectTree(node.getProjectId());
    }

    /** 按当前名称全量重建项目下流程拼音简码 */
    @Transactional
    public FlowNodeTreeDto rebuildPinyinCodes(String projectId) {
        getProject(projectId);
        List<FlowNode> nodes = flowNodeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAsc(projectId);
        for (FlowNode node : nodes) {
            node.setPinyinCode(PinyinCodeUtil.toJianpin(node.getTitle()));
        }
        flowNodeRepository.saveAll(nodes);
        return getProjectTree(projectId);
    }

    @Transactional
    public FlowNodeTreeDto deleteNode(String nodeId, String deletedBy) {
        FlowNode node = requireActiveNode(nodeId);
        if (node.getParentId() == null) {
            throw new IllegalArgumentException("根节点不能删除");
        }
        String projectId = node.getProjectId();
        softDeleteSubtree(nodeId, deletedBy);
        return getProjectTree(projectId);
    }

    public String exportProjectText(String projectId) {
        AnalysisProject project = getProject(projectId);
        FlowNodeTreeDto tree = getProjectTree(projectId);
        return textExporter.export(project, tree);
    }

    public byte[] exportProjectMergeExcel(String projectId) {
        AnalysisProject project = getProject(projectId);
        FlowNodeTreeDto tree = getProjectTree(projectId);
        return mergeExcelExporter.export(project, tree);
    }

    /** 全部功能菜单标题（去重排序），供工单列表筛选 */
    public List<String> listDistinctMenuTitles() {
        return flowNodeRepository.findByDeletedFalse().stream()
                .map(FlowNode::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    /**
     * 按分析项目（系统）加载功能菜单标题；未选系统时返回空列表。
     * 多系统时取并集去重排序。
     */
    public List<String> listMenuTitlesByProjectIds(List<String> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        Set<String> titles = new LinkedHashSet<>();
        for (String projectId : projectIds) {
            if (projectId == null || projectId.isBlank()) {
                continue;
            }
            flowNodeRepository.findByProjectIdAndDeletedFalseOrderBySortOrderAsc(projectId.trim())
                    .stream()
                    .map(FlowNode::getTitle)
                    .filter(t -> t != null && !t.isBlank())
                    .forEach(titles::add);
        }
        return titles.stream().sorted(String::compareToIgnoreCase).toList();
    }

    /** 多个菜单标题解析别名（当前名 + 曾用名）并集 */
    public List<String> resolveMenuAliases(List<String> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        Set<String> aliases = new LinkedHashSet<>();
        for (String menu : menus) {
            if (menu == null || menu.isBlank()) {
                continue;
            }
            aliases.addAll(resolveMenuAliases(menu.trim()));
        }
        return aliases.stream().filter(t -> t != null && !t.isBlank()).toList();
    }

    public List<FlowNodeChangeLogDto> listNodeChanges(String nodeId) {
        flowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        return changeLogRepository.findByNodeIdOrderByChangeTimeDesc(nodeId).stream()
                .map(this::toChangeDto)
                .toList();
    }

    /**
     * 解析菜单筛选别名：当前名 + 相关曾用名，便于改名后仍能筛到旧工单内容。
     */
    public List<String> resolveMenuAliases(String menu) {
        if (menu == null || menu.isBlank()) {
            return List.of();
        }
        String target = menu.trim();
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(target);

        for (FlowNode node : flowNodeRepository.findByTitle(target)) {
            collectTitlesFromLogs(aliases, changeLogRepository.findByNodeIdOrderByChangeTimeDesc(node.getId()));
        }
        collectTitlesFromLogs(aliases, changeLogRepository.findByTitleInvolved(target));
        return aliases.stream().filter(t -> t != null && !t.isBlank()).toList();
    }

    private void collectTitlesFromLogs(Set<String> aliases, List<FlowNodeChangeLog> logs) {
        for (FlowNodeChangeLog log : logs) {
            if (log.getOldTitle() != null && !log.getOldTitle().isBlank()) {
                aliases.add(log.getOldTitle());
            }
            if (log.getNewTitle() != null && !log.getNewTitle().isBlank()) {
                aliases.add(log.getNewTitle());
            }
        }
    }

    private FlowNodeChangeLogDto toChangeDto(FlowNodeChangeLog log) {
        FlowNodeChangeLogDto dto = new FlowNodeChangeLogDto();
        dto.setId(log.getId());
        dto.setNodeId(log.getNodeId());
        dto.setOldTitle(log.getOldTitle());
        dto.setNewTitle(log.getNewTitle());
        dto.setOldDescription(log.getOldDescription());
        dto.setNewDescription(log.getNewDescription());
        dto.setChangeBy(log.getChangeBy());
        dto.setChangeTime(log.getChangeTime());
        dto.setTitleChanged(!Objects.equals(normalizeNullable(log.getOldTitle()), normalizeNullable(log.getNewTitle())));
        dto.setDescriptionChanged(!Objects.equals(
                normalizeNullable(log.getOldDescription()),
                normalizeNullable(log.getNewDescription())));
        return dto;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private FlowNode requireActiveNode(String nodeId) {
        FlowNode node = flowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        if (node.isDeleted()) {
            throw new IllegalArgumentException("节点已删除");
        }
        return node;
    }

    private void softDeleteSubtree(String nodeId, String deletedBy) {
        List<FlowNode> children = flowNodeRepository.findByParentIdAndDeletedFalseOrderBySortOrderAsc(nodeId);
        for (FlowNode child : children) {
            softDeleteSubtree(child.getId(), deletedBy);
        }
        FlowNode node = flowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        if (!node.isDeleted()) {
            node.setDeleted(true);
            node.setDeletedBy(deletedBy);
            node.setDeletedTime(java.time.LocalDateTime.now());
            flowNodeRepository.save(node);
        }
    }

    private FlowNodeTreeDto buildTree(List<FlowNode> nodes) {
        Map<String, FlowNodeTreeDto> dtoMap = new HashMap<>();
        Map<String, List<FlowNode>> childrenMap = new HashMap<>();

        for (FlowNode node : nodes) {
            dtoMap.put(node.getId(), new FlowNodeTreeDto(
                    node.getId(),
                    node.getTitle(),
                    node.getPinyinCode(),
                    node.getDescription()
            ));
            if (node.getParentId() != null) {
                childrenMap.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
            }
        }

        FlowNodeTreeDto root = null;
        for (FlowNode node : nodes) {
            if (node.getParentId() == null) {
                root = dtoMap.get(node.getId());
                break;
            }
        }
        if (root == null) {
            throw new IllegalArgumentException("项目缺少根流程节点");
        }

        attachChildren(root, childrenMap, dtoMap);
        return root;
    }

    private void attachChildren(FlowNodeTreeDto parent,
                                Map<String, List<FlowNode>> childrenMap,
                                Map<String, FlowNodeTreeDto> dtoMap) {
        List<FlowNode> children = new ArrayList<>(childrenMap.getOrDefault(parent.getId(), List.of()));
        children.sort(Comparator.comparingInt(FlowNode::getSortOrder));
        for (FlowNode child : children) {
            FlowNodeTreeDto childDto = dtoMap.get(child.getId());
            attachChildren(childDto, childrenMap, dtoMap);
            parent.getChildren().add(childDto);
        }
    }
}
