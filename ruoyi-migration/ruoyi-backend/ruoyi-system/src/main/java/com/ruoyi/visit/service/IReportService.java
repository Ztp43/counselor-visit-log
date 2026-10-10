package com.ruoyi.visit.service;

import java.time.LocalDate;
import java.util.List;
import com.ruoyi.visit.domain.VisitRecord;

/**
 * Excel 报表导出。迁移自现网 ReportService。
 */
public interface IReportService
{
    /**
     * 工作报表（汇总页 + 逐月趋势 + 明细页），返回 xlsx 字节。
     *
     * @param records 已按时间/关键词过滤好的记录
     * @param reporterName 报告人姓名
     * @param rangeLabel 统计范围描述（写进副标题）
     * @param titleName 指定辅导员姓名（null=通用标题）
     */
    byte[] buildReport(List<VisitRecord> records, String reporterName, String rangeLabel, String titleName);

    /**
     * 周报（本周一至今天），返回 xlsx 字节。
     */
    byte[] buildWeeklyReport(List<VisitRecord> records, String reporterName, String weekLabel,
                             List<LocalDate> days, String titleName);
}
