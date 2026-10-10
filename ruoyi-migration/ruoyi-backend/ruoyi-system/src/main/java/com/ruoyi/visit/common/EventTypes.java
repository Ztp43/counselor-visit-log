package com.ruoyi.visit.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作类型（9 类）—— 唯一的权威定义。
 *
 * 顺序固定：图表、报表、统计都要按这个顺序展示。
 * 迁移自现网 EventTypes（与原系统 EVENT_TYPE_ORDER / EVENT_TYPE_CN 完全对应）。
 */
public final class EventTypes
{
    private EventTypes() {}

    /** 按展示顺序排列的 类型 -> 中文名 */
    public static final Map<String, String> CN = new LinkedHashMap<>();

    static
    {
        CN.put("dorm_check", "查寝点名");
        CN.put("hygiene", "安全卫生");
        CN.put("talk", "谈心谈话");
        CN.put("class_check", "查课听课");
        CN.put("meeting", "班会活动");
        CN.put("visit_help", "走访帮扶");
        CN.put("crisis", "危机处置");
        CN.put("admin", "事务工作");
        CN.put("other", "其他");
    }

    public static boolean isValid(String type)
    {
        return type != null && CN.containsKey(type);
    }

    public static String cn(String type)
    {
        return CN.getOrDefault(type, "其他");
    }
}
