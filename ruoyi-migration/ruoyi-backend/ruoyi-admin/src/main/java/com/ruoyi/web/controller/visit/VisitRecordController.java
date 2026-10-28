package com.ruoyi.web.controller.visit;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.visit.domain.VisitRecord;
import com.ruoyi.visit.service.IAiService;
import com.ruoyi.visit.service.IAsrService;
import com.ruoyi.visit.service.IReportService;
import com.ruoyi.visit.service.IStorageService;
import com.ruoyi.visit.service.IVisitOrgService;
import com.ruoyi.visit.service.IVisitRecordService;

/**
 * 辅导员走访记录（迁移到若依）
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/visit/record")
public class VisitRecordController extends BaseController
{
    @Autowired
    private IVisitRecordService visitRecordService;

    @Autowired
    private IAiService aiService;

    @Autowired
    private IAsrService asrService;

    @Autowired
    private IStorageService storageService;

    @Autowired
    private IReportService reportService;

    @Autowired
    private IVisitOrgService visitOrgService;

    @Autowired
    private ISysUserService sysUserService;

    /**
     * 查询走访记录列表（四级数据权限自动过滤）
     */
    @PreAuthorize("@ss.hasPermi('visit:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(VisitRecord record)
    {
        startPage();
        List<VisitRecord> list = visitRecordService.selectVisitRecordList(record);
        return getDataTable(list);
    }

    /**
     * 回收站列表（软删除记录，四级数据权限自动过滤）
     */
    @PreAuthorize("@ss.hasPermi('visit:record:list')")
    @GetMapping("/trash")
    public TableDataInfo trash(VisitRecord record)
    {
        startPage();
        List<VisitRecord> list = visitRecordService.selectTrashList(record);
        return getDataTable(list);
    }

    /**
     * 导出 Excel 工作报表（汇总 + 逐月趋势 + 明细）。
     * start/end 为可选日期范围；keyword 为可选关键词；targetUserId 为可选下钻辅导员。
     */
    @PreAuthorize("@ss.hasPermi('visit:record:list')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @RequestParam(value = "keyword", required = false) String keyword)
    {
        if (start != null && end != null && !start.isBlank() && !end.isBlank() && start.compareTo(end) > 0)
        {
            throw new com.ruoyi.common.exception.ServiceException("开始日期不能晚于结束日期");
        }

        String kw = keyword == null ? "" : keyword.strip();
        if (kw.length() > 50) kw = kw.substring(0, 50);

        // 当前登录人（报告人姓名：优先昵称/真实姓名，退回用户名）
        String reporterName = SecurityUtils.getUsername();
        com.ruoyi.common.core.domain.entity.SysUser self = sysUserService.selectUserById(SecurityUtils.getUserId());
        if (self != null && com.ruoyi.common.utils.StringUtils.isNotBlank(self.getNickName()))
        {
            reporterName = self.getNickName();
        }

        // 下钻对象名（用于标题）
        String titleName = null;
        if (targetUserId != null)
        {
            // 下钻导出某个辅导员（领导/管理员可用，越权时数据权限会过滤为空）
            // 用真实姓名做标题，而非 userId 数字
            com.ruoyi.common.core.domain.entity.SysUser target = sysUserService.selectUserById(targetUserId);
            if (target != null)
            {
                titleName = com.ruoyi.common.utils.StringUtils.isNotBlank(target.getNickName())
                        ? target.getNickName() : target.getUserName();
            }
            else
            {
                titleName = String.valueOf(targetUserId);
            }
        }
        else if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()))
        {
            // 非管理员且不下钻：报告人 = 自己
            titleName = reporterName;
        }

        // 统计范围描述
        String rangeLabel = buildRangeLabel(start, end, kw);

        // 取记录（复用 @DataScope 数据权限过滤，不分页）
        VisitRecord query = new VisitRecord();
        if (targetUserId != null) query.setUserId(targetUserId);
        if (kw != null && !kw.isEmpty()) query.setKeyword(kw);
        if (start != null && !start.isBlank()) query.getParams().put("beginTime", start);
        if (end != null && !end.isBlank()) query.getParams().put("endTime", end);
        List<VisitRecord> records = visitRecordService.selectVisitRecordList(query);

        byte[] data = reportService.buildReport(records, reporterName, rangeLabel, titleName);

        String filename = (titleName != null && !titleName.equals(reporterName)
                ? "辅导员" + titleName + "工作日志" : "辅导员工作日志") + ".xlsx";
        return toResponse(data, filename);
    }

    /**
     * 导出周报（本周一至今天）。
     */
    @PreAuthorize("@ss.hasPermi('visit:record:list')")
    @GetMapping("/weekly-report")
    public ResponseEntity<byte[]> weeklyReport(
            @RequestParam(value = "targetUserId", required = false) Long targetUserId)
    {
        String reporterName = SecurityUtils.getUsername();
        String titleName = null;
        if (targetUserId != null)
        {
            titleName = String.valueOf(targetUserId);
        }
        else if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()))
        {
            titleName = reporterName;
        }

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = monday; !d.isAfter(today); d = d.plusDays(1))
        {
            days.add(d);
        }

        String weekLabel = monday + " ~ " + today;
        VisitRecord query = new VisitRecord();
        if (targetUserId != null) query.setUserId(targetUserId);
        query.getParams().put("beginTime", monday.toString());
        query.getParams().put("endTime", today.toString());
        List<VisitRecord> records = visitRecordService.selectVisitRecordList(query);

        byte[] data = reportService.buildWeeklyReport(records, reporterName, weekLabel, days, titleName);

        String filename = (titleName != null && !titleName.equals(reporterName)
                ? "辅导员" + titleName + "周报" : "周报") + ".xlsx";
        return toResponse(data, filename);
    }

    private String buildRangeLabel(String start, String end, String kw)
    {
        String label;
        if (start != null && end != null && !start.isBlank() && !end.isBlank()) label = start + " ~ " + end;
        else if (start != null && !start.isBlank()) label = start + " 起";
        else if (end != null && !end.isBlank()) label = "截至 " + end;
        else label = "全部时间";
        if (kw != null && !kw.isEmpty())
        {
            label += "（关键词「" + (kw.length() > 20 ? kw.substring(0, 20) + "…" : kw) + "」）";
        }
        return label;
    }

    private ResponseEntity<byte[]> toResponse(byte[] data, String filename)
    {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        return new ResponseEntity<>(data, headers, 200);
    }

    /**
     * AI 文字录入：text → AI 意图识别（含补录日期）→ 落库
     * dryRun=true 时只返回识别结果不落库（供前端补录日期确认）。
     */
    @PreAuthorize("@ss.hasPermi('visit:record:add')")
    @PostMapping("/chat")
    public AjaxResult chat(
            @RequestParam(value = "text", defaultValue = "") String text,
            @RequestParam(value = "recordDate", required = false) String recordDate,
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
            @RequestParam(value = "files", required = false) MultipartFile[] files)
    {
        // 诊断日志：排查浏览器端 FormData/参数传递问题（2026-09-29）
        logger.info("[chat] contentType={}, textLen={}, dryRun={}, files={}",
                com.ruoyi.common.utils.ServletUtils.getRequest().getContentType(),
                text == null ? 0 : text.length(),
                dryRun,
                files == null ? 0 : files.length);

        text = text == null ? "" : text.strip();
        if (text.isEmpty())
        {
            return error("请说点什么");
        }

        Map<String, String> parsed = aiService.parseIntent(text);

        // 补录日期优先级：AI 的 record_date > 前端 recordDate
        String targetDate = recordDate;
        String aiDate = parsed.get("record_date");
        if (aiDate != null && !aiDate.isBlank())
        {
            targetDate = aiDate;
        }

        // 补录日期边界校验（issue #15，迁移自旧版：仅限当年内且不能是未来）
        // dryRun 也校验——前端在确认弹窗前就拿到明确错误，避免确认后才失败
        if (targetDate != null && !targetDate.isBlank())
        {
            java.time.LocalDate target;
            try
            {
                target = java.time.LocalDate.parse(targetDate);
            }
            catch (Exception e)
            {
                return error("无法识别补录日期「" + targetDate + "」，请检查后重试，或去掉日期描述按今天记录");
            }
            java.time.LocalDate today = java.time.LocalDate.now();
            if (target.isAfter(today))
            {
                return error("补录日期不能是未来（" + target.getYear() + "年" + target.getMonthValue() + "月"
                        + target.getDayOfMonth() + "日）。如需记录今天的工作，请去掉日期描述后重新发送");
            }
            if (target.getYear() < today.getYear())
            {
                return error("只能补录今年（" + today.getYear() + "年）的记录，" + target.getYear()
                        + "年的工作无法补录");
            }
        }

        if (dryRun)
        {
            Map<String, Object> preview = new HashMap<>();
            preview.put("eventType", parsed.get("event_type"));
            preview.put("studentName", parsed.get("student_name"));
            preview.put("location", parsed.get("location"));
            preview.put("detail", parsed.get("detail"));
            preview.put("reply", parsed.get("reply"));
            preview.put("recordDate", targetDate == null ? null : targetDate);
            return success(preview);
        }

        VisitRecord rec = new VisitRecord();
        rec.setEventType(parsed.get("event_type"));
        rec.setStudentName(parsed.get("student_name"));
        rec.setLocation(parsed.get("location"));
        rec.setDetail(parsed.get("detail"));
        rec.setAiReply(parsed.get("reply"));
        rec.setRawText(parsed.get("raw_text"));
        if (targetDate != null && !targetDate.isBlank())
        {
            try { rec.setRecordDate(java.sql.Date.valueOf(targetDate)); } catch (Exception ignored) {}
        }
        visitRecordService.insertVisitRecord(rec);

        // 图片：存盘 + 写 visit_photo 表（recordId 已回填）
        if (files != null && files.length > 0)
        {
            List<byte[]> images = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (MultipartFile f : files)
            {
                if (f == null || f.isEmpty())
                {
                    continue;
                }
                try
                {
                    images.add(f.getBytes());
                    names.add(f.getOriginalFilename());
                }
                catch (Exception ignored) { }
            }
            if (!images.isEmpty())
            {
                rec.setPhotos(visitRecordService.insertPhotos(rec.getRecordId(), images, names));
            }
        }
        return success(rec);
    }

    /**
     * 语音录入：wav → 腾讯云 ASR → AI 识别 → 落库
     */
    @PreAuthorize("@ss.hasPermi('visit:record:add')")
    @PostMapping("/voice")
    public AjaxResult voice(@RequestParam("audio") MultipartFile audio)
    {
        byte[] data;
        try
        {
            data = audio.getBytes();
        }
        catch (Exception e)
        {
            return error("录音读取失败，请重新录音");
        }
        if (data.length == 0)
        {
            return error("录音为空，请重新录音");
        }

        String text = asrService.transcribeWav(data);
        if (text == null || text.isBlank())
        {
            return error("没听清，请靠近麦克风再说一次");
        }

        String audioName = storageService.saveAudio(data);
        Map<String, String> parsed = aiService.parseIntent(text);

        VisitRecord rec = new VisitRecord();
        rec.setEventType(parsed.get("event_type"));
        rec.setStudentName(parsed.get("student_name"));
        rec.setLocation(parsed.get("location"));
        rec.setDetail(parsed.get("detail"));
        rec.setAiReply(parsed.get("reply"));
        rec.setRawText(parsed.get("raw_text"));
        rec.setAudioPath(audioName);
        String aiDate = parsed.get("record_date");
        if (aiDate != null && !aiDate.isBlank())
        {
            try { rec.setRecordDate(java.sql.Date.valueOf(aiDate)); } catch (Exception ignored) {}
        }
        visitRecordService.insertVisitRecord(rec);

        Map<String, Object> result = new HashMap<>();
        result.put("text", text);
        result.put("record", rec);
        return success(result);
    }

    /**
     * 月度统计（12 行，四级数据权限）
     */
    @PreAuthorize("@ss.hasPermi('visit:record:list')")
    @GetMapping("/stats")
    public AjaxResult stats(VisitRecord record)
    {
        return success(visitRecordService.monthlyStats(record));
    }

    /**
     * 获取记录详情
     */
    @PreAuthorize("@ss.hasPermi('visit:record:query')")
    @GetMapping("/{recordId}")
    public AjaxResult getInfo(@PathVariable Long recordId)
    {
        return success(visitRecordService.selectVisitRecordById(recordId));
    }

    /**
     * 新增记录（AI 识别 + 补录日期）
     */
    @PreAuthorize("@ss.hasPermi('visit:record:add')")
    @Log(title = "走访记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody VisitRecord record)
    {
        return toAjax(visitRecordService.insertVisitRecord(record));
    }

    /**
     * 修改记录
     */
    @PreAuthorize("@ss.hasPermi('visit:record:edit')")
    @Log(title = "走访记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody VisitRecord record)
    {
        return toAjax(visitRecordService.updateVisitRecord(record));
    }

    /**
     * 软删除（进回收站）
     */
    @PreAuthorize("@ss.hasPermi('visit:record:remove')")
    @Log(title = "走访记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordId}")
    public AjaxResult remove(@PathVariable Long recordId)
    {
        return toAjax(visitRecordService.softDeleteVisitRecord(recordId));
    }

    /**
     * 恢复
     */
    @PreAuthorize("@ss.hasPermi('visit:record:edit')")
    @PostMapping("/restore/{recordId}")
    public AjaxResult restore(@PathVariable Long recordId)
    {
        return toAjax(visitRecordService.restoreVisitRecord(recordId));
    }

    /**
     * 彻底删除
     */
    @PreAuthorize("@ss.hasPermi('visit:record:remove')")
    @DeleteMapping("/purge/{recordId}")
    public AjaxResult purge(@PathVariable Long recordId)
    {
        return toAjax(visitRecordService.deleteVisitRecordById(recordId));
    }

    /**
     * 三级目录（学校 → 学院 → 辅导员），供领导下钻筛选。
     * counselor 角色返回空列表。
     */
    @PreAuthorize("@ss.hasPermi('visit:record:list')")
    @GetMapping("/org-tree")
    public AjaxResult orgTree()
    {
        return success(visitOrgService.orgTree());
    }
}
