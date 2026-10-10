package com.ruoyi.visit.service;

import java.util.List;
import java.util.Map;

/**
 * 走访三级目录（学校 → 学院 → 辅导员）。
 */
public interface IVisitOrgService
{
    /**
     * 三级目录树：按当前用户角色返回可见的学院→辅导员。
     *   admin / school_leader：全校所有辅导员
     *   college_leader：本学院辅导员
     *   counselor：空列表（前端不展示筛选）
     *
     * 返回 [{ collegeId, collegeName, counselors: [{id,name,username,role}] }]
     */
    List<Map<String, Object>> orgTree();
}
