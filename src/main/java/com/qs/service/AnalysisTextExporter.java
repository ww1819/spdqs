package com.qs.service;

import com.qs.dto.FlowNodeTreeDto;
import com.qs.entity.AnalysisProject;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AnalysisTextExporter {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String export(AnalysisProject project, FlowNodeTreeDto tree) {
        StringBuilder sb = new StringBuilder();
        int[] stats = new int[2];

        sb.append("========================================\n");
        sb.append("项目分析：").append(project.getName()).append("\n");
        sb.append("========================================\n");
        sb.append("项目简介：").append(blankToDash(project.getDescription())).append("\n");
        sb.append("创建人：").append(blankToDash(project.getCreateBy())).append("\n");
        sb.append("创建时间：").append(formatTime(project.getCreateTime())).append("\n");
        sb.append("\n");
        sb.append("----------------------------------------\n");
        sb.append("流程结构（树形）\n");
        sb.append("----------------------------------------\n");

        appendTreeSection(sb, tree, "1", "", true, stats);

        sb.append("\n");
        sb.append("----------------------------------------\n");
        sb.append("流程汇总（按阅读顺序）\n");
        sb.append("----------------------------------------\n");

        appendLinearSection(sb, tree, "1", 0, stats);

        sb.append("\n");
        sb.append("----------------------------------------\n");
        sb.append("统计：共 ").append(stats[0]).append(" 个流程节点，")
                .append(stats[1]).append(" 个已填写功能说明\n");
        sb.append("导出时间：").append(LocalDateTime.now().format(DT_FMT)).append("\n");
        sb.append("========================================\n");

        return sb.toString();
    }

    private void appendTreeSection(StringBuilder sb, FlowNodeTreeDto node, String number,
                                   String prefix, boolean isLast, int[] stats) {
        countNode(node, stats);

        sb.append(prefix);
        if (!prefix.isEmpty()) {
            sb.append(isLast ? "└─ " : "├─ ");
        }
        sb.append(number).append(". ").append(node.getTitle()).append("\n");

        String descPrefix = prefix + (prefix.isEmpty() ? "   " : (isLast ? "      " : "│     "));
        sb.append(descPrefix).append("【功能说明】").append(formatDescription(node.getDescription())).append("\n");

        List<FlowNodeTreeDto> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean last = i == children.size() - 1;
            String childNumber = number + "." + (i + 1);
            String childPrefix = prefix.isEmpty() ? "   " : prefix + (isLast ? "      " : "│     ");
            if (!children.get(i).getChildren().isEmpty() || i < children.size() - 1) {
                sb.append(childPrefix).append("│\n");
            }
            appendTreeSection(sb, children.get(i), childNumber, childPrefix, last, stats);
        }
    }

    private void appendLinearSection(StringBuilder sb, FlowNodeTreeDto node, String number,
                                     int depth, int[] stats) {
        String indent = "    ".repeat(depth);
        sb.append(indent).append("[").append(number).append("] ").append(node.getTitle()).append("\n");
        sb.append(indent).append("    ").append(formatDescription(node.getDescription())).append("\n");

        List<FlowNodeTreeDto> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            String childNumber = number + "." + (i + 1);
            sb.append(indent).append("    → ");
            appendLinearInline(sb, children.get(i), childNumber, depth + 1);
        }
    }

    private void appendLinearInline(StringBuilder sb, FlowNodeTreeDto node, String number, int depth) {
        sb.append("[").append(number).append("] ").append(node.getTitle()).append("\n");
        String indent = "    ".repeat(depth + 1);
        sb.append(indent).append(formatDescription(node.getDescription())).append("\n");

        List<FlowNodeTreeDto> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            String childNumber = number + "." + (i + 1);
            sb.append(indent).append("→ ");
            appendLinearInline(sb, children.get(i), childNumber, depth + 1);
        }
    }

    private void countNode(FlowNodeTreeDto node, int[] stats) {
        stats[0]++;
        if (node.isHasDescription()) {
            stats[1]++;
        }
        for (FlowNodeTreeDto child : node.getChildren()) {
            countNode(child, stats);
        }
    }

    private String formatDescription(String description) {
        if (description == null || description.isBlank()) {
            return "（未填写）";
        }
        return description.trim().replace("\r\n", "\n").replace("\n", "\n       ");
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "—" : time.format(DT_FMT);
    }
}
