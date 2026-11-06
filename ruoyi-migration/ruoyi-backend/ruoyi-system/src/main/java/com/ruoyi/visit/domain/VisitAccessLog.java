package com.ruoyi.visit.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 敏感记录访问审计（issue #13）
 */
public class VisitAccessLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long recordId;
    private String recordType;
    private Long viewerId;
    private String viewerName;
    private Date accessTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public Long getViewerId() { return viewerId; }
    public void setViewerId(Long viewerId) { this.viewerId = viewerId; }
    public String getViewerName() { return viewerName; }
    public void setViewerName(String viewerName) { this.viewerName = viewerName; }
    public Date getAccessTime() { return accessTime; }
    public void setAccessTime(Date accessTime) { this.accessTime = accessTime; }
}
