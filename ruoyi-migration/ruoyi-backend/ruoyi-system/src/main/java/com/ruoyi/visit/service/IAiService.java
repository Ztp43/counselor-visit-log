package com.ruoyi.visit.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 意图识别（DeepSeek）。迁移自旧系统 AiService。
 */
public interface IAiService
{
    /** 解析一句口语化描述，返回 event_type/student_name/location/detail/reply/record_date/raw_text */
    Map<String, String> parseIntent(String text);

    /** 根据量化数据生成一段工作总结文字（用于领导总结报告，issue 总结报告功能） */
    String generateSummary(String dataJson);
}
