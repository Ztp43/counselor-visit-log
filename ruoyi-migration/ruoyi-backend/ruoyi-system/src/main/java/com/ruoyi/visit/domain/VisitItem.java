package com.ruoyi.visit.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 走访工作跟进事项（issue #11 事项/跟进闭环体系）
 *
 * 状态机：pending(待处理) → following(跟进中) → closed(已结案)
 */
public class VisitItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 事项ID */
    private Long itemId;

    /** 事项编号（XS-年份-序号，如 XS-2026-001） */
    private String itemNo;

    /** 事项标题 */
    private String title;

    /** 问题类别（复用九类工作类型：dorm_check/hygiene/talk/...） */
    private String category;

    /** 涉及学生姓名 */
    private String studentName;

    /** 事项详情 */
    private String description;

    /** 状态：pending待处理 / following跟进中 / closed已结案 */
    private String status;

    /** 责任人ID（sys_user.user_id） */
    private Long assigneeId;

    /** 截止日期 */
    private Date dueDate;

    /** 所属学院ID（数据权限） */
    private Long deptId;

    /** 创建人ID */
    private Long userId;

    /** 软删除时间 */
    private Date deletedAt;

    /** 责任人姓名（联表回填，非表字段） */
    private String assigneeName;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemNo() { return itemNo; }
    public void setItemNo(String itemNo) { this.itemNo = itemNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
}
