package com.ruoyi.visit.domain;

/**
 * 三级目录里的学院节点（查询结果）。
 */
public class VisitCollege
{
    private Long collegeId;
    private String collegeName;

    public Long getCollegeId() { return collegeId; }
    public void setCollegeId(Long collegeId) { this.collegeId = collegeId; }
    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }
}
