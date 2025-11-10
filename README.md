# Digital Enterprise Services

基于 Dify 知识库的企业筛选助手，提供前后端一体化的对话式检索体验。前端使用 React + Tailwind 构建交互式界面，后端基于 Spring Boot 代理 Dify Chat App 与 Dataset API，实现 SSE 流式问答与企业卡片推荐。

```
┌────────────────────────────┐        ┌──────────────────────────────┐
│        React Frontend       │        │      Spring Boot Backend      │
│  Zustand/SSE 聊天与筛选界面 │◀────SSE─┤  SSE 代理 / Dify Chat / Map 状态 │
└────────────┬───────────────┘        └──────────────┬───────────────┘
             │                                        │
             │ REST                                   │ REST & SSE
             ▼                                        ▼
        Company Cards                         Dify REST APIs
                                            (Chat App & Dataset)
```

## 环境变量

在后端 `application.yml` 与根目录 `.env.example` 中预置了所需变量，部署时请按需修改：

| 变量 | 说明 |
| --- | --- |
| `DIFY_BASE_URL` | Dify 服务地址（例如 `http://localhost:5001`） |
| `DIFY_CHAT_APP_KEY` | Chat App Key，用于流式对话筛选 |
| `DIFY_DATASET_API_KEY` | Dataset API Key（只读），用于知识库检索 |
| `DIFY_DATASET_ID` | 企业知识库 Dataset ID |
| `SERVER_PORT` | 后端启动端口，默认 `8080` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端来源，默认 `http://localhost:5173` |
| `VITE_API_BASE` | 前端调用后端的基础地址，默认 `http://localhost:8080` |

将 `.env.example` 复制为 `.env` 并填充真实值：

```bash
cp .env.example .env
```

## Dify 配置指引

1. 在 Dify 控制台创建 Chat App，并启用需要的工具调用（`pick_companies`）或在系统提示中引导模型返回 `<FILTERS>{...}</FILTERS>` 片段。
2. 记录 Chat App 的 API Key，填入 `DIFY_CHAT_APP_KEY`。
3. 在知识库模块上传企业相关文档后获取 Dataset ID，并在 API Keys 页面创建只读 Dataset API Key，分别填入 `DIFY_DATASET_ID` 与 `DIFY_DATASET_API_KEY`。
4. 确认后端能够通过 `${DIFY_BASE_URL}` 访问到 Dify 服务。

## 本地启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

### 前端

```bash
cd frontend
pnpm install
pnpm dev
```

访问 <http://localhost:5173/chat> 即可体验。

### Docker Compose 一键启动

```bash
docker-compose up -d --build
```

前端服务在 5173 端口，后端暴露在 `SERVER_PORT` 指定端口。

## API 示例

### 聊天流式筛选

```bash
curl -N -X POST "http://localhost:8080/api/chat/stream" \
  -H "Content-Type: application/json" \
  -d '{
    "message":"找在华东、有AI能力的中型制造企业"
  }'
```

### 企业检索

```bash
curl "http://localhost:8080/api/companies/search?q=AI&industry=制造业&size=中型&region=华东&tech=Kubernetes&limit=10"
```

## 项目结构

```
backend/  Spring Boot 服务（WebFlux、SSE、Dify 客户端封装、知识库检索）
frontend/ React + Vite 单页应用（聊天界面、筛选栏、公司卡片）
docker-compose.yml  前后端容器化配置
.env.example        环境变量模板
```

## 测试

后端包含针对 `CompanySearchService` 的单元测试，验证检索参数构造与元数据过滤逻辑：

```bash
cd backend
mvn test
```
