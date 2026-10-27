# 辅导员工作日志系统

辅导员通过**自然语言 / 语音 / 照片**记录查寝、查卫生、谈心等日常工作，系统自动识别意图、落库为结构化日志，**按月统计**并支持 **Excel 导出**与**周报**。

面向全校 21 个学院、四级角色（管理员 / 学校领导 / 学院领导 / 辅导员），基于 **若依（RuoYi-Vue）** 框架，预留学校统一身份认证（CAS）。

---

## 功能

### 智能录入
- **AI 文字录入**：自然语言 → 自动抽取「事件类型 / 地点 / 涉及学生 / 摘要」，支持补录日期（「上周五开会」自动识别回填到上周五）
- **语音录入**：腾讯云 ASR 语音转文字 → 走同一条 AI 识别链路，微信式「按住说话、松开发送」
- **照片上传**：文字 + 图片一起归档，一次可传多张，上传前自动压缩（长边 1600px）

### 日志管理
- **九类工作分类**：查寝点名 / 安全卫生 / 谈心谈话 / 查课听课 / 班会活动 / 走访帮扶 / 危机处置 / 事务工作 / 其他
- **工作日志列表**：搜索 / 日期筛选 / 编辑 / 删除 / 回收站 / 三级目录下钻
- **Excel 报表**：工作汇总 + 逐月趋势 + 明细三页；周报（本周概览 + 按日统计）

### 统计与权限
- **月度统计**：9 类彩色卡片 + 近 6 月柱状图
- **四级角色权限**：管理员看全部、学校领导看全部、学院领导看本学院、辅导员只看自己（两级领导只读）
- **组织下钻**：学校 → 学院 → 辅导员三级级联筛选（越权返回空不报错）

### 界面与体验
- **微信风格聊天界面**：绿色气泡、居中时间分组、按住说话
- **深色模式**：全站浅色 / 深色一键切换（顶栏太阳/月亮按钮或头像下拉），自动记住
- **移动端适配**：窄屏沉浸式布局，聊天区撑满全屏、输入栏贴底

### 数据安全
- **定时备份**：每日凌晨 2 点 + 启动补份，数据库全量 + 图片附件一起备份，滚动保留 14 份（`deploy/backups/`）
- **四级数据隔离**：接口层双重控制（功能权限 + 数据权限），软删除进回收站可追溯

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3 + MyBatis（若依 springboot3 分支）+ Java 21 + Maven |
| 前端 | Vue3 + Vite + TypeScript + Element Plus + Pinia（RuoYi-Vue3） |
| 数据库 | MySQL 8（库名 `ry-vue`）+ Redis（token / 验证码缓存） |
| 意图识别 | DeepSeek API |
| 语音识别 | 腾讯云 ASR |
| 报表 | Apache POI |
| 登录 | Spring Security + JWT（预留 CAS 单点登录） |
| 备份 | mysqldump + ZIP（Spring `@Scheduled` 定时任务） |
| 部署 | WinSW Windows 服务（Redis / 后端 / 前端） |

---

## 目录结构

```
ruoyi-migration/
  ruoyi-backend/        后端（若依多模块：ruoyi-admin / ruoyi-system / ruoyi-framework / ruoyi-common）
    ruoyi-system/       业务代码（com.ruoyi.visit.*：录入/统计/导出/回收站/三级目录/备份/存储/CAS）
  ruoyi-frontend/       前端（RuoYi-Vue3，views/visit/ 下为业务页）
  deploy/               部署目录（WinSW 服务 xml + 打包 jar + 定时备份输出）
docs/
  ruoyi-migration-plan.md   迁移方案文档
```

业务页面（`ruoyi-frontend/src/views/visit/`）：

| 页面 | 路径 | 说明 |
|---|---|---|
| 今日记录 | `visit/today/index` | 微信式聊天录入（文字 / 按住说话语音 / 照片 / 补录确认） |
| 工作日志 | `visit/record/index` | 列表 / 搜索 / 编辑 / 删除 / 回收站 / 导出 / 周报 / 三级目录 |
| 月度统计 | `visit/stats/index` | 9 类卡片 + 柱状图 + 三级目录下钻 |

---

## 运行

### 环境

- JDK 21、Maven 3.9、Node 18+、MySQL 8、Redis
- 环境变量（生产经 `deploy/*.xml` 注入，勿写入 git）：
  - `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` — 意图识别
  - `TENCENT_SECRET_ID` / `TENCENT_SECRET_KEY` / `TENCENT_ASR_APPID` — 语音转写
  - `MYSQL_PASSWORD` — 数据库密码（备份也复用此变量）
  - `TOKEN_SECRET` — JWT 强密钥
  - `RUOYI_PROFILE` — 文件存储根目录（Windows 默认 `D:/ruoyi/uploadPath`，Linux 设 `/home/ruoyi/uploadPath`）

### 后端（8081）

