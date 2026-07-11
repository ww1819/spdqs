package com.qs.service;

import com.qs.dto.FlowNodeTreeDto;
import com.qs.entity.AnalysisProject;
import com.qs.entity.FlowNode;
import com.qs.repository.AnalysisProjectRepository;
import com.qs.repository.FlowNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private final AnalysisProjectRepository projectRepository;
    private final FlowNodeRepository flowNodeRepository;
    private final AnalysisTextExporter textExporter;

    public AnalysisService(AnalysisProjectRepository projectRepository,
                           FlowNodeRepository flowNodeRepository,
                           AnalysisTextExporter textExporter) {
        this.projectRepository = projectRepository;
        this.flowNodeRepository = flowNodeRepository;
        this.textExporter = textExporter;
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
        List<FlowNode> nodes = flowNodeRepository.findByProjectIdOrderBySortOrderAsc(projectId);
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
        FlowNode parent = flowNodeRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("父节点不存在"));

        List<FlowNode> siblings = flowNodeRepository.findByParentIdOrderBySortOrderAsc(parentId);
        int nextOrder = siblings.stream()
                .mapToInt(FlowNode::getSortOrder)
                .max()
                .orElse(-1) + 1;

        FlowNode node = new FlowNode();
        node.setProjectId(parent.getProjectId());
        node.setParentId(parentId);
        node.setTitle(title.trim());
        node.setSortOrder(nextOrder);
        node.setCreateBy(createBy);
        flowNodeRepository.save(node);

        return getProjectTree(parent.getProjectId());
    }

    @Transactional
    public FlowNodeTreeDto updateNode(String nodeId, String title, String description) {
        FlowNode node = flowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        if (title != null && !title.isBlank()) {
            node.setTitle(title.trim());
        }
        if (description != null) {
            node.setDescription(description.trim());
        }
        flowNodeRepository.save(node);
        return getProjectTree(node.getProjectId());
    }

    @Transactional
    public FlowNodeTreeDto deleteNode(String nodeId) {
        FlowNode node = flowNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        if (node.getParentId() == null) {
            throw new IllegalArgumentException("根节点不能删除");
        }
        String projectId = node.getProjectId();
        deleteSubtree(nodeId);
        return getProjectTree(projectId);
    }

    public String exportProjectText(String projectId) {
        AnalysisProject project = getProject(projectId);
        FlowNodeTreeDto tree = getProjectTree(projectId);
        return textExporter.export(project, tree);
    }

    private void deleteSubtree(String nodeId) {
        List<FlowNode> children = flowNodeRepository.findByParentIdOrderBySortOrderAsc(nodeId);
        for (FlowNode child : children) {
            deleteSubtree(child.getId());
        }
        flowNodeRepository.deleteById(nodeId);
    }

    private FlowNodeTreeDto buildTree(List<FlowNode> nodes) {
        Map<String, FlowNodeTreeDto> dtoMap = new HashMap<>();
        Map<String, List<FlowNode>> childrenMap = new HashMap<>();

        for (FlowNode node : nodes) {
            dtoMap.put(node.getId(), new FlowNodeTreeDto(
                    node.getId(),
                    node.getTitle(),
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
