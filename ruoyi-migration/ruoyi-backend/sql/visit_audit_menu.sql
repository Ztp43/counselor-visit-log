-- 敏感记录访问审计日志菜单（issue #37，仅管理员可见）

DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu WHERE perms = 'visit:audit:list' OR menu_name = '审计日志'
);
DELETE FROM sys_menu WHERE perms = 'visit:audit:list' OR menu_name = '审计日志';

-- 一级菜单「审计日志」（2200 段）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2202, '审计日志', 0, 6, 'audit', 'visit/audit/index', 1, 0, 'C', '0', '0', 'visit:audit:list', 'search', 'admin', NOW());

-- 仅绑定管理员角色（role_id=1 超级管理员；超管默认全权限，这里显式绑定保证菜单可见）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 2202);
