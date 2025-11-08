# 企业信息采集与 Dify 集成示例

该仓库提供一个简洁的前后端方案，帮助企业快速完成资料采集、文件上传，并通过 Dify workflow/chatflow 进行入库与问答匹配。整体结构遵循 [dify-java-client](https://github.com/imfangs/dify-java-client/tree/main/src) 的职责划分：

- `backend/`：Spring Boot 3 应用，提供企业信息、文件上传与 Dify 对接接口。
- `frontend/`：无需打包工具的 Vue 3 + 原生 JavaScript 页面，可直接以静态文件形式部署。
- `docs/`：包含 Dify workflow/chatflow 的配置参考。

## 架构概览

```
┌────────────────┐     HTTP      ┌────────────────────┐      REST       ┌──────────────────────┐
│ Vue 静态页面   │ ───────────▶ │ Spring Boot API     │ ─────────────▶ │ Dify Workflow/Chatflow│
│ (HTML+JS+CSS) │              │ /api/* 接口          │               │ 企业画像与问答服务     │
└────────────────┘              └────────────────────┘               └──────────────────────┘
```

- **前端**：基于 CDN 加载的 Vue 3，仅依赖 `index.html`、`app.js`、`styles.css` 三个文件，方便与任意后端或网关集成。
- **后端**：标准 Spring Boot 工程，利用 `RestClient` 调用 Dify API，并以内存仓储保存示例数据。
- **Dify**：通过配置文件指定 workflow/chatflow 编码，实现企业信息入库与问答匹配。

## 目录结构

```
.
├── backend                     # Spring Boot 项目
│   ├── pom.xml
│   └── src/main/java/com/example/des
│       ├── controller          # REST 控制器
│       ├── service             # 业务封装、Dify 调用
│       ├── repository          # 内存存储示例
│       ├── dify                # Dify Client
│       └── dto/model           # 数据结构与校验
├── frontend
│   ├── index.html              # Vue 应用入口
│   ├── app.js                  # 表单逻辑、API 调用
│   └── styles.css              # 基础样式
└── docs
    └── dify-workflow.md        # Dify 配置指引
```

## 后端运行

```bash
cd backend
mvn spring-boot:run
```

默认监听 `http://localhost:8080`。关键配置位于 `backend/src/main/resources/application.yml`：

```yaml
dify:
  api-key: <从 Dify 控制台生成的 API Key>
  base-url: https://api.dify.ai/v1
  workflow-code: enterprise_onboarding_workflow
  chatflow-code: enterprise_match_chatflow
```

> 请将 `workflow-code` 与 `chatflow-code` 替换为在 Dify 中创建的流程编码。

## 前端运行

前端为纯静态文件，可直接双击打开 `frontend/index.html`，或使用任意静态服务器托管：

```bash
# 例如使用 npm 提供的 http-server
yarn global add http-server  # 或 npm install -g http-server
cd frontend
http-server
```

如需跨域访问其他主机上的后端，可在加载页面前设置全局变量：

```html
<script>window.API_BASE = 'https://your-domain.com/api';</script>
```

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/documents` | 上传 PDF/DOCX/PNG/JPG 文件，返回 `id` 等元数据。 |
| `POST` | `/api/companies` | 提交企业资料，后端完成校验并触发 Dify workflow。 |
| `GET`  | `/api/companies` | 查看已提交的企业（示例为内存存储）。 |
| `POST` | `/api/companies/match?query=...` | 调用 Dify chatflow，根据问题匹配企业。 |

## 表单字段覆盖

前端表单覆盖了需求中所有核心字段：

1. **企业基础信息**：名称、统一社会信用代码、成立日期、地址（国家/省/市/区/详细地址）、规模、行业、企业类型、营业执照。
2. **能力与业务画像**：业务简介（500–1500 字）、核心产品/服务（可动态添加）、技术栈、知识产权、附件上传。
3. **联系人信息**：姓名、职务、电话、工作邮箱。

文件上传采用后端 `/api/documents` 接口，前端会在上传成功后自动填充 `fileId` 至提交 payload 中。

## Dify 集成

- `CompanyService` 会在企业创建时构造 workflow payload（含企业基本信息、联系人、产品列表、文件 ID 等），并通过 `DifyClient` 的 `triggerWorkflow` 方法调用。
- 匹配侧边栏使用 `matchCompany` 接口，该接口会将当前企业列表作为上下文发往 chatflow，实现简单的问答匹配。
- 更详细的流程节点设计、prompt 示例如需参考，请查看 [docs/dify-workflow.md](docs/dify-workflow.md)。

## 扩展建议

1. **持久化实现**：将示例中的内存仓储替换为 MySQL/PostgreSQL，并把文件保存到对象存储。
2. **身份认证**：在 Spring Security 层面增加企业账号与审批流程，保障数据安全。
3. **Workflow 回调**：Dify workflow 支持回调，可新增接收端点用于同步任务状态或结果。
4. **多语言界面**：在前端引入 i18n，实现中英文或多语言切换。

## 许可证

本示例以 MIT 协议开源，可自由使用与修改。
