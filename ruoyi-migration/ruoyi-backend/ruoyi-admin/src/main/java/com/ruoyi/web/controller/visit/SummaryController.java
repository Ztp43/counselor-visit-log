package com.ruoyi.web.controller.visit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.visit.domain.VisitItem;
import com.ruoyi.visit.domain.VisitRecord;
import com.ruoyi.visit.service.IAiService;
import com.ruoyi.visit.service.IVisitItemService;
import com.ruoyi.visit.service.IVisitRecordService;

/**
 * 领导总结性报告（周/月/学期/年）。
 *
 * 聚合指定周期内某辅导员的工作数据，量化指标 + AI 文字总结。
 * 数据权限：领导看全校/本学院，辅导员看自己（复用 @DataScope）。
 */
@RestController
@RequestMapping("/visit/report")
public class SummaryController extends BaseController
{
    private static final String[] EVENT_TYPES = {
        "dorm_check", "hygiene", "talk", "class_check", "meeting",
        "visit_help", "crisis", "admin", "other"
    };

    private final IVisitRecordService visitRecordService;
    private final IVisitItemService visitItemService;
    private final IAiService aiService;

    public SummaryController(IVisitRecordService visitRecordService,
                             IVisitItemService visitItemService,
                             IAiService aiService)
    {
        this.visitRecordService = visitRecordService;
        this.visitItemService = visitItemService;
        this.aiService = aiService;
    }

