# 数字化企业服务 Demo

本仓库提供一个前后端分离的示例项目，用于演示以下功能模块的基础框架：

1. 企业匹配
2. 专利搜寻（预留）
3. 企业搜寻（预留）

## 项目结构

```
.
├── backend       # Spring Boot 后端服务
└── frontend      # 静态前端页面（HTML/CSS/JavaScript）
```

## 后端（Spring Boot）快速开始

1. 确保本地已安装 JDK 17 以及 Maven。
2. 进入后端目录并启动服务：

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   应用默认监听 `http://localhost:8080`。

3. 若需要打包可执行 JAR：

   ```bash
   mvn clean package
   ```

## Dify 工作流对接

- 配置文件位于 `backend/src/main/resources/application.yml`，可通过如下属性配置工作流：

  ```yaml
  dify:
    workflow:
      base-url: https://api.dify.ai
      api-key: <你的 API Key>
      workflow-id: <工作流 ID>
  ```

- `DifyWorkflowClient` 目前使用简单的 HTTP 调用方式，并带有占位返回。待 [dify-java-client](https://github.com/imfangs/dify-java-client) 发布后，可在该位置替换为官方客户端调用。

## 前端使用

前端为纯静态页面，可直接通过浏览器打开 `frontend/index.html` 进行调试。页面会将匹配请求发送至后端 `/api/matching` 接口，并展示返回结果。

## 后续规划

- 在专利搜寻与企业搜寻模块接入实际接口，并完善前端交互。
- 基于 dify-java-client 或官方 SDK 完善鉴权、错误处理与响应解析。
- 根据业务需要补充认证、日志与持久化等通用能力。
