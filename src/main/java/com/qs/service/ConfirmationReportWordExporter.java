package com.qs.service;

import com.qs.entity.Ticket;
import com.qs.entity.TicketFollowUp;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVerticalJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 导出可编辑的确认报告 Word，便于实施按客户现场增减内容、调整样式后打印签字。
 */
@Service
public class ConfirmationReportWordExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_CN = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String FONT = "宋体";
    private static final String FONT_TITLE = "黑体";

    public byte[] export(Ticket ticket, List<TicketFollowUp> followUps) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addBrandLine(doc, ticket);
            addCenteredTitle(doc, "需求 / 变更确认报告", 18, true);
            addCenteredTitle(doc, "Requirement & Change Confirmation Report", 10, false);
            addSpacer(doc, 6);

            addSectionTitle(doc, "一、基本信息");
            addInfoTable(doc, ticket);

            addSectionTitle(doc, "二、确认事项（工单内容）");
            addBodyBox(doc, blankToDash(ticket.getContent()), 4);

            if (ticket.getAttentionNote() != null && !ticket.getAttentionNote().isBlank()) {
                addSectionTitle(doc, "三、注意事项");
                addBodyBox(doc, ticket.getAttentionNote().trim(), 2);
                addSectionTitle(doc, "四、实施说明 / 完成情况");
            } else {
                addSectionTitle(doc, "三、实施说明 / 完成情况");
            }
            addImplementationSection(doc, followUps);

            addSectionTitle(doc, "客户确认声明");
            addParagraph(doc, "1. 已核对上述需求 / 变更内容与现场业务场景一致；", false, 11);
            addParagraph(doc, "2. 已确认实施结果符合约定，可投入使用（或按约定进入下一阶段）；", false, 11);
            addParagraph(doc, "3. 如有遗留问题，已在「注意事项」或下方签字区注明。", false, 11);
            addParagraph(doc, "（可按客户要求增删条款）", true, 10);
            addSpacer(doc, 4);

            addSectionTitle(doc, "签字栏");
            addSignTable(doc);

            addSpacer(doc, 10);
            addParagraph(doc,
                    "说明：本 Word 可由实施人员按不同客户现场情况增删段落、调整措辞与排版后再打印签字；"
                            + "签字扫描件请上传至系统工单「确认报告」作为凭证。非正式法律文书，不替代合同与正式验收文件。",
                    true, 9);

            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出确认报告 Word 失败", e);
        }
    }

    public String buildFileName(Ticket ticket) {
        String project = ticket.getArchive() != null ? ticket.getArchive().getProjectName() : "工单";
        String safe = project.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safe.length() > 40) {
            safe = safe.substring(0, 40);
        }
        String no = ticket.getTicketNo() != null ? "WO-" + ticket.getTicketNo() : "TK";
        return "确认报告_" + safe + "_" + no + ".docx";
    }

    private void addBrandLine(XWPFDocument doc, Ticket ticket) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun left = p.createRun();
        left.setText("管家工程系统 · SPDQS");
        left.setFontFamily(FONT);
        left.setFontSize(10);
        left.setColor("1F4E79");
        left.setBold(true);
        left.addBreak(BreakType.TEXT_WRAPPING);

        XWPFRun meta = p.createRun();
        meta.setText("工单编号：" + ticketNo(ticket) + "    导出日期：" + LocalDate.now().format(DATE_CN));
        meta.setFontFamily(FONT);
        meta.setFontSize(10);
        meta.setColor("5C6570");
    }

    private void addCenteredTitle(XWPFDocument doc, String text, int size, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingAfter(60);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontFamily(bold ? FONT_TITLE : FONT);
        run.setFontSize(size);
        run.setBold(bold);
        run.setColor("1A1D21");
    }

    private void addSectionTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(160);
        p.setSpacingAfter(80);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontFamily(FONT_TITLE);
        run.setFontSize(12);
        run.setBold(true);
        run.setColor("1F4E79");
    }

    private void addParagraph(XWPFDocument doc, String text, boolean muted, int size) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(40);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontFamily(FONT);
        run.setFontSize(size);
        if (muted) {
            run.setColor("5C6570");
        }
    }

    private void addSpacer(XWPFDocument doc, int after) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(after);
        p.createRun().setText("");
    }

    private void addBodyBox(XWPFDocument doc, String text, int minEmptyLines) {
        addParagraph(doc, text, false, 11);
        for (int i = 0; i < minEmptyLines; i++) {
            XWPFParagraph line = doc.createParagraph();
            line.setSpacingAfter(40);
            XWPFRun run = line.createRun();
            run.setText("________________________________________________________________");
            run.setFontFamily(FONT);
            run.setFontSize(10);
            run.setColor("9AA6B2");
        }
        addParagraph(doc, "（上方横线可手写补充，或在 Word 中直接增删文字）", true, 9);
    }

    private void addImplementationSection(XWPFDocument doc, List<TicketFollowUp> followUps) {
        if (followUps == null || followUps.isEmpty()) {
            addBodyBox(doc, "（暂无跟进记录，请在下方填写实施说明）", 4);
            return;
        }
        int i = 1;
        for (TicketFollowUp fu : followUps) {
            String meta = i + ". "
                    + blankToDash(fu.getCreateBy()) + "  "
                    + formatDateTime(fu.getCreateTime());
            addParagraph(doc, meta, true, 10);
            addParagraph(doc, blankToDash(fu.getContent()), false, 11);
            i++;
        }
        addBodyBox(doc, "（可继续补充实施说明）", 2);
    }

    private void addInfoTable(XWPFDocument doc, Ticket ticket) {
        XWPFTable table = doc.createTable(4, 4);
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);

        setInfoCell(table, 0, 0, "项目名称", true);
        setInfoCell(table, 0, 1, ticket.getArchive() != null ? blankToDash(ticket.getArchive().getProjectName()) : "—", false);
        setInfoCell(table, 0, 2, "工单类型", true);
        setInfoCell(table, 0, 3, blankToDash(ticket.getOrderType()), false);

        setInfoCell(table, 1, 0, "当前状态", true);
        setInfoCell(table, 1, 1, blankToDash(ticket.getStatus()), false);
        setInfoCell(table, 1, 2, "提交时间", true);
        setInfoCell(table, 1, 3, formatDateTime(ticket.getCreateTime()), false);

        setInfoCell(table, 2, 0, "提交人", true);
        setInfoCell(table, 2, 1, blankToDash(ticket.getSubmitter()), false);
        setInfoCell(table, 2, 2, "处理人", true);
        setInfoCell(table, 2, 3, blankToDash(ticket.getHandler()), false);

        setInfoCell(table, 3, 0, "目标完成", true);
        setInfoCell(table, 3, 1, formatDate(ticket.getTargetCompleteDate()), false);
        setInfoCell(table, 3, 2, "预计完成", true);
        setInfoCell(table, 3, 3, formatDate(ticket.getExpectedCompleteDate()), false);
    }

    private void addSignTable(XWPFDocument doc) {
        XWPFTable table = doc.createTable(1, 2);
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);

        fillSignCard(table.getRow(0).getCell(0), "客户方确认");
        fillSignCard(table.getRow(0).getCell(1), "实施方确认");
    }

    private void fillSignCard(XWPFTableCell cell, String title) {
        clearCell(cell);
        XWPFParagraph titleP = cell.addParagraph();
        XWPFRun titleRun = titleP.createRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        titleRun.setFontFamily(FONT_TITLE);
        titleRun.setFontSize(11);
        titleRun.setColor("1F4E79");

        addSignLine(cell, "单位名称");
        addSignLine(cell, "确认人签字");
        addSignLine(cell, "联系电话");
        addSignLine(cell, "确认日期");
    }

    private void addSignLine(XWPFTableCell cell, String label) {
        XWPFParagraph p = cell.addParagraph();
        p.setSpacingBefore(120);
        XWPFRun run = p.createRun();
        run.setText(label + "：____________________");
        run.setFontFamily(FONT);
        run.setFontSize(11);
    }

    private void setInfoCell(XWPFTable table, int rowIdx, int colIdx, String text, boolean header) {
        XWPFTableRow row = table.getRow(rowIdx);
        XWPFTableCell cell = row.getCell(colIdx);
        clearCell(cell);
        if (header) {
            cell.setColor("F4F6F8");
        }
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTVerticalJc vJc = tcPr.isSetVAlign() ? tcPr.getVAlign() : tcPr.addNewVAlign();
        vJc.setVal(STVerticalJc.CENTER);

        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = p.createRun();
        run.setText(text == null ? "" : text);
        run.setFontFamily(FONT);
        run.setFontSize(10);
        run.setBold(header);
    }

    private void clearCell(XWPFTableCell cell) {
        for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
    }

    private static String ticketNo(Ticket ticket) {
        if (ticket.getTicketNo() != null) {
            return "WO-" + ticket.getTicketNo();
        }
        return ticket.getId() != null ? ticket.getId() : "—";
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE);
    }

    private static String formatDateTime(LocalDateTime time) {
        return time == null ? "—" : time.format(DATETIME);
    }
}
