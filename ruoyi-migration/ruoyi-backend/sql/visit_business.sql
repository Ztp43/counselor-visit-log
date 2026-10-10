-- 辅导员工作日志系统 —— 业务表（迁移到若依 ry-vue 库）
-- 命名对齐若依规范（单数、下划线、create_time），字段兼容若依数据权限。
-- 说明：user_id 指向 sys_user.user_id；dept_id 冗余指向 sys_dept.dept_id，
--       供若依 @DataScope 做部门数据权限过滤（学院领导看本学院）。

-- ============ 走访记录表 ============
CREATE TABLE IF NOT EXISTS visit_record (
    record_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id       BIGINT       NOT NULL COMMENT '辅导员ID（sys_user.user_id）',
    dept_id       BIGINT       NULL COMMENT '所属学院ID（sys_dept.dept_id，冗余便于数据权限）',
    event_type    VARCHAR(32)  NOT NULL COMMENT '工作类型（九类之一）',
    location      VARCHAR(256) NULL COMMENT '地点',
    student_name  VARCHAR(128) NULL COMMENT '涉及学生姓名（多人用、分隔）',
    detail        TEXT         NULL COMMENT '台账归档摘要',
    ai_reply      TEXT         NULL COMMENT 'AI确认回复',
    raw_text      TEXT         NULL COMMENT '原始文字/语音转写',
    audio_path    VARCHAR(256) NULL COMMENT '录音文件路径',
    record_date   DATE         NULL COMMENT '补录日期（null=当天）',
    deleted_at    DATETIME     NULL COMMENT '软删除时间（null=在库）',
    create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user (user_id),
    KEY idx_dept (dept_id),
    KEY idx_record_date (record_date),
    KEY idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='辅导员走访工作记录';

-- ============ 走访照片表 ============
CREATE TABLE IF NOT EXISTS visit_photo (
    photo_id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '照片ID',
    record_id   BIGINT       NOT NULL COMMENT '所属记录ID',
    file_path   VARCHAR(256) NOT NULL COMMENT '文件路径',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_record (record_id),
    CONSTRAINT fk_photo_record FOREIGN KEY (record_id) REFERENCES visit_record(record_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='走访记录照片';
