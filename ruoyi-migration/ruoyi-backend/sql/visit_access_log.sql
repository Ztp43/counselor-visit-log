-- 敏感记录访问留痕审计（issue #13）
-- 记录谁在什么时间查看了哪条敏感记录
CREATE TABLE IF NOT EXISTS visit_access_log (
    log_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审计日志ID',
    record_id    BIGINT       NOT NULL COMMENT '被查看的记录ID（visit_record.record_id）',
    record_type  VARCHAR(32)  NOT NULL COMMENT '事件类型（talk/crisis 等）',
    viewer_id    BIGINT       NOT NULL COMMENT '查看人ID（sys_user.user_id）',
    viewer_name  VARCHAR(64)  DEFAULT '' COMMENT '查看人用户名',
    access_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    KEY idx_record (record_id),
    KEY idx_viewer (viewer_id),
    KEY idx_time (access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感记录访问审计日志';
