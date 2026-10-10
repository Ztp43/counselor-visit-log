package com.ruoyi.visit.mapper;

import java.util.List;
import com.ruoyi.visit.domain.VisitCollege;
import com.ruoyi.visit.domain.VisitCounselor;

/**
 * 走访三级目录（学校 → 学院 → 辅导员）数据层。
 * 只列 counselor 角色用户（领导/管理员不混入）。
 */
public interface VisitOrgMapper
{
    /** 查所有辅导员（role_key='counselor'，未删除） */
    List<VisitCounselor> selectCounselorUsers();

    /** 查 21 个学院（sys_dept 里 parent_id=200 的部门） */
    List<VisitCollege> selectColleges();
}
