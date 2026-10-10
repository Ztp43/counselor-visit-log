package com.ruoyi.visit.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.visit.domain.VisitCollege;
import com.ruoyi.visit.domain.VisitCounselor;
import com.ruoyi.visit.mapper.VisitOrgMapper;
import com.ruoyi.visit.service.IVisitOrgService;

/**
 * 走访三级目录（学校 → 学院 → 辅导员）。
 * 迁移自现网 UserServiceImpl.orgTree。
 */
@Service
public class VisitOrgServiceImpl implements IVisitOrgService
{
    @Autowired
    private VisitOrgMapper visitOrgMapper;

    @Override
    public List<Map<String, Object>> orgTree()
    {
        // 当前用户角色判断（若依 SecurityUtils）
        // 注意：若依 hasRole 会把超级管理员 admin 视为拥有所有角色，
        // 所以要先判断 isAdmin，学院领导判断要排除 admin。
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isAdminOrSchool = isAdmin || SecurityUtils.hasRole("school_leader");
        boolean isCollegeLeader = !isAdmin && SecurityUtils.hasRole("college_leader");
        if (!isAdminOrSchool && !isCollegeLeader)
        {
            // 辅导员：不展示树（前端隐藏筛选）
            return List.of();
        }

        List<VisitCounselor> users = visitOrgMapper.selectCounselorUsers();
        List<VisitCollege> colleges = visitOrgMapper.selectColleges();

        // 学院领导：只看本学院
        Long myDeptId = SecurityUtils.getDeptId();
        if (isCollegeLeader && myDeptId != null)
        {
            users.removeIf(u -> u.getDeptId() == null || !u.getDeptId().equals(myDeptId));
        }

        // 学院 -> 辅导员列表
        Map<Long, List<Map<String, Object>>> byCollege = new LinkedHashMap<>();
        List<Map<String, Object>> unassigned = new ArrayList<>();
        for (VisitCounselor u : users)
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("username", u.getUsername());
            item.put("role", "counselor");
            if (u.getDeptId() != null)
            {
                byCollege.computeIfAbsent(u.getDeptId(), k -> new ArrayList<>()).add(item);
            }
            else
            {
                unassigned.add(item);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (VisitCollege c : colleges)
        {
            List<Map<String, Object>> cs = byCollege.get(c.getCollegeId());
            if (cs == null || cs.isEmpty())
            {
                continue;   // 没人的学院不显示
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("collegeId", c.getCollegeId());
            node.put("collegeName", c.getCollegeName());
            node.put("counselors", cs);
            result.add(node);
        }
        if (!unassigned.isEmpty())
        {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("collegeId", 0L);
            node.put("collegeName", "未分配学院");
            node.put("counselors", unassigned);
            result.add(node);
        }
        return result;
    }
}
