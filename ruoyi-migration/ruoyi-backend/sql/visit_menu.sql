-- 走访日志菜单 + 权限字符 + 四级角色绑定（迁移到若依 sys_menu / sys_role_menu）
-- 幂等：先删旧 visit 菜单（含子），再重建。
-- 菜单 ID 段用 2000+，避开若依默认 1~1099 段。
-- ⚠️ 2026-10-11：移除「走访日志」目录层，三个业务菜单直接作一级菜单。

-- ============ 1. 清旧（按菜单名/perms 兜底删除，防止残留） ============
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu WHERE menu_name IN ('走访日志','今日记录','工作日志','月度统计')
      OR perms LIKE 'visit:record:%'
);
DELETE FROM sys_menu WHERE menu_name IN ('走访日志','今日记录','工作日志','月度统计')
    OR perms LIKE 'visit:record:%';

-- ============ 2. 三个一级业务菜单（无目录层） ============
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2001, '今日记录', 0, 1, 'today',  'visit/today/index',  1, 0, 'C', '0', '0', 'visit:record:list', 'edit',  'admin', NOW()),
(2006, '工作日志', 0, 2, 'record', 'visit/record/index', 1, 0, 'C', '0', '0', 'visit:record:list', 'list',  'admin', NOW()),
(2007, '月度统计', 0, 3, 'stats',  'visit/stats/index',  1, 0, 'C', '0', '0', 'visit:record:list', 'chart', 'admin', NOW());

-- ============ 3. 按钮权限（挂在「今日记录」下） ============
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2002, '走访记录新增', 2001, 1, '', '', 1, 0, 'F', '0', '0', 'visit:record:add',    '#', 'admin', NOW()),
(2003, '走访记录修改', 2001, 2, '', '', 1, 0, 'F', '0', '0', 'visit:record:edit',   '#', 'admin', NOW()),
(2004, '走访记录删除', 2001, 3, '', '', 1, 0, 'F', '0', '0', 'visit:record:remove', '#', 'admin', NOW()),
(2005, '走访记录查询', 2001, 4, '', '', 1, 0, 'F', '0', '0', 'visit:record:query',  '#', 'admin', NOW());

-- ============ 4. 角色绑定 ============
-- 辅导员 counselor(role_id=12)：全部权限（三个菜单 + 增删改查）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(12, 2001), (12, 2006), (12, 2007), (12, 2002), (12, 2003), (12, 2004), (12, 2005);

-- 学院领导 college_leader(role_id=11)：只读（三个菜单 + 查询）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(11, 2001), (11, 2006), (11, 2007), (11, 2005);

-- 学校领导 school_leader(role_id=10)：只读（三个菜单 + 查询）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(10, 2001), (10, 2006), (10, 2007), (10, 2005);

-- ============ 5. 验证 ============
SELECT menu_id, menu_name, parent_id, menu_type, perms, component FROM sys_menu WHERE menu_id >= 2000 ORDER BY menu_id;
SELECT r.role_key, m.menu_name FROM sys_role_menu rm
    JOIN sys_role r ON rm.role_id = r.role_id
    JOIN sys_menu m ON rm.menu_id = m.menu_id
    WHERE rm.menu_id >= 2000 ORDER BY r.role_key, m.menu_id;
