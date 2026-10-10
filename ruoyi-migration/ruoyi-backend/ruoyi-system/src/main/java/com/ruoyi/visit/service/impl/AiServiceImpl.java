package com.ruoyi.visit.service.impl;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.visit.config.VisitConfig;
import com.ruoyi.visit.service.IAiService;

/**
 * DeepSeek 意图识别（迁移自旧系统 AiServiceImpl）。
 * - 有 Key 且调用成功 → 用大模型结果；
 * - 没 Key 或调用失败 → 走关键词兜底，保证录入不中断。
 * - record_date 补录日期：把「今天是 X年X月X日 星期X」注入提示词，AI 才能换算相对日期。
 */
@Service
public class AiServiceImpl implements IAiService
{
    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final VisitConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private boolean warnedNoKey = false;

    public AiServiceImpl(VisitConfig config, ObjectMapper objectMapper)
    {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    private static final String SYSTEM_PROMPT = """
            你是高校辅导员的工作日志助手。辅导员会用口语化大白话（有时来自语音转写，可能带错别字）告诉你刚做了什么。

            你的任务：抽取结构化字段，并给辅导员一句自然的确认回复。严格按以下 JSON 格式输出（不要输出任何多余文字）：

            {
              "event_type": "九类之一",
              "student_name": "涉及的学生姓名",
              "location": "主要地点",
              "detail": "台账归档摘要",
              "reply": "给辅导员的确认回复",
              "record_date": "补录日期 yyyy-MM-dd，或 null"
            }

            【分类规则】event_type 必须从以下九类中选一个：
            - dorm_check 查寝点名：去宿舍查寝、晚点名、归寝检查、核查学生在不在宿舍、夜不归宿
            - hygiene 安全卫生：检查宿舍/公共区域卫生、违禁电器、消防隐患、用电安全、督促整改
            - talk 谈心谈话：与学生个别面对面交流、思想动态、心理疏导、开导、批评教育、情感/人际/学业/就业话题的私下交流
            - class_check 查课听课：随堂听课、查课堂出勤、课堂纪律、学风检查、巡视教室
            - meeting 班会活动：面向学生开展的集体活动，如主题班会、年级大会、晚点名、班团会、学生干部例会、开学/毕业典礼
            - visit_help 走访帮扶：上门慰问困难生、实地找学生（如去网吧/校外找人）、解决生活/后勤问题、送温暖、困难帮扶
            - crisis 危机处置：突发事件处理、违纪处分、心理危机干预、安全稳定事件、紧急联系家长、护送就医
            - admin 事务工作：奖助贷评优、材料收集、信息统计、军训组织、档案整理、常规通知传达、行政协调、开会
            - other 其他：确实无法归入上述八类的

            【"开会"的区分（重要）】
            - 明确提到"班会/给学生开会/年级会/晚点名/班委会/支部会" → meeting 班会活动
            - 提到"学院开会/学工开会/部门会议/领导开会"，或只说"开会/去开会"没说明对象 → admin 事务工作
            - 简单说"下午要开会"这类无任何学生信号的，一律归 admin 事务工作

            【student_name 规则（重要）】
            - 只填原话里明确出现的学生姓名，多人用「、」分隔
            - 语音转写的姓名错别字要按上下文改正
            - 只说了人数或泛指，没有具体姓名时，一律填空字符串，绝对不要编造姓名
            - 不确定是不是人名（如只出现姓氏「小李」「王同学」）时，可以按原话填「小李」「王同学」

            【location 规则】
            - 只填一个最主要的地点，用规范简写，禁止把多个地点拼接
            - 没有明确地点就填空字符串

            【detail 规则（重要）】
            - 写成台账归档摘要，禁止照抄原文，20~60 字，第三人称
            - 语音转写的错别字要按上下文改正

            【reply 规则】
            - 自然、简洁、有温度，1~2 句，不超过 60 字，简体中文
            - 句式要有变化

            【record_date 规则（补录日期，重要）】
            - 默认输出 null，表示"就是今天，无需补录"
            - 只有当原话**明确提到过去某个具体日期**时才填，格式 yyyy-MM-dd
            - 常见表述的换算：昨天、前天、大前天；上周一~上周日；本周一~本周日（若已过去）；
              X月X日（若今年已过）；X号（本月已过的日子）；「上周五」「上个月X号」等
            - 只识别**过去**的日期；提到未来日期（如下周五、下月）一律 null
            - 模糊表述（"前几天""最近""前段时间""上周"这种没有具体到某一天的）一律 null，不要猜测
            - 拿不准是否该补录、或算不出确切日期时，一律输出 null
            """;

    /** 把「今天是 X 年 X 月 X 日，星期 X」注入提示词，否则 DeepSeek 不知道今天，无法换算相对日期。 */
    private String systemPromptWithToday()
    {
        LocalDate today = LocalDate.now();
        String[] cnWeek = {"日", "一", "二", "三", "四", "五", "六"};
        String week = cnWeek[today.getDayOfWeek().getValue() % 7];
        return SYSTEM_PROMPT + "\n\n【今天日期】今天是 " + today.getYear() + " 年 "
                + today.getMonthValue() + " 月 " + today.getDayOfMonth() + " 日，星期" + week
                + "。换算\"上周五/昨天/本周X\"等相对时间时，以这个日期为准。";
    }

    @Override
    public Map<String, String> parseIntent(String text)
    {
        String apiKey = config.getDeepseek().getApiKey();
        if (apiKey == null || apiKey.isBlank())
        {
            if (!warnedNoKey)
            {
                log.warn("未配置 visit.deepseek.api-key，意图识别将一直走关键词兜底规则");
                warnedNoKey = true;
            }
            return fallback(text);
        }

        try
        {
            String url = config.getDeepseek().getBaseUrl() + "/chat/completions";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "deepseek-chat");
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPromptWithToday()),
                    Map.of("role", "user", "content", text)));
            body.put("temperature", 0.3);
            body.put("response_format", Map.of("type", "json_object"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, req, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode data = objectMapper.readTree(content);
            return normalize(data, text);
        }
        catch (Exception e)
        {
            log.warn("DeepSeek 意图识别失败，已改用关键词兜底：{}: {}", e.getClass().getSimpleName(), e.getMessage());
            return fallback(text);
        }
    }

    private Map<String, String> normalize(JsonNode data, String raw)
    {
        String et = data.path("event_type").asText("other");
        if (!isValidEventType(et))
        {
            et = "other";
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("event_type", et);
        result.put("student_name", data.path("student_name").asText("").strip());
        result.put("location", data.path("location").asText("").strip());
        result.put("detail", data.path("detail").asText(raw).strip());
        result.put("reply", data.path("reply").asText("").strip());
        result.put("record_date", data.path("record_date").asText("").strip());
        result.put("raw_text", raw);
        return result;
    }

    private Map<String, String> fallback(String text)
    {
        String t = text == null ? "" : text;
        String et;
        if (containsAny(t, "查寝", "查宿舍", "晚点名", "归寝", "寝室", "宿舍", "夜不归宿")) {
            et = "dorm_check";
        } else if (containsAny(t, "卫生", "打扫", "清洁", "违禁电器", "消防", "用电")) {
            et = "hygiene";
        } else if (containsAny(t, "谈心", "谈话", "聊天", "开导", "辅导", "交流", "心理")) {
            et = "talk";
        } else if (containsAny(t, "听课", "查课", "课堂", "出勤", "考勤", "学风")) {
            et = "class_check";
        } else if (containsAny(t, "班会", "班委会", "支部会", "年级会", "典礼", "晚点评", "主题班")) {
            et = "meeting";
        } else if (containsAny(t, "慰问", "走访", "帮扶", "困难生", "送温暖", "找学生")) {
            et = "visit_help";
        } else if (containsAny(t, "危机", "突发", "违纪", "处分", "护送", "就医", "家长")) {
            et = "crisis";
        } else if (containsAny(t, "奖助", "评优", "材料", "统计", "军训", "档案", "通知", "开会", "会议")) {
            et = "admin";
        } else {
            et = "other";
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("event_type", et);
        result.put("student_name", "");
        result.put("location", "");
        result.put("detail", t);
        result.put("reply", "已记下这条" + eventTypeCn(et) + "记录（AI 助手暂时没连上，先按关键词简单归类了）。");
        result.put("record_date", "");
        result.put("raw_text", t);
        return result;
    }

    private boolean containsAny(String text, String... keywords)
    {
        for (String k : keywords)
        {
            if (text.contains(k))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isValidEventType(String type)
    {
        return EVENT_TYPES.contains(type);
    }

    private String eventTypeCn(String type)
    {
        switch (type)
        {
            case "dorm_check": return "查寝点名";
            case "hygiene": return "安全卫生";
            case "talk": return "谈心谈话";
            case "class_check": return "查课听课";
            case "meeting": return "班会活动";
            case "visit_help": return "走访帮扶";
            case "crisis": return "危机处置";
            case "admin": return "事务工作";
            default: return "其他";
        }
    }

    private static final List<String> EVENT_TYPES = List.of(
        "dorm_check", "hygiene", "talk", "class_check", "meeting",
        "visit_help", "crisis", "admin", "other");
}
