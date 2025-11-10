# Dify 企业对话筛选

## 简介
基于 Spring Boot WebFlux 与 Vue 3 的端到端示例，整合 Dify Chat App 与 Dataset，提供对话式的企业筛选体验。后端以 SSE 代理形式转发模型输出，并在模型调用 `pick_companies` 工具或降级解析 `<FILTERS>` 时联动知识库检索；前端通过导航栏与模块化路由挂载“对话筛选”与示例占位模块。

## 架构
```
┌───────────────────────────────┐
│           前端 (Vue)          │
│  ┌─────────────┐  ┌────────┐ │
│  │ ChatModule  │  │ About  │ │
│  └──────┬──────┘  └────────┘ │
│         │  EventSource / REST │
└─────────┼────────────────────┘
          │
┌─────────▼────────────────────┐
│        后端 (Spring Boot)    │
│  ChatController  CompanyCtrl │
│  │          │               │
│  ▼          ▼               │
│ DifyChatClient  CompanySvc  │
│        │           │        │
│        ▼           ▼        │
│    Dify Chat App  Dataset   │
└──────────────────────────────┘
```

## 运行步骤
1. 设置系统环境变量：`DIFY_BASE_URL`、`DIFY_CHAT_APP_KEY`、`DIFY_DATASET_API_KEY`、`DIFY_DATASET_ID`
2. 进入 `backend` 目录执行 `mvn spring-boot:run`
3. 打开浏览器访问 [http://localhost:8080/](http://localhost:8080/)

## 使用示例
- 页面操作：进入“对话筛选”模块，输入“找在华东、有 AI 能力的中型制造企业”，点击“开始对话”，实时查看模型回答与公司列表。
- API 调用：`GET /api/companies/search?q=AI&industry=制造业&size=中型&region=华东&tech=Kubernetes&limit=10`

## 常见问题
- **SSE 连接中断**：请确认浏览器允许跨域访问，并检查网络与 Dify 配置；必要时刷新页面重新建立会话。
- **工具调用未启用**：若模型未触发 `pick_companies`，后端会解析回答中的 `<FILTERS>{...}</FILTERS>` 块并以此检索公司列表，实现降级补偿。
