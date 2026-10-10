# 辅导员工作日志系统 → 若依（RuoYi）迁移方案

> 状态：规划基线（只读，未动现网系统）
> 日期：2026-09-24
> 目标：学校要求统一技术基座，整体迁移到若依框架，保留当前交互与业务能力，接入校园门户 CAS。

---

## 一、目标确认（用户已拍板）

1. 彻底迁移到若依框架（硬性要求，交给学校信息中心维护）；
2. 所有代码文件按若依标准工程结构组织（多模块 + 包命名 + 分层）；
3. 数据库**完全用若依标准表**（`sys_user`/`sys_role`/`sys_dept`/`sys_menu` 等），
   现有 `users`/`college` 与四级权限逻辑**重写映射**到若依 RBAC；
4. 尽可能保留业务能力：AI 录入、补录日期识别、语音转写、统计下钻、三级目录、报表导出；
5. 接入校园门户 CAS 统一认证（账号身份来自门户）；
6. 过程中随时停下确认，不做破坏性决定。

---

## 二、技术栈对比（迁移债评估）

| 维度 | 现状 | 若依目标 | 代价 |
|---|---|---|---|
| 后端 | Spring Boot 3.2.5 单模块 | RuoYi-Vue springboot3 分支多模块 | 低 |
| 前端 | Vue3+TS+Vite+ElementPlus+Pinia | RuoYi-Vue3-TypeScript | **极低（同栈）** |
| ORM | MyBatis-Plus | MyBatis（若依原生，可用 MP 兼容） | 中 |
| 登录 | 自写 JWT | Spring Security + JWT + Redis token | 中 |
| 权限 | 自写 Roles + PermissionService | RBAC + @DataScope AOP | **高（核心）** |
| 业务 | Ai/Asr/Report/Storage Service | 平移 | 低 |

结论：**前端近乎无损平移，后端业务 Service 大多可平移，真正重写的是「登录 + 权限 + 数据模型」三层。**

---

## 三、四级角色 → 若依模型映射（核心设计）

| 现状概念 | 若依落点 |
|---|---|
| 学校「浙江农林大学」 | `sys_dept` 顶级部门节点 |
| 21 个学院 | `sys_dept` 的 21 个子部门 |
| 管理员/学校领导/学院领导/辅导员 | `sys_role` 4 个角色 |
| 管理员看全部 | `@DataScope(DATA_SCOPE_ALL)` |
| 辅导员只看自己 | `@DataScope(DATA_SCOPE_SELF)` |
| 学院领导看本学院 | `@DataScope(DATA_SCOPE_DEPT)` |
| 学校领导看除管理员外全部 | **自定义 SQL 片段**（若依原生覆盖不了） |

**关键取舍**：若依原生的 DataScope 只有「全部/本部门及以下/本部门/仅本人」几种，
「学校领导=除管理员外全部」「学院领导=本学院辅导员（不含学院领导本人）」这类
**精确到角色组合的规则需要写自定义数据权限**。方案：保留现有 `PermissionService`
「集中计算可见 userId 集合」的思路，改造成若依的「自定义数据权限数据源」，
挂到 `@DataScope(deptAlias=...)` 的自定义实现里。业务逻辑不违背，只换挂载点。

---

## 四、数据库映射

### 若依框架表（照建，约 30 张）
`sys_user` `sys_role` `sys_menu` `sys_dept` `sys_post` `sys_user_role`
`sys_user_post` `sys_role_menu` `sys_role_dept` `sys_dict_type` `sys_dict_data`
`sys_config` `sys_notice` `sys_logininfor` `sys_oper_log` `sys_job` 等

### 业务表（平移，改名对齐若依命名风格）
| 现状表 | 若依目标表 | 说明 |
|---|---|---|
| `college` | 并入 `sys_dept` | 学院=部门，不再单独立表 |
| `users` | `sys_user` | 角色/部门用若依关联表表达 |
| `visit_records` | `visit_record`（业务表） | 保留，`user_id` 指向 `sys_user.id` |
| `visit_photos` | `visit_photo`（业务表） | 保留，外键指向 `visit_record` |

> 业务表保留但改单数命名、去掉若依已提供的字段（如创建人/时间若依用 BaseEntity）。

---

## 五、分步迁移计划（每步可独立验证，不停摆）

### 阶段 0：准备（只读，不碰现网）
- 拉取 RuoYi-Vue3-TypeScript 前端 + springboot3 后端；
- 确认 CAS 真实认证地址（`portal.zfsoft.com` 还是 `uis.zafu.edu.cn`）。

### 阶段 1：若依空壳跑通
- 建库、跑若依 `sql/` 脚本，起后端 + 前端，用若依自带 `admin/admin123` 登录；
- 目标：一个能跑的、纯若依的干净工程。

### 阶段 2：数据模型迁移
- 建业务表 `visit_record`/`visit_photo`；
- 建部门树（浙江农林大学 → 21 学院）；
- 建 4 个角色 + 角色-部门数据权限配置。

### 阶段 3：业务代码平移
- `AiServiceImpl`（含补录日期 `systemPromptWithToday`）、`AsrServiceImpl`、
  `ReportServiceImpl`、`StorageServiceImpl` 平移到 `ruoyi-system` 对应包；
- `VisitRecord` 业务 CRUD + 统计 + 导出 + 三级目录，改用 `@DataScope` + 自定义数据权限。

### 阶段 4：前端平移
- 业务页（今日记录/统计/日志/管理）迁到 RuoYi-Vue3 的 `views/` 下，注册成菜单；
- 复用若依的登录/布局外壳；业务页尽量保持现有交互。

### 阶段 5：CAS 统一认证接入
- 在若依的登录流程前挂 CAS 认证（jasig cas client 或手写 CAS 2.0 校验）；
- CAS 返回的 `CODE`（工号）映射到 `sys_user`，首次登录自动建号；
- 保留本地登录作为开发兜底（或按学校要求关闭）。

### 阶段 6：数据迁移 + 验证
- 老库 21 学院/21 辅导员/administrator → 若依新表（或按需新库从零）；
- 全量回归：AI 录入、补录、语音、统计、导出、四级权限、三级目录。

---

## 六、待确认事项（下一步要问）

1. **Q1 迁移方式**：全新起工程（A）/ 原地改（B）/ 先做样板模块（C，推荐）
2. **Q2 数据**：老数据迁入若依新表 / 新库从零（占位账号接 CAS 后可能要作废）
3. **CAS 真实地址**：`portal.zfsoft.com` 还是 `uis.zafu.edu.cn`
4. **CAS 调试账号**：学校是否提供测试账号密码
5. **本地登录去留**：接 CAS 后本地账号登录保留还是关闭

---

## 七、风险提示

- **权限是最大工作量**：若依 RBAC 与现有四级权限语义不完全对齐，
  「学校领导/学院领导」的细粒度数据范围需自定义，是迁移的风险点。
- **CAS 依赖学校侧**：真实地址、调试账号、回调地址都需学校提供，非纯代码问题。
- **不建议原地改**：原地改造中途系统不可用，推荐「全新工程 + 分阶段验证 + 最后切流量」。
