package com.ruoyi.visit.domain;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 辅导员走访工作记录（迁移到若依）。
 *
 * 数据权限字段：
 *   user_id 指向 sys_user.user_id（辅导员）
 *   dept_id 指向 sys_dept.dept_id（所属学院，冗余，供 @DataScope 部门权限）
 *
 * @author ruoyi
 */
public class VisitRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 辅导员ID（sys_user.user_id） */
    private Long userId;

    /** 所属学院ID（sys_dept.dept_id） */
    private Long deptId;

    /** 工作类型（九类之一：dorm_check/hygiene/talk/class_check/meeting/visit_help/crisis/admin/other） */
    private String eventType;

    /** 地点 */
    private String location;

    /** 涉及学生姓名（多人用、分隔） */
    private String studentName;

    /** 台账归档摘要 */
    private String detail;

    /** AI确认回复 */
    private String aiReply;

    /** 原始文字/语音转写 */
    private String rawText;

    /** 录音文件路径 */
    private String audioPath;

    /** 补录日期（null=当天） */
    private Date recordDate;

    /** 软删除时间（null=在库） */
    private Date deletedAt;

    /** 辅导员姓名（联表展示用，非表字段） */
    private String recorderName;

    /** 照片 URL 列表（联表展示用，非表字段，/profile/xxx.jpg） */
    private List<String> photos;

    /** 关键词（非表字段，搜索用：student_name/detail/raw_text/location 四列 or 包含匹配） */
    private String keyword;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getAiReply() { return aiReply; }
    public void setAiReply(String aiReply) { this.aiReply = aiReply; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public Date getRecordDate() { return recordDate; }
    public void setRecordDate(Date recordDate) { this.recordDate = recordDate; }
    public Date getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
    public String getRecorderName() { return recorderName; }
    public void setRecorderName(String recorderName) { this.recorderName = recorderName; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
