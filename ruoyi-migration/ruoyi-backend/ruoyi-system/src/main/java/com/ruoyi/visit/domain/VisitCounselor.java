package com.ruoyi.visit.domain;

/**
 * 三级目录里的辅导员节点（查询结果）。
 */
public class VisitCounselor
{
    private Long id;
    private String name;
    private String username;
    private Long deptId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
}