    @PreAuthorize("@ss.hasPermi('visit:record:list')")
    @GetMapping("/summary")
    public AjaxResult summary(
            @RequestParam(value = "period", defaultValue = "week") String period,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId)
    {
        LocalDate[] range = calcRange(period);
        LocalDate start = range[0];
        LocalDate end = range[1];
        String rangeLabel = start + " ~ " + end;

        // 查该周期内记录（含数据权限）
        VisitRecord q = new VisitRecord();
        if (targetUserId != null)
        {
            q.setUserId(targetUserId);
        }
        q.getParams().put("beginTime", start.toString());
        q.getParams().put("endTime", end.toString());
        List<VisitRecord> records = visitRecordService.selectVisitRecordList(q);

        // 量化指标：九类计数
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        for (String et : EVENT_TYPES)
        {
            typeCount.put(et, 0);
        }
        for (VisitRecord r : records)
        {
            String et = r.getEventType();
            if (et != null)
            {
                typeCount.merge(et, 1, Integer::sum);
            }
        }
        int total = records.size();

        // 事项闭环指标（同周期，复用 itemStats 的逻辑——这里按当前数据权限取全量事项统计）
        Map<String, Object> itemMetrics = visitItemService.itemStats(new VisitItem());

        // 组装指标
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("total", total);
        for (String et : EVENT_TYPES)
        {
            metrics.put(et, typeCount.getOrDefault(et, 0));
        }
        metrics.put("itemTotal", itemMetrics.getOrDefault("total", 0));
        metrics.put("itemPending", itemMetrics.getOrDefault("pending", 0));
        metrics.put("itemFollowing", itemMetrics.getOrDefault("following", 0));
        metrics.put("itemClosed", itemMetrics.getOrDefault("closed", 0));
        metrics.put("itemOverdue", itemMetrics.getOrDefault("overdue", 0));

        // AI 生成文字总结
        String summaryText = null;
        try
        {
            String dataJson = buildDataJson(period, rangeLabel, metrics);
            summaryText = aiService.generateSummary(dataJson);
        }
        catch (Exception ignored)
        {
            summaryText = null;
        }
        if (summaryText == null || summaryText.isBlank())
        {
            summaryText = fallbackSummary(period, rangeLabel, total, typeCount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("rangeLabel", rangeLabel);
        result.put("reporterName", SecurityUtils.getUsername());
        result.put("summary", summaryText);
        result.put("metrics", metrics);
        return success(result);
    }

    /** 周期起止：周=本周一至今天；月=本月1号至今天；学期=春秋学期；年=1月1号至今天 */
    private LocalDate[] calcRange(String period)
    {
        LocalDate today = LocalDate.now();
        return switch (period)
        {
            case "month" -> new LocalDate[]{today.withDayOfMonth(1), today};
            case "term" -> new LocalDate[]{termStart(today), today};
            case "year" -> new LocalDate[]{today.withDayOfYear(1), today};
            default -> new LocalDate[]{today.with(DayOfWeek.MONDAY), today};
        };
    }

    /** 学期起点：2~7月为春季学期(2月1号)，9~次年1月为秋季学期(9月1号) */
    private LocalDate termStart(LocalDate today)
    {
        int m = today.getMonthValue();
        if (m >= 2 && m <= 7)
        {
            return LocalDate.of(today.getYear(), 2, 1);
        }
        if (m >= 9)
        {
            return LocalDate.of(today.getYear(), 9, 1);
        }
        // 1 月：属于上一年的秋季学期
        return LocalDate.of(today.getYear() - 1, 9, 1);
    }

    private String buildDataJson(String period, String rangeLabel, Map<String, Object> metrics)
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"周期\":\"").append(periodCn(period)).append("\",");
        sb.append("\"时间范围\":\"").append(rangeLabel).append("\",");
        sb.append("\"记录总数\":").append(metrics.get("total")).append(",");
        sb.append("\"查寝点名\":").append(metrics.get("dorm_check")).append(",");
        sb.append("\"安全卫生\":").append(metrics.get("hygiene")).append(",");
        sb.append("\"谈心谈话\":").append(metrics.get("talk")).append(",");
        sb.append("\"查课听课\":").append(metrics.get("class_check")).append(",");
        sb.append("\"班会活动\":").append(metrics.get("meeting")).append(",");
        sb.append("\"走访帮扶\":").append(metrics.get("visit_help")).append(",");
        sb.append("\"危机处置\":").append(metrics.get("crisis")).append(",");
        sb.append("\"事务工作\":").append(metrics.get("admin")).append(",");
        sb.append("\"其他\":").append(metrics.get("other")).append(",");
        sb.append("\"事项总数\":").append(metrics.get("itemTotal")).append(",");
        sb.append("\"待处理事项\":").append(metrics.get("itemPending")).append(",");
        sb.append("\"跟进中事项\":").append(metrics.get("itemFollowing")).append(",");
        sb.append("\"已结案事项\":").append(metrics.get("itemClosed")).append(",");
        sb.append("\"超期事项\":").append(metrics.get("itemOverdue"));
        sb.append("}");
        return sb.toString();
    }

    private String periodCn(String period)
    {
        return switch (period)
        {
            case "month" -> "本月";
            case "term" -> "本学期";
            case "year" -> "本年度";
            default -> "本周";
        };
    }

    /** AI 不可用时的模板兜底 */
    private String fallbackSummary(String period, String rangeLabel, int total,
                                   Map<String, Integer> typeCount)
    {
        return String.format(
            "%s（%s）共录入工作日志 %d 条，其中查寝点名 %d 次、安全卫生 %d 次、谈心谈话 %d 次、" +
            "查课听课 %d 次、班会活动 %d 次、走访帮扶 %d 次、危机处置 %d 次、事务工作 %d 次、其他 %d 次。",
            periodCn(period), rangeLabel, total,
            typeCount.getOrDefault("dorm_check", 0),
            typeCount.getOrDefault("hygiene", 0),
            typeCount.getOrDefault("talk", 0),
            typeCount.getOrDefault("class_check", 0),
            typeCount.getOrDefault("meeting", 0),
            typeCount.getOrDefault("visit_help", 0),
            typeCount.getOrDefault("crisis", 0),
            typeCount.getOrDefault("admin", 0),
            typeCount.getOrDefault("other", 0));
    }
}
