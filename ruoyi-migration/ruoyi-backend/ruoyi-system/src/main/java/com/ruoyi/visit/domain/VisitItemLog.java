package com.ruoyi.visit.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 事项跟进时间线（issue #11 第三阶段）
 */
public class VisitItemLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 所属事项ID */
    private Long itemId;

    /** 操作类型：create创建/follow跟进/status状态变更/remark备注 */
    private String action;

    /** 跟进内容/说明 */
    private String content;

    /** 变更前状态（仅 status 操作有值） */
    private String fromStatus;

    /** 变更后状态（仅 status 操作有值） */
    private String toStatus;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
}
