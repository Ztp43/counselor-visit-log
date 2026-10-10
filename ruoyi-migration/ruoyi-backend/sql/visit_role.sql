-- 四级角色（迁移到若依 sys_role）
-- data_scope 取值：1=全部 2=自定 3=本部门 4=本部门及以下 5=仅本人
-- 现有：role_id=1 超级管理员(admin, data_scope=1)，role_id=2 普通角色(common, 演示用，删掉)

-- 1. 删演示角色 common
DELETE FROM sys_role WHERE role_key = 'common';

-- 2. 学校领导：data_scope=1(全部)——「看除管理员外全部」需要在业务查询里用自定义 SQL 排除 admin，角色层先给全部
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, del_flag, create_by, create_time)
VALUES (10, '学校领导', 'school_leader', 2, 1, '0', '0', 'admin', NOW());

-- 3. 学院领导：data_scope=2(自定)——配合 sys_role_dept 指定「本学院」，实际按用户 dept_id 过滤
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, del_flag, create_by, create_time)
VALUES (11, '学院领导', 'college_leader', 3, 2, '0', '0', 'admin', NOW());

-- 4. 辅导员：data_scope=5(仅本人)
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, del_flag, create_by, create_time)
VALUES (12, '辅导员', 'counselor', 4, 5, '0', '0', 'admin', NOW());

-- 5. 验证
SELECT role_id, role_name, role_key, data_scope FROM sys_role ORDER BY role_id;
