package com.ruoyi.visit.service.impl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.ruoyi.visit.common.EventTypes;
import com.ruoyi.visit.domain.VisitRecord;
import com.ruoyi.visit.service.IReportService;

/**
 * Excel 报表（Apache POI）。迁移自现网 ReportServiceImpl。
 *
 * 结构：
 *   - 工作汇总：横幅 + 核心指标 + 类型分布 + 详细工作事务
 *   - 逐月趋势：分月 + 合计
 *   - 工作明细：序号/记录人/时间/类型/地点/学生/详情/原文
 *   - 周报：本周概览 + 按日统计 + 明细
 *
 * 时间语义：统计/分组按「补录日期 record_date」优先，无补录时用 create_time。
 * （补录的「上周五」应算到上周五，而非今天）
 */
@Service
public class ReportServiceImpl implements IReportService
{
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public byte[] buildReport(List<VisitRecord> records, String reporterName, String rangeLabel, String titleName)
    {
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            int total = records.size();
            Map<String, Integer> counts = countByType(records);
            String info = infoLine(reporterName, rangeLabel, "");
            String mainTitle = titleName != null ? "辅导员" + titleName + "工作日志" : "辅导员工作日志报表";
            String detailTitle = titleName != null ? "辅导员" + titleName + "工作日志明细" : "辅导员工作日志明细";

            Sheet summary = wb.createSheet("工作汇总");
            buildSummarySheet(summary, records, counts, total, mainTitle, info);

            Sheet trend = wb.createSheet("逐月趋势");
            buildTrendSheet(trend, records, counts, total, info);

            Sheet detail = wb.createSheet("工作明细");
            buildDetailSheet(detail, records, detailTitle, info, 8);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new RuntimeException("报表生成失败", e);
        }
    }

    @Override
    public byte[] buildWeeklyReport(List<VisitRecord> records, String reporterName, String weekLabel,
                                    List<LocalDate> days, String titleName)
    {
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            int total = records.size();
            Map<String, Integer> counts = countByType(records);
            String info = infoLine(reporterName, weekLabel, "");
            String mainTitle = titleName != null ? "辅导员" + titleName + "周报" : "辅导员工作周报";

            Sheet summary = wb.createSheet("周报汇总");
            buildWeeklySummary(summary, records, counts, total, days, mainTitle, info);

            Sheet detail = wb.createSheet("事务明细");
            buildDetailSheet(detail, records, "本周工作事务明细", info, 6);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
        catch (Exception e)
        {
            throw new RuntimeException("周报生成失败", e);
        }
    }

    // ==================== 汇总页 ====================

    private void buildSummarySheet(Sheet ws, List<VisitRecord> records, Map<String, Integer> counts,
                                   int total, String title, String info)
    {
        ws.setColumnWidth(0, 10 * 256);
        for (int i = 1; i < 10; i++) ws.setColumnWidth(i, 10 * 256);

        banner(ws, title, info, 10);

        int r = 3;
        Cell section = ws.createRow(r).createCell(0);
        section.setCellValue("一、核心指标");
        r = metricCards(ws, records, r + 1);

        r += 2;
        ws.createRow(r).createCell(0).setCellValue("二、工作类型分布");
        r = typeTable(ws, counts, total, r + 1);

        r += 2;
        ws.createRow(r).createCell(0).setCellValue("三、辅导员详细工作事务");
        workItems(ws, records, r + 1);
    }

    private int metricCards(Sheet ws, List<VisitRecord> records, int row)
    {
        int total = records.size();
        int nTypes = (int) records.stream().map(VisitRecord::getEventType).distinct().count();
        int nLocs = (int) records.stream().map(VisitRecord::getLocation)
                .filter(s -> s != null && !s.isEmpty()).distinct().count();

        String[][] cards = {
                {"总工作次数", total + " 次"},
                {"工作类型", nTypes + " 类"},
                {"涉及地点", nLocs + " 个"},
        };
        int[][] spans = {{0, 3}, {3, 6}, {6, 10}};
        for (int i = 0; i < 3; i++)
        {
            Cell label = ws.createRow(row).createCell(spans[i][0]);
            label.setCellValue(cards[i][0]);
            ws.addMergedRegion(new CellRangeAddress(row, row, spans[i][0], spans[i][1] - 1));
            Cell val = ws.createRow(row + 1).createCell(spans[i][0]);
            val.setCellValue(cards[i][1]);
            ws.addMergedRegion(new CellRangeAddress(row + 1, row + 1, spans[i][0], spans[i][1] - 1));
        }
        return row + 1;
    }

    private int typeTable(Sheet ws, Map<String, Integer> counts, int total, int headRow)
    {
        String[] headers = {"类型", "次数", "占比"};
        int[][] spans = {{0, 4}, {4, 6}, {6, 10}};
        Row hr = ws.createRow(headRow);
        for (int i = 0; i < 3; i++)
        {
            hr.createCell(spans[i][0]).setCellValue(headers[i]);
            mergeRange(ws, headRow, spans[i][0], spans[i][1] - 1);
        }
        int r = headRow + 1;
        for (String et : EventTypes.CN.keySet())
        {
            int cnt = counts.getOrDefault(et, 0);
            double pct = total == 0 ? 0 : cnt * 100.0 / total;
            Row row = ws.createRow(r);
            row.createCell(0).setCellValue(EventTypes.cn(et));
            row.createCell(4).setCellValue(cnt);
            row.createCell(6).setCellValue(String.format("%.1f%%", pct));
            r++;
        }
        Row totalRow = ws.createRow(r);
        totalRow.createCell(0).setCellValue("合计");
        totalRow.createCell(4).setCellValue(total);
        totalRow.createCell(6).setCellValue(total == 0 ? "0.0%" : "100.0%");
        return r;
    }

    private void workItems(Sheet ws, List<VisitRecord> records, int headRow)
    {
        String[] headers = {"时间", "类型", "地点", "涉及学生", "工作详情"};
        int[][] spans = {{0, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 10}};
        Row hr = ws.createRow(headRow);
        for (int i = 0; i < headers.length; i++)
        {
            hr.createCell(spans[i][0]).setCellValue(headers[i]);
            mergeRange(ws, headRow, spans[i][0], spans[i][1] - 1);
        }
        int r = headRow + 1;
        for (VisitRecord rec : records)
        {
            Row row = ws.createRow(r);
            row.createCell(0).setCellValue(fmt(rec, DTF));
            row.createCell(2).setCellValue(EventTypes.cn(rec.getEventType()));
            row.createCell(3).setCellValue(nvl(rec.getLocation()));
            row.createCell(4).setCellValue(nvl(rec.getStudentName()));
            row.createCell(5).setCellValue(nvl(rec.getDetail(), "（无详情）"));
            r++;
        }
        if (records.isEmpty())
        {
            Row row = ws.createRow(r);
            row.createCell(0).setCellValue("（统计范围内暂无记录）");
            ws.addMergedRegion(new CellRangeAddress(r, r, 0, 9));
        }
    }

    // ==================== 逐月趋势 ====================

    private void buildTrendSheet(Sheet ws, List<VisitRecord> records, Map<String, Integer> counts, int total, String info)
    {
        int nCols = 2 + EventTypes.CN.size();
        banner(ws, "逐月工作趋势", info, nCols);

        List<String> typeOrder = new ArrayList<>(EventTypes.CN.keySet());
        int headRow = 3;
        Row hr = ws.createRow(headRow);
        hr.createCell(0).setCellValue("月份");
        hr.createCell(1).setCellValue("总次数");
        for (int i = 0; i < typeOrder.size(); i++)
        {
            hr.createCell(2 + i).setCellValue(EventTypes.cn(typeOrder.get(i)));
        }

        Map<String, Map<String, Integer>> monthMap = new TreeMap<>();
        for (VisitRecord rec : records)
        {
            String m = fmt(rec, MONTH_FMT);
            monthMap.computeIfAbsent(m, k -> new HashMap<>()).merge(rec.getEventType(), 1, Integer::sum);
        }

        int r = headRow + 1;
        for (Map.Entry<String, Map<String, Integer>> e : monthMap.entrySet())
        {
            Row row = ws.createRow(r);
            row.createCell(0).setCellValue(e.getKey());
            int sum = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            row.createCell(1).setCellValue(sum);
            for (int i = 0; i < typeOrder.size(); i++)
            {
                row.createCell(2 + i).setCellValue(e.getValue().getOrDefault(typeOrder.get(i), 0));
            }
            r++;
        }
        Row totalRow = ws.createRow(r);
        totalRow.createCell(0).setCellValue("合计");
        totalRow.createCell(1).setCellValue(total);
        for (int i = 0; i < typeOrder.size(); i++)
        {
            totalRow.createCell(2 + i).setCellValue(counts.getOrDefault(typeOrder.get(i), 0));
        }
    }

    // ==================== 明细页 ====================

    private void buildDetailSheet(Sheet ws, List<VisitRecord> records, String title, String info, int nCols)
    {
        banner(ws, title, info, nCols);
        String[] headers;
        if (nCols == 8)
        {
            headers = new String[]{"序号", "记录人", "时间", "类型", "地点", "涉及学生", "工作详情", "语音/文字原文"};
        }
        else
        {
            headers = new String[]{"序号", "时间", "类型", "地点", "涉及学生", "工作详情"};
        }
        int headRow = 3;
        Row hr = ws.createRow(headRow);
        for (int i = 0; i < headers.length; i++) hr.createCell(i).setCellValue(headers[i]);

        int r = headRow + 1;
        int i = 1;
        for (VisitRecord rec : records)
        {
            Row row = ws.createRow(r);
            row.createCell(0).setCellValue(i);
            int c = 1;
            if (nCols == 8)
            {
                row.createCell(c++).setCellValue(nvl(rec.getRecorderName()));
                row.createCell(c++).setCellValue(fmt(rec, DTF));
            }
            else
            {
                row.createCell(c++).setCellValue(fmt(rec, DTF));
            }
            row.createCell(c++).setCellValue(EventTypes.cn(rec.getEventType()));
            row.createCell(c++).setCellValue(nvl(rec.getLocation()));
            row.createCell(c++).setCellValue(nvl(rec.getStudentName()));
            row.createCell(c++).setCellValue(nvl(rec.getDetail()));
            if (nCols == 8) row.createCell(c).setCellValue(nvl(rec.getRawText()));
            r++;
            i++;
        }
        if (records.isEmpty())
        {
            Row row = ws.createRow(r);
            row.createCell(0).setCellValue("（统计范围内暂无记录）");
            ws.addMergedRegion(new CellRangeAddress(r, r, 0, nCols - 1));
        }
    }

    // ==================== 周报 ====================

    private void buildWeeklySummary(Sheet ws, List<VisitRecord> records, Map<String, Integer> counts,
                                    int total, List<LocalDate> days, String title, String info)
    {
        ws.setColumnWidth(0, 12 * 256);
        for (int i = 1; i < 11; i++) ws.setColumnWidth(i, 8 * 256);

        banner(ws, title, info, 11);

        int r = 3;
        ws.createRow(r).createCell(0).setCellValue("一、本周概览");
        r = metricCards(ws, records, r + 1);

        r += 2;
        ws.createRow(r).createCell(0).setCellValue("二、按日工作统计");
        int headRow = r + 1;
        Row hr = ws.createRow(headRow);
        hr.createCell(0).setCellValue("日期");
        List<String> typeOrder = new ArrayList<>(EventTypes.CN.keySet());
        for (int i = 0; i < typeOrder.size(); i++) hr.createCell(1 + i).setCellValue(EventTypes.cn(typeOrder.get(i)));
        hr.createCell(10).setCellValue("合计");

        Map<String, Map<String, Integer>> dayMap = new HashMap<>();
        for (VisitRecord rec : records)
        {
            String d = fmt(rec, DAY_FMT);
            dayMap.computeIfAbsent(d, k -> new HashMap<>()).merge(rec.getEventType(), 1, Integer::sum);
        }

        r = headRow + 1;
        for (LocalDate d : days)
        {
            String key = d.format(DAY_FMT);
            Map<String, Integer> m = dayMap.getOrDefault(key, Map.of());
            Row row = ws.createRow(r);
            row.createCell(0).setCellValue(key);
            for (int i = 0; i < typeOrder.size(); i++) row.createCell(1 + i).setCellValue(m.getOrDefault(typeOrder.get(i), 0));
            row.createCell(10).setCellValue(m.values().stream().mapToInt(Integer::intValue).sum());
            r++;
        }
        Row totalRow = ws.createRow(r);
        totalRow.createCell(0).setCellValue("合计");
        for (int i = 0; i < typeOrder.size(); i++) totalRow.createCell(1 + i).setCellValue(counts.getOrDefault(typeOrder.get(i), 0));
        totalRow.createCell(10).setCellValue(total);

        r += 2;
        ws.createRow(r).createCell(0).setCellValue("三、本周工作事务明细");
        workItems(ws, records, r + 1);
    }

    // ==================== 工具 ====================

    /** 合并单元格：只在区间宽度 >1 时才合并，否则 POI 抛
     *  "Merged region must contain 2 or more cells"（单列合并是老系统踩过的坑）。 */
    private void mergeRange(Sheet ws, int row, int c1, int c2)
    {
        if (c2 > c1)
        {
            ws.addMergedRegion(new CellRangeAddress(row, row, c1, c2));
        }
    }

    private void banner(Sheet ws, String title, String info, int nCols)
    {
        Row t = ws.createRow(0);
        Cell tc = t.createCell(0);
        tc.setCellValue(title);
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, nCols - 1));
        CellStyle titleStyle = ws.getWorkbook().createCellStyle();
        Font f = ws.getWorkbook().createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 17);
        f.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(f);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        tc.setCellStyle(titleStyle);

        Row s = ws.createRow(1);
        Cell sc = s.createCell(0);
        sc.setCellValue(info);
        ws.addMergedRegion(new CellRangeAddress(1, 1, 0, nCols - 1));
        CellStyle subStyle = ws.getWorkbook().createCellStyle();
        Font sf = ws.getWorkbook().createFont();
        sf.setFontHeightInPoints((short) 10);
        sf.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subStyle.setFont(sf);
        subStyle.setAlignment(HorizontalAlignment.CENTER);
        sc.setCellStyle(subStyle);
    }

    private String infoLine(String reporter, String rangeLabel, String extra)
    {
        String now = LocalDate.now().atTime(0, 0).format(DTF);
        StringBuilder sb = new StringBuilder();
        sb.append("报告人：").append(reporter).append("　|　统计范围：").append(rangeLabel);
        if (extra != null && !extra.isEmpty()) sb.append("　|　").append(extra);
        sb.append("　|　导出时间：").append(java.time.LocalDateTime.now().format(DTF));
        return sb.toString();
    }

    private Map<String, Integer> countByType(List<VisitRecord> records)
    {
        Map<String, Integer> counts = new HashMap<>();
        for (VisitRecord r : records) counts.merge(r.getEventType(), 1, Integer::sum);
        return counts;
    }

    /** 记录的有效时间：补录日期 record_date 优先，无补录用 create_time */
    private Date effectiveTime(VisitRecord rec)
    {
        return rec.getRecordDate() != null ? rec.getRecordDate() : rec.getCreateTime();
    }

    private String fmt(VisitRecord rec, DateTimeFormatter formatter)
    {
        Date d = effectiveTime(rec);
        if (d == null) return "";
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter);
    }

    private String nvl(String s)
    {
        return s == null ? "" : s;
    }

    private String nvl(String s, String def)
    {
        return (s == null || s.isEmpty()) ? def : s;
    }
}
