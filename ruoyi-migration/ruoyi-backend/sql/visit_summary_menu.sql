-- 总结报告菜单（领导/辅导员都可见）
-- 菜单 ID 用 2200 段，避开现有业务菜单。

DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu WHERE perms = 'visit:report:summary' OR menu_name = '总结报告'
);
DELETE FROM sys_menu WHERE perms = 'visit:report:summary' OR menu_name = '总结报告';

-- 一级菜单「总结报告」
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2201, '总结报告', 0, 5, 'summary', 'visit/summary/index', 1, 0, 'C', '0', '0', 'visit:report:summary', 'chart', 'admin', NOW());

-- 角色绑定：辅导员 + 学院领导 + 学校领导（管理员走超管默认全权限）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(12, 2201),  -- 辅导员 counselor
(11, 2201),  -- 学院领导 college_leader
(10, 2201);  -- 学校领导 school_leader
