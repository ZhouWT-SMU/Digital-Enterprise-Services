# 企业信息采集与 Dify 集成示例

该仓库提供一个简洁的前后端方案，帮助企业完成资料采集、文件上传，并通过 Dify Workflow/ChatFlow 实现入库与问答匹配。整体结构对齐 [dify-java-client](https://github.com/imfangs/dify-java-client/tree/main/src) 的职责划分，同时参考 [SMU-Agent-PaperRAGPipeLine](https://github.com/ZhouWT-SMU/SMU-Project/tree/SMU-Agent-PaperRAGPipeLine) 的工程组织方式。

- `backend/`：Spring Boot 3 应用，提供企业信息提交、文件上传以及与 Dify 的对接接口。
- `frontend/`：无需构建工具的 Vue 3 页面，左侧导航区分“企业信息录入”和“企业匹配问答”两大模块。
- `docs/`：包含 Dify Workflow/ChatFlow 的详细创建步骤。

## 架构概览

```
┌────────────────┐     HTTP      ┌────────────────────┐      REST       ┌──────────────────────┐
│ Vue 静态页面   │ ───────────▶ │ Spring Boot API     │ ─────────────▶ │ Dify Workflow/ChatFlow│
│ (HTML+JS+CSS) │              │ /api/* 接口          │               │ 企业画像与问答服务     │
└────────────────┘              └────────────────────┘               └──────────────────────┘
```

- **前端**：通过 CDN 加载 Vue，采用简单的左侧导航，将企业录入和匹配问答拆分为两个独立面板，表单字段覆盖业务需求。
- **后端**：内存态存储示例实现，无 DTO/复杂校验，直接以领域模型 `Company` 对接 Spring MVC 与 Dify API。
- **Dify**：在控制台中配置 Workflow 与 ChatFlow，流程代码通过配置文件注入，详情参见 `docs/dify-workflow.md`。

## 目录结构

```
.
├── backend                     # Spring Boot 项目
│   ├── pom.xml
│   └── src/main/java/com/example/des
│       ├── controller          # REST 控制器
│       ├── service             # 业务封装、Dify 调用
│       ├── dify                # Dify Client
│       └── model               # 领域模型
├── frontend
│   ├── index.html              # Vue 应用入口（含侧边导航）
│   ├── app.js                  # 表单逻辑、API 调用
│   └── styles.css              # 页面样式
└── docs
    └── dify-workflow.md        # Dify 配置手册
```

## 后端运行

```bash
cd backend
mvn spring-boot:run
```

应用默认监听 `http://localhost:8080`。关键配置位于 `backend/src/main/resources/application.yml`：

```yaml
dify:
  api-key: <从 Dify 控制台生成的 API Key>
  base-url: https://api.dify.ai/v1
  workflow-code: enterprise_onboarding_workflow
  chatflow-code: enterprise_match_chatflow
```

> 请将 `workflow-code` 与 `chatflow-code` 替换为实际发布后的编码。

## 前端运行

前端为纯静态页面，可直接打开 `frontend/index.html`，或使用任意静态服务器：

```bash
# 例如使用 npm 提供的 http-server
yarn global add http-server  # 或 npm install -g http-server
cd frontend
http-server
```

如需跨域访问部署在其他域名的后端，可在加载页面前设置全局变量：

```html
<script>window.API_BASE = 'https://your-domain.com/api';</script>
```

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/documents` | 上传 PDF/DOCX/PNG/JPG 文件，返回文件 `id` 等元数据。 |
| `POST` | `/api/companies` | 提交企业资料，后端直接触发 Dify Workflow。 |
| `GET`  | `/api/companies` | 查看已提交企业（示例以内存 Map 保存）。 |
| `POST` | `/api/companies/match?query=...` | 调用 Dify ChatFlow，根据问题匹配企业。 |

## 表单字段覆盖

前端录入页面覆盖以下核心字段：

1. **企业基础信息**：名称、统一社会信用代码、成立日期、地址（国家/省/市/区/详细地址）、规模、行业、企业类型、营业执照。
2. **能力画像**：业务简介、核心产品/服务（名称、类型、描述）、技术栈、知识产权（类型、编号、说明）、附件上传。
3. **联系人信息**：姓名、职务、电话、工作邮箱。

上传文件将自动调用 `/api/documents`，并把返回的 `id` 注入企业提交 payload。

## Dify 集成流程

1. `CompanyController` 接收前端提交的 `Company` JSON，`CompanyService` 保存到内存并构造 Workflow payload。
2. `DifyClient` 通过 `RestClient` 调用 Dify API：`/workflows/{code}/execute` 与 `/chatflows/{code}/messages`。
3. 匹配模块调用 `/api/companies/match`，后端把已提交企业的核心信息作为 `context` 传递给 ChatFlow。
4. 详细的 Workflow/ChatFlow 创建步骤见 [docs/dify-workflow.md](docs/dify-workflow.md)。

## 扩展建议

- **持久化**：将内存 Map 替换为数据库（JPA/MyBatis），文件同步上传到对象存储。
- **鉴权与审计**：接入企业认证、操作日志、限流等安全措施。
- **流程编排**：根据 Workflow 执行结果新增回调接口或审批流程。
- **多端入口**：可在 Vue 页面外嵌聊天组件，或提供移动端适配样式。

## 许可证

本示例以 MIT 协议开源，可自由使用与修改。
