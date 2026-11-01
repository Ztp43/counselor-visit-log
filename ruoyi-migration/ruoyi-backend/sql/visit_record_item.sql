-- 事项关联（issue #11 第二阶段）：visit_record 加 item_id，关联 visit_item.item_id
-- 可空：普通日志不挂事项，值为 NULL
ALTER TABLE visit_record ADD COLUMN item_id BIGINT NULL COMMENT '关联事项ID（visit_item.item_id，null=普通日志）' AFTER audio_path;
ALTER TABLE visit_record ADD KEY idx_item (item_id);
