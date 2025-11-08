# 企业信息采集与 Dify 工作流集成方案

本仓库提供一个完整的前后端示例，用于支撑企业信息采集、文件上传，以及将企业画像发送至 Dify Workflow 与 ChatFlow 的集成。整体设计借鉴了 [dify-java-client](https://github.com/imfangs/dify-java-client/tree/main/src) 的模块划分方式，拆分为 `backend` 与 `frontend` 两个主要目录。

## 架构概览

```
┌─────────────┐        ┌────────────────────┐        ┌─────────────────────┐
│ React 前端  │  HTTP  │ Spring Boot 后端    │  REST  │ Dify Workflow/ChatFlow │
│ (Vite+TS)  │ ─────► │ /api/companies 等  │ ─────► │ 工作流执行 / 会话接口 │
└─────────────┘        └────────────────────┘        └─────────────────────┘
        ▲                         │                              │
        │                         └── In-memory 存储 / 对象存储扩展 │
        └────── React Query 状态管理 ──────────────────────────────┘
```

* **前端**：基于 React + TypeScript + Vite，提供企业资料填报表单、文件上传组件以及 ChatFlow 匹配侧边栏。
* **后端**：采用 Spring Boot 3，提供企业信息 API、文件上传 API，并通过可配置的 `DifyClient` 调用 Dify 工作流与 ChatFlow。
* **Dify 集成**：配置在 `application.yml` 中，可分别指定 Workflow Code 与 ChatFlow Code，方便在不同环境间切换。

## 目录结构

```
.
├── backend              # Spring Boot 应用
│   ├── pom.xml
│   └── src/main/java/com/example/des
│       ├── controller   # REST API 定义
│       ├── service      # 业务逻辑、Dify 组装
│       ├── repository   # 内存数据存储（可替换为数据库）
│       └── dify         # 与 Dify 交互的客户端封装
├── frontend             # React 前端
│   ├── package.json
│   └── src
│       ├── components   # 表单/上传/匹配组件
│       ├── api          # Axios 封装
│       └── pages        # 页面布局
└── docs
    └── dify-workflow.md # Dify Workflow & ChatFlow 配置指南
```

## 快速开始

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口 `8080`，关键配置位于 `src/main/resources/application.yml`：

```yaml
dify:
  api-key: <从 Dify 控制台生成的 API Key>
  base-url: https://api.dify.ai/v1
  workflow-code: enterprise_onboarding_workflow
  chatflow-code: enterprise_match_chatflow
```

> `workflow-code` 与 `chatflow-code` 需与 Dify 后台创建的流程保持一致。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`，并通过 Vite 代理将 `/api` 请求转发到后端 `http://localhost:8080`。

### 3. 主要 API 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/documents` | 接收 PDF/DOCX/PNG/JPG 等文件并返回 `fileId`，用于关联到企业信息。 |
| `POST` | `/api/companies` | 提交企业基础信息与画像。后端会自动调用 Dify Workflow 进行入库处理。 |
| `GET`  | `/api/companies` | 查看已提交的企业列表（内存版示例）。 |
| `POST` | `/api/companies/match?query=...` | 调用 Dify ChatFlow，根据问题/需求返回最匹配企业。 |

## 核心实现要点

### 表单字段覆盖

* 企业基础信息：名称、统一社会信用代码、成立日期、注册地址、企业规模、行业、企业类型、营业执照文件。
* 能力/业务画像：业务简介（500-1500字限制）、核心产品/服务（可动态增删）、技术栈、知识产权、附件上传。
* 联系与法律：法人/授权联系人姓名、职务、电话、邮箱。

### 文件上传策略

* 后端 `DocumentStorageService` 默认以内存字典存储文件元数据，可根据实际需要扩展至对象存储（OSS、S3 等）。
* 上传成功后返回 `fileId`，前端自动回填到企业提交 payload 中。

### Dify 调用封装

* `DifyClient` 使用 Spring `RestClient` 发起 HTTP 请求。
* `CompanyService` 将企业信息序列化为 Workflow/ChatFlow 所需的 JSON 结构：
  * Workflow：包含公司概况、核心产品、知识产权、联系人、文件 ID 等。
  * ChatFlow：以当前内存中的企业列表构造上下文，实现轻量级的问答匹配示例。

详细的 payload 示例与 Dify 侧配置说明见 [docs/dify-workflow.md](docs/dify-workflow.md)。

## 后续扩展建议

1. **持久化**：将 `CompanyRepository` 替换为数据库实现（MySQL/PostgreSQL），并接入对象存储保存文件内容。
2. **权限与审计**：在 Spring Security 层面引入企业登录/审批流，与 Dify Workflow 的状态机结合。
3. **工作流回调**：Dify Workflow 执行后可以通过 Webhook 回传结果，后端可新增回调接口以更新企业状态。
4. **多语言支持**：前端采用 i18n，实现中英文界面切换。

## 许可证

本示例以 MIT 协议开源，欢迎在企业项目中自定义和扩展。
