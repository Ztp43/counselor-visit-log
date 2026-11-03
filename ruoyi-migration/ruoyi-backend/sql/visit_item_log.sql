-- 事项跟进时间线（issue #11 第三阶段）
-- 记录每个事项的完整跟进过程：创建、状态变更、手动跟进、备注等
CREATE TABLE IF NOT EXISTS visit_item_log (
    log_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    item_id     BIGINT       NOT NULL COMMENT '所属事项ID（visit_item.item_id）',
    action      VARCHAR(16)  NOT NULL COMMENT '操作类型：create创建/follow跟进/status状态变更/remark备注',
    content     TEXT         NULL COMMENT '跟进内容/说明',
    from_status VARCHAR(16)  NULL COMMENT '变更前状态（仅 status 操作有值）',
    to_status   VARCHAR(16)  NULL COMMENT '变更后状态（仅 status 操作有值）',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '操作人用户名',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_item (item_id),
    CONSTRAINT fk_itemlog_item FOREIGN KEY (item_id) REFERENCES visit_item(item_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='走访事项跟进时间线';
