package com.ruoyi.visit.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 写操作归属校验（issue #24 水平越权修复）
 *
 * 规则（与 issue 预期一致）：
 *   1. 管理员：放行
 *   2. 学校领导 / 学院领导：禁止编辑（他们本就只读，403）
 *   3. 辅导员：仅允许操作 user_id == 本人的记录，否则 403
 *
 * 列表查询/统计/导出走 @DataScope 数据权限，不在此校验范围。
 */
public final class OwnershipGuard
{
    private OwnershipGuard() {}

    /**
     * 校验当前用户能否对「归属 user_id = ownerUserId 的记录」做写操作。
     * 不通过抛 ServiceException(403)。
     */
    public static void check(Long ownerUserId)
    {
        // 管理员放行
        if (SecurityUtils.isAdmin())
        {
            return;
        }
        // 两级领导：禁止编辑
        if (SecurityUtils.hasRole("school_leader") || SecurityUtils.hasRole("college_leader"))
        {
            throw new ServiceException("无权限：学校/学院领导仅可查看，不能编辑或删除记录", 403);
        }
        // 辅导员：仅本人
        Long currentUserId = SecurityUtils.getUserId();
        if (ownerUserId == null || !ownerUserId.equals(currentUserId))
        {
            throw new ServiceException("无权限：只能操作本人创建的记录", 403);
        }
    }
}
