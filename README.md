# 辅导员工作日志系统

辅导员通过**自然语言 / 语音 / 照片**记录查寝、查卫生、谈心等日常工作，系统自动识别意图、落库为结构化日志，**按月统计**并支持 **Excel 导出**与**周报**。

面向全校 21 个学院、四级角色（管理员 / 学校领导 / 学院领导 / 辅导员），基于 **若依（RuoYi-Vue）** 框架，后续接入学校统一身份认证（CAS）。

---

## 功能

- **AI 文字录入**：自然语言 → 自动抽取「事件类型 / 地点 / 涉及学生 / 摘要」，支持补录日期（「上周五开会」自动识别回填到上周五）
- **语音录入**：腾讯云 ASR 语音转文字 → 走同一条 AI 识别链路
- **照片上传**：文字 + 图片一起归档，图片存若依 `/profile` 目录，列表可预览
- **九类工作分类**：查寝点名 / 安全卫生 / 谈心谈话 / 查课听课 / 班会活动 / 走访帮扶 / 危机处置 / 事务工作 / 其他
- **月度统计**：9 类卡片 + 近 6 月柱状图
- **工作日志列表**：搜索 / 日期筛选 / 编辑 / 删除 / 回收站 / 三级目录下钻
- **Excel 报表**：工作汇总 + 逐月趋势 + 明细三页；周报（本周概览 + 按日统计）
- **四级角色权限**：管理员看全部、学校领导看全部、学院领导看本学院、辅导员只看自己（领导只读）

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3 + MyBatis（若依 springboot3 分支）+ Java 21 + Maven |
| 前端 | Vue3 + Vite + TypeScript + Element Plus + Pinia（RuoYi-Vue3） |
| 数据库 | MySQL 8（库名 `ry-vue`）+ Redis（token 缓存） |
| 意图识别 | DeepSeek API |
| 语音识别 | 腾讯云 ASR |
| 报表 | Apache POI |
| 登录 | Spring Security + JWT（预留 CAS 单点登录） |
| 部署 | WinSW Windows 服务（Redis / 后端 / 前端） |

## 目录结构

```
ruoyi-migration/
  ruoyi-backend/        后端（若依多模块：ruoyi-admin / ruoyi-system / ruoyi-framework / ruoyi-common）
    ruoyi-system/       业务代码（com.ruoyi.visit.*：录入/统计/导出/回收站/三级目录/CAS）
  ruoyi-frontend/       前端（RuoYi-Vue3，views/visit/ 下为业务页）
  deploy/               部署目录（WinSW 服务 xml + 打包 jar）
docs/
  ruoyi-migration-plan.md   迁移方案文档
backend/  frontend-vue/     旧版系统（已废弃，仅供回溯）
backups/  uploads/          旧版遗留数据（保留未删）
```

业务页面（`ruoyi-frontend/src/views/visit/`）：

| 页面 | 路径 | 说明 |
|---|---|---|
| 今日记录 | `visit/today/index` | 聊天式录入（文字 / 语音 / 照片 / 补录确认） |
| 工作日志 | `visit/record/index` | 列表 / 搜索 / 编辑 / 删除 / 回收站 / 导出 / 周报 / 三级目录 |
| 月度统计 | `visit/stats/index` | 9 类卡片 + 柱状图 + 三级目录下钻 |

## 运行

### 环境

- JDK 21：`D:\dev\jdk-21.0.12.1+1`
- Maven：`D:\dev\apache-maven-3.9.16\bin\mvn.cmd`
- Node：`C:\Program Files\nodejs\node.exe`（或工作区托管版本）
- MySQL 8（`ry-vue` 库）、Redis（6379）

### 后端（8081）

```bash
cd ruoyi-backend
# 编译打包
mvn.cmd -DskipTests package
# 直接运行
java -jar ruoyi-admin/target/ruoyi-admin.jar --server.port=8081
```

### 前端（5174）

```bash
cd ruoyi-frontend
npm run dev      # 开发（代理 /dev-api → 8081）
npm run build:prod   # 生产构建（产物 dist/）
```

### Windows 服务（生产，deploy/ 目录）

```bash
cd deploy
winsw.exe install ruoyi-redis.xml      # Redis
winsw.exe install ruoyi-backend.xml    # 后端 8081
winsw.exe install ruoyi-frontend.xml   # 前端 5174
```

三个服务：`ruoyi-redis` / `ruoyi-backend` / `ruoyi-frontend`，`startmode=Automatic` 开机自启、失败自动重启。

> ⚠️ **凭证注入**：DeepSeek Key / 腾讯云凭证在 `deploy/ruoyi-backend.xml` 的 `<env>` 节点（Windows 服务读不到命令行 `set` 的变量，只能这样注入）。该文件含敏感信息，**勿提交版本库**。

## 默认账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | 管理员（启动自动创建） |
| `counselor1` ~ `counselor21` | `123456` | 辅导员（占位，挂 21 学院） |
| `collegeleader1` | `123456` | 学院领导（测试） |

## API 概览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 登录 |
| POST | `/visit/record/chat` | AI 文字/图片录入（text + recordDate + dryRun + files） |
| POST | `/visit/record/voice` | 语音录入 |
| GET | `/visit/record/list` | 列表（含 photos） |
| GET | `/visit/record/trash` | 回收站 |
| GET | `/visit/record/stats` | 月度统计 |
| GET | `/visit/record/export` | 导出 Excel |
| GET | `/visit/record/weekly-report` | 周报 |
| GET | `/visit/record/org-tree` | 三级目录 |
| GET | `/cas/login` `/cas/callback` | CAS 单点登录（默认关闭） |

## 当前状态与待办

**已迁移完成**：阶段 0–4（环境 / 数据模型 / 后端业务代码 / 前端页面），核心功能全量端到端验证通过。

**待完成**：

- **生产部署**：Nginx 静态托管 + HTTPS + 正式域名（当前仍是 vite preview 开发态托管，暂停中）
- **CAS 接入**：代码已就绪（默认关闭），待学校开通接入 + 提供测试账号后做真实登录闭环验证
- **数据迁移**：21 个占位辅导员账号待替换为真实名单

详见 `docs/ruoyi-migration-plan.md` 与项目记忆（`.workbuddy/memory/`）。
