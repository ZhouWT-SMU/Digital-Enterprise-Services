# 数字化企业服务 Demo

本仓库提供一个前后端分离的示例项目，用于演示以下功能模块的基础框架：

1. 企业匹配
2. 专利搜寻（预留）
3. 企业搜寻（预留）

## 项目结构

```
.
├── backend       # Node.js + Express 后端服务
└── frontend      # 静态前端页面（HTML/CSS/JavaScript）
```

## 快速开始

1. 安装依赖：

   ```bash
   cd backend
   npm install
   ```

2. 启动后端开发服务器（默认端口 3000）：

   ```bash
   npm run start
   ```

   启动后访问 `http://localhost:3000` 可打开前端页面。

## 企业匹配工作流对接

- 后端预置了 `src/services/workflowClient.js`，用于与 Dify 工作流交互。
- 通过设置环境变量 `DIFY_BASE_URL` 与 `DIFY_API_KEY` 即可启用真实的工作流调用。
- 当前工作流尚未搭建时，接口会返回回显请求参数的占位响应，方便前端调试。

## 后续规划

- 在专利搜寻与企业搜寻模块接入实际接口，并完善前端交互。
- 根据 dify-java-client 或其他 SDK 完善 Dify 工作流的调用与鉴权逻辑。
- 增加统一的日志、配置与错误处理机制。
