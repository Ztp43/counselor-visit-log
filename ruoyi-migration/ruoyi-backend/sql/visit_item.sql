-- 辅导员工作日志系统 —— 事项/跟进闭环表（issue #11 第一阶段：事项台账 CRUD）
-- 命名对齐若依规范，字段兼容若依数据权限（@DataScope）。
-- 状态机：pending(待处理) → following(跟进中) → closed(已结案)

-- ============ 事项表 ============
CREATE TABLE IF NOT EXISTS visit_item (
    item_id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '事项ID',
    item_no        VARCHAR(32)  NOT NULL COMMENT '事项编号（XS-年份-序号，如 XS-2026-001）',
    title          VARCHAR(256) NOT NULL COMMENT '事项标题',
    category       VARCHAR(32)  NOT NULL COMMENT '问题类别（复用九类工作类型）',
    student_name   VARCHAR(128) NULL COMMENT '涉及学生姓名',
    description    TEXT         NULL COMMENT '事项详情',
    status         VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT '状态：pending待处理/following跟进中/closed已结案',
    assignee_id    BIGINT       NULL COMMENT '责任人ID（sys_user.user_id）',
    due_date       DATE         NULL COMMENT '截止日期',
    dept_id        BIGINT       NULL COMMENT '所属学院ID（sys_dept.dept_id，数据权限）',
    user_id        BIGINT       NOT NULL COMMENT '创建人ID（sys_user.user_id）',
    deleted_at     DATETIME     NULL COMMENT '软删除时间（null=在库）',
    create_by      VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_item_no (item_no),
    KEY idx_status (status),
    KEY idx_assignee (assignee_id),
    KEY idx_dept (dept_id),
    KEY idx_user (user_id),
    KEY idx_due_date (due_date),
    KEY idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='走访工作跟进事项';
