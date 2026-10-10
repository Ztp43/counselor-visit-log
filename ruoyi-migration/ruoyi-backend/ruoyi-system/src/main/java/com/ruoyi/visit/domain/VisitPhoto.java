package com.ruoyi.visit.domain;

import java.util.Date;

/**
 * 走访记录照片（迁移到若依，对应 visit_photo 表）。
 *
 * @author ruoyi
 */
public class VisitPhoto
{
    private static final long serialVersionUID = 1L;

    /** 照片ID */
    private Long photoId;

    /** 所属记录ID */
    private Long recordId;

    /** 文件路径（相对 profile 目录的文件名，URL 为 /profile/xxx） */
    private String filePath;

    /** 创建时间 */
    private Date createTime;

    public Long getPhotoId() { return photoId; }
    public void setPhotoId(Long photoId) { this.photoId = photoId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
