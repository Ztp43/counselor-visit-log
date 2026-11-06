package com.ruoyi.visit.service.impl;

import java.util.Set;

/**
 * 敏感信息保护（issue #13）
 *
 * 敏感事件类型：talk（谈心）、crisis（危机处置）——这两类涉及学生心理/家庭困难/危机隐私。
 * 敏感字段：detail、rawText、studentName、aiReply。
 */
public final class SensitiveGuard
{
    private SensitiveGuard() {}

    /** 敏感事件类型（可扩展） */
    private static final Set<String> SENSITIVE_TYPES = Set.of("talk", "crisis");

    /** 脱敏占位符 */
    public static final String MASK = "***";

    /** 判断某事件类型是否敏感 */
    public static boolean isSensitive(String eventType)
    {
        return eventType != null && SENSITIVE_TYPES.contains(eventType);
    }

    /** 脱敏一个字段：非空则替换为 ***，空保持空 */
    public static String mask(String value)
    {
        return (value == null || value.isBlank()) ? value : MASK;
    }
}
