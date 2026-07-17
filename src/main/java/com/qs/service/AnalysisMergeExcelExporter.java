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
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
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

    /** 层级列交替底色（RGB） */
    private static final byte[][] LEVEL_FILLS = {
            rgb(232, 240, 254), // 蓝
            rgb(232, 245, 233), // 绿
            rgb(255, 243, 224), // 橙
            rgb(243, 229, 245), // 紫
            rgb(255, 235, 238), // 粉
            rgb(224, 247, 250)  // 青
    };
    private static final byte[] ROW_EVEN = rgb(250, 250, 250);
    private static final byte[] ROW_ODD = rgb(255, 255, 255);
    private static final byte[] HEADER_FILL = rgb(66, 133, 244);

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
        int lastCol = depth + 1;

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("合并层级");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle[] levelStyles = new CellStyle[depth];
            CellStyle[] levelStylesAlt = new CellStyle[depth];
            for (int c = 0; c < depth; c++) {
                byte[] fill = LEVEL_FILLS[c % LEVEL_FILLS.length];
                levelStyles[c] = createFilledStyle(workbook, fill, false);
                // 同级略深一档，便于相邻合并块区分
                levelStylesAlt[c] = createFilledStyle(workbook, darken(fill, 12), false);
            }
            CellStyle pyEven = createFilledStyle(workbook, ROW_EVEN, false);
            CellStyle pyOdd = createFilledStyle(workbook, ROW_ODD, false);
            CellStyle descEven = createFilledStyle(workbook, ROW_EVEN, true);
            CellStyle descOdd = createFilledStyle(workbook, ROW_ODD, true);

            Row header = sheet.createRow(0);
            header.setHeightInPoints(22);
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

            // 先为每一行每一列创建带边框的单元格（合并区域也要有底格，边框才完整）
            for (int r = 0; r < rows.size(); r++) {
                Row excelRow = sheet.createRow(r + 1);
                excelRow.setHeightInPoints(18);
                List<PathCell> path = rows.get(r);
                PathCell leaf = path.isEmpty() ? null : path.get(path.size() - 1);
                boolean odd = (r % 2) == 1;

                for (int c = 0; c < depth; c++) {
                    Cell cell = excelRow.createCell(c);
                    PathCell pc = c < path.size() ? path.get(c) : null;
                    // 合并块内非首行也写值，合并后显示首格；样式保证边框/底色不断档
                    cell.setCellValue(pc != null ? nullToEmpty(pc.title()) : "");
                    boolean altBlock = false;
                    if (pc != null) {
                        // 用 id hash 做同级块底色微差
                        altBlock = (pc.id() != null && (pc.id().hashCode() & 1) == 1);
                    }
                    cell.setCellStyle(altBlock ? levelStylesAlt[c] : levelStyles[c]);
                }

                Cell pyCell = excelRow.createCell(depth);
                pyCell.setCellValue(leaf != null ? nullToEmpty(leaf.pinyinCode()) : "");
                pyCell.setCellStyle(odd ? pyOdd : pyEven);

                Cell descCell = excelRow.createCell(depth + 1);
                descCell.setCellValue(leaf != null ? nullToEmpty(leaf.description()) : "");
                descCell.setCellStyle(odd ? descOdd : descEven);
            }

            // 再合并；并对合并区域强制四周边框
            List<CellRangeAddress> merged = new ArrayList<>();
            for (int r = 0; r < rows.size(); r++) {
                for (int c = 0; c < depth; c++) {
                    int span = spans[r][c];
                    if (span > 1) {
                        CellRangeAddress region = new CellRangeAddress(r + 1, r + span, c, c);
                        sheet.addMergedRegion(region);
                        merged.add(region);
                    }
                }
            }
            for (CellRangeAddress region : merged) {
                applyRegionBorder(sheet, region);
            }

            for (int i = 0; i < depth; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            sheet.setColumnWidth(depth, 14 * 256);
            sheet.setColumnWidth(depth + 1, 40 * 256);
            sheet.createFreezePane(0, 1);
            if (!rows.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, lastCol));
            }

            Sheet meta = workbook.createSheet("项目信息");
            CellStyle metaLabel = createFilledStyle(workbook, rgb(238, 238, 238), false);
            CellStyle metaValue = createFilledStyle(workbook, ROW_ODD, false);
            Row m0 = meta.createRow(0);
            m0.createCell(0).setCellValue("项目名称");
            m0.getCell(0).setCellStyle(metaLabel);
            m0.createCell(1).setCellValue(nullToEmpty(project.getName()));
            m0.getCell(1).setCellStyle(metaValue);
            Row m1 = meta.createRow(1);
            m1.createCell(0).setCellValue("项目简介");
            m1.getCell(0).setCellStyle(metaLabel);
            m1.createCell(1).setCellValue(nullToEmpty(project.getDescription()));
            m1.getCell(1).setCellStyle(metaValue);
            meta.setColumnWidth(0, 14 * 256);
            meta.setColumnWidth(1, 50 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出 Excel 失败：" + e.getMessage(), e);
        }
    }

    private void applyRegionBorder(Sheet sheet, CellRangeAddress region) {
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
        RegionUtil.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex(), region, sheet);
        RegionUtil.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex(), region, sheet);
        RegionUtil.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex(), region, sheet);
        RegionUtil.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex(), region, sheet);
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

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(HEADER_FILL, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(style);
        return style;
    }

    private CellStyle createFilledStyle(XSSFWorkbook workbook, byte[] rgb, boolean wrap) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(wrap);
        setBorder(style);
        return style;
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }

    private static byte[] rgb(int r, int g, int b) {
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }

    private static byte[] darken(byte[] rgb, int delta) {
        return new byte[]{
                (byte) Math.max(0, (rgb[0] & 0xff) - delta),
                (byte) Math.max(0, (rgb[1] & 0xff) - delta),
                (byte) Math.max(0, (rgb[2] & 0xff) - delta)
        };
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record PathCell(String id, String title, String pinyinCode, String description) {
    }
}
