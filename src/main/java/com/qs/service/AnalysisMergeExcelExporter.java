package com.qs.service;

import com.qs.dto.FlowNodeTreeDto;
import com.qs.entity.AnalysisProject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 按合并层级视图样式导出 Excel：叶子路径展平 + 同祖先纵向合并单元格。
 */
@Service
public class AnalysisMergeExcelExporter {

    public byte[] export(AnalysisProject project, FlowNodeTreeDto tree) {
        List<List<PathCell>> rows = flattenPaths(tree, new ArrayList<>());
        int depth = 0;
        for (List<PathCell> row : rows) {
            depth = Math.max(depth, row.size());
        }
        if (depth == 0) {
            depth = 1;
        }
        int[][] spans = computeRowspans(rows, depth);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("合并层级");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle cellStyle = createCellStyle(workbook);
            CellStyle wrapStyle = createWrapStyle(workbook);

            Row header = sheet.createRow(0);
            for (int i = 0; i < depth; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue("第" + (i + 1) + "级");
                cell.setCellStyle(headerStyle);
            }
            Cell pyHeader = header.createCell(depth);
            pyHeader.setCellValue("拼音简码");
            pyHeader.setCellStyle(headerStyle);
            Cell descHeader = header.createCell(depth + 1);
            descHeader.setCellValue("功能描述");
            descHeader.setCellStyle(headerStyle);

            for (int r = 0; r < rows.size(); r++) {
                Row excelRow = sheet.createRow(r + 1);
                List<PathCell> path = rows.get(r);
                PathCell leaf = path.isEmpty() ? null : path.get(path.size() - 1);

                for (int c = 0; c < depth; c++) {
                    int span = spans[r][c];
                    if (span == 0) {
                        continue;
                    }
                    Cell cell = excelRow.createCell(c);
                    PathCell pc = c < path.size() ? path.get(c) : null;
                    cell.setCellValue(pc != null ? nullToEmpty(pc.title()) : "—");
                    cell.setCellStyle(cellStyle);
                    if (span > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(r + 1, r + span, c, c));
                    }
                }

                Cell pyCell = excelRow.createCell(depth);
                pyCell.setCellValue(leaf != null ? nullToEmpty(leaf.pinyinCode()) : "");
                pyCell.setCellStyle(cellStyle);

                Cell descCell = excelRow.createCell(depth + 1);
                descCell.setCellValue(leaf != null ? nullToEmpty(leaf.description()) : "");
                descCell.setCellStyle(wrapStyle);
            }

            for (int i = 0; i < depth; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            sheet.setColumnWidth(depth, 14 * 256);
            sheet.setColumnWidth(depth + 1, 40 * 256);
            sheet.createFreezePane(0, 1);

            // 标题行信息放在第二 sheet 避免破坏合并表
            Sheet meta = workbook.createSheet("项目信息");
            meta.createRow(0).createCell(0).setCellValue("项目名称");
            meta.getRow(0).createCell(1).setCellValue(nullToEmpty(project.getName()));
            meta.createRow(1).createCell(0).setCellValue("项目简介");
            meta.getRow(1).createCell(1).setCellValue(nullToEmpty(project.getDescription()));
            meta.setColumnWidth(0, 14 * 256);
            meta.setColumnWidth(1, 50 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出 Excel 失败：" + e.getMessage(), e);
        }
    }

    private List<List<PathCell>> flattenPaths(FlowNodeTreeDto node, List<PathCell> path) {
        List<PathCell> current = new ArrayList<>(path);
        current.add(new PathCell(node.getId(), node.getTitle(), node.getPinyinCode(), node.getDescription()));
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return List.of(current);
        }
        List<List<PathCell>> rows = new ArrayList<>();
        for (FlowNodeTreeDto child : node.getChildren()) {
            rows.addAll(flattenPaths(child, current));
        }
        return rows;
    }

    private int[][] computeRowspans(List<List<PathCell>> rows, int depth) {
        int[][] spans = new int[rows.size()][depth];
        for (int col = 0; col < depth; col++) {
            int i = 0;
            while (i < rows.size()) {
                int j = i + 1;
                while (j < rows.size()) {
                    boolean same = true;
                    for (int c = 0; c <= col; c++) {
                        PathCell a = cellAt(rows.get(i), c);
                        PathCell b = cellAt(rows.get(j), c);
                        if (a == null || b == null || a.id() == null || !a.id().equals(b.id())) {
                            same = false;
                            break;
                        }
                    }
                    if (!same) {
                        break;
                    }
                    j++;
                }
                spans[i][col] = j - i;
                for (int k = i + 1; k < j; k++) {
                    spans[k][col] = 0;
                }
                i = j;
            }
        }
        return spans;
    }

    private PathCell cellAt(List<PathCell> row, int col) {
        return col < row.size() ? row.get(col) : null;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(style);
        return style;
    }

    private CellStyle createCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);
        setBorder(style);
        return style;
    }

    private CellStyle createWrapStyle(Workbook workbook) {
        CellStyle style = createCellStyle(workbook);
        style.setWrapText(true);
        return style;
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record PathCell(String id, String title, String pinyinCode, String description) {
    }
}
