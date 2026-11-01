-- 事项台账菜单 + 权限字符 + 四级角色绑定（issue #11）
-- 菜单 ID 用 2100 段，避开现有 2000 段业务菜单。
-- 幂等：先删旧 visit:item 权限 + 菜单，再重建。

-- ============ 1. 清旧 ============
DELETE FROM sys_role_menu WHERE menu_id IN (
    SELECT menu_id FROM sys_menu WHERE perms LIKE 'visit:item:%' OR menu_name = '事项台账'
);
DELETE FROM sys_menu WHERE perms LIKE 'visit:item:%' OR menu_name = '事项台账';

-- ============ 2. 一级菜单「事项台账」 ============
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2101, '事项台账', 0, 4, 'item', 'visit/item/index', 1, 0, 'C', '0', '0', 'visit:item:list', 'documentation', 'admin', NOW());

-- ============ 3. 按钮权限 ============
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2102, '事项新增', 2101, 1, '', '', 1, 0, 'F', '0', '0', 'visit:item:add',    '#', 'admin', NOW()),
(2103, '事项修改', 2101, 2, '', '', 1, 0, 'F', '0', '0', 'visit:item:edit',   '#', 'admin', NOW()),
(2104, '事项删除', 2101, 3, '', '', 1, 0, 'F', '0', '0', 'visit:item:remove', '#', 'admin', NOW()),
(2105, '事项查询', 2101, 4, '', '', 1, 0, 'F', '0', '0', 'visit:item:query',  '#', 'admin', NOW());

-- ============ 4. 角色绑定 ============
-- 辅导员 counselor(role_id=12)：全部（菜单 + 增删改查）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(12, 2101), (12, 2102), (12, 2103), (12, 2104), (12, 2105);

-- 学院领导 college_leader(role_id=11)：只读（菜单 + 查询）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(11, 2101), (11, 2105);

-- 学校领导 school_leader(role_id=10)：只读（菜单 + 查询）
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(10, 2101), (10, 2105);