```bash
cd ruoyi-backend
mvn -DskipTests package
java -jar ruoyi-admin/target/ruoyi-admin.jar --server.port=8081
```

### 前端（5174）

```bash
cd ruoyi-frontend
npm install
npm run dev          # 开发（vite）
npm run build        # 构建产物到 dist/
```

### Windows 服务（生产，deploy/ 目录）

三件套用 WinSW 管理，开机自启：

| 服务 | 端口 | 说明 |
|---|---|---|
| `ruoyi-redis` | 6379 | Redis |
| `ruoyi-backend` | 8081 | Spring Boot jar |
| `ruoyi-frontend` | 5174 | `vite preview` 静态托管 dist |

```bash
cd deploy
./winsw.exe restart ruoyi-backend.xml    # 改后端配置后需重新 package + restart
./winsw.exe restart ruoyi-frontend.xml   # 改前端后需 build + restart
```

> ⚠️ 前端改动后若「构建产物不更新」，先 `rm -rf dist node_modules/.vite` 再 `build`（异步 chunk 会复用旧文件）。

### 定时备份

`ruoyi-backend/ruoyi-admin/src/main/resources/application.yml` 的 `visit.backup.*` 段控制：

- 每天凌晨 2:00 自动备份 + 启动时当天无备份则补一份
- 每份快照：`ry-vue.sql`（全库）+ `photos.zip`（图片附件）+ `如何恢复.txt`
- 滚动保留 14 份，输出到 `deploy/backups/backup-日期/`

---

## 默认账号

| 角色 | 账号 | 数据可见范围 |
|---|---|---|
| 管理员 | `admin` | 全部 + 系统管理 |
| 学校领导 | `schoolleader1` | 除管理员外全部（只读） |
| 学院领导 | `collegeleader1` | 本学院（只读） |
| 辅导员 | `counselor1` ~ `counselor21` | 仅本人 |

> 默认密码请查看部署配置；首次登录后建议修改。账号由管理员在「系统管理」手动创建。

---

## API 概览

业务接口位于 `/visit/record/*`（前缀 `VisitRecordController`）：

| 接口 | 说明 |
|---|---|
| `POST /visit/record/chat` | AI 文字 + 图片录入（multipart） |
| `POST /visit/record/voice` | 语音录入（wav → ASR → AI） |
| `GET /visit/record/list` | 分页列表（keyword / 日期 / userId 下钻） |
| `GET /visit/record/trash` | 回收站列表 |
| `GET /visit/record/stats` | 月度统计（9 类） |
| `GET /visit/record/export` | 导出 Excel 报表 |
| `GET /visit/record/weekly-report` | 导出周报 |
| `GET /visit/record/org-tree` | 三级组织目录 |
| `PUT /visit/record` | 编辑记录 |
| `DELETE /visit/record/{id}` | 软删除 |
| `POST /visit/record/restore/{id}` | 恢复 |
| `DELETE /visit/record/purge/{id}` | 彻底删除 |
| `GET /visit/record/{id}` | 单条详情 |

---

## 当前状态与待办

**已完成（截至 2026-10）**

- ✅ 若依迁移版全链路（录入 / 统计 / 导出 / 四级权限 / 三级目录）上线
- ✅ 菜单重构为三个一级菜单（今日记录 / 工作日志 / 月度统计）
- ✅ UI 三梯队：聊天质感 + 卡片配色 + 空状态 + 横幅 + 图标化
- ✅ 微信风格聊天界面 + 按住说话
- ✅ 深色模式 + 移动端撑满适配
- ✅ 图片上传压缩 + 上传限制调大（20MB/50MB）
- ✅ 数据库定时备份（滚动 14 份，含图片附件，已验证可恢复）
- ✅ 文件存储路径环境变量化（`RUOYI_PROFILE`，图片录音统一目录树）
- ✅ CAS SSO 代码就绪（`cas.enabled=false`，待学校测试账号）

**待办（按优先级）**

1. **事项 / 跟进体系**：当前为流水式日志，无「待跟进事项、结案状态、责任人」概念，管理层「问题是否闭环」无法回答（大工程，分阶段做）
2. **敏感信息分级**：谈心、心理、危机等记录目前无脱敏 / 分级授权 / 访问留痕
3. **审核流程**：辅导员提交 → 负责人审核 → 退回修改（文档要求，未实现）
4. **成效统计**：区分「工作量」与「工作成效」（当前仅计数）
5. **生产部署**：域名 + 云服务器 + Nginx + HTTPS + 移动端公网访问（阻塞：学校域名未发放）
6. **21 个占位辅导员账号** → 替换为真实姓名

---

## 部署安全提示

- `deploy/*.xml`、`.env` 含敏感凭证，已在 `.gitignore` 中，**勿提交公开仓库**
- 数据库密码、AI/ASR Key、Token 密钥全部经环境变量注入，不硬编码
