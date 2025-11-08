# Dify Workflow & ChatFlow 创建手册

本文档提供在 Dify 控制台中搭建企业入库 Workflow 与企业匹配 ChatFlow 的步骤说明，配合本仓库的 Spring Boot 接口即可完成“信息采集 → Workflow 入库 → ChatFlow 问答匹配”的闭环。

---

## 1. Workflow：enterprise_onboarding_workflow

### 1.1 创建流程

1. 登录 Dify 控制台，进入 **Workflow** 页面，点击“新建 Workflow”。
2. 填写名称，例如 `enterprise_onboarding_workflow`，描述可写“企业资料入库与摘要”。
3. 选择“空白流程”模板并创建。

### 1.2 配置入参

1. 在 `Start` 节点中新增以下输入变量，全部设置为 **JSON 对象/数组/字符串** 类型：
   - `company_id`
   - `name`
   - `unified_social_credit_code`
   - `establishment_date`
   - `scale`
   - `industries`
   - `company_type`
   - `business_overview`
   - `technology_stack`
   - `core_offerings`
   - `intellectual_properties`
   - `contact`
   - `address`
   - `business_license_file_id`
   - `attachments`
2. 点击“测试输入”并粘贴下方样例 JSON，确保解析无误。

```json
{
  "company_id": "2c1b19d8-...",
  "name": "示例科技有限公司",
  "unified_social_credit_code": "9132XXXXXXXXXXXXXX",
  "establishment_date": "2019-06-01",
  "scale": "150-499",
  "industries": ["软件和信息服务业", "制造业"],
  "company_type": "民营",
  "business_overview": "企业主营工业互联网平台...",
  "technology_stack": ["Java", "Spring Boot", "MySQL"],
  "core_offerings": [
    { "name": "工业互联网平台", "type": "方案", "description": "跨设备连接" }
  ],
  "intellectual_properties": [
    { "type": "发明专利", "registration_number": "ZL2023XXXX", "description": "工业数据采集方法" }
  ],
  "contact": { "name": "张三", "title": "法定代表人", "phone": "+86-13500000000", "work_email": "zhangsan@example.com" },
  "address": { "country": "中国", "province": "上海", "city": "上海", "district": "浦东新区", "street": "世纪大道100号" },
  "business_license_file_id": "9f2d...",
  "attachments": ["a11b...", "c32d..."]
}
```

### 1.3 节点搭建建议

| 顺序 | 节点类型 | 配置要点 |
| ---- | -------- | -------- |
| 1 | **Python/脚本节点** | 可选，校验统一社会信用代码格式或去重。 |
| 2 | **LLM 节点：业务摘要** | Prompt 示例如：“根据企业资料生成 300 字以内摘要，突出行业、能力与客户案例。” |
| 3 | **LLM 节点：标签提取** | Prompt 示例：“请生成 `行业标签`、`能力标签`、`技术关键词` 三组列表，以 JSON 返回。” |
| 4 | **工具节点：HTTP 请求** | 将原始信息、摘要、标签写入自建数据库或知识库。 |
| 5 | **可选通知节点** | Webhook / 邮件通知运营。 |
| 6 | **结束节点** | 输出 `status`、`summary`、`tags`、`data_store_record_id` 等字段。 |

完成后点击“发布”并记下 Workflow 代码（`Settings` → `API` → `Workflow Code`），填入 `backend/src/main/resources/application.yml` 的 `dify.workflow-code` 中。

---

## 2. ChatFlow：enterprise_match_chatflow

### 2.1 创建流程

1. 在 Dify 控制台切换到 **ChatFlow**，点击“新建 ChatFlow”。
2. 选择“空白会话”模板，命名为 `enterprise_match_chatflow`。
3. 在“配置”页签中开启 **API Access** 并复制 `ChatFlow Code`。

### 2.2 输入与知识源

1. 在 `Inputs` 中新增变量：
   - `query`（字符串）：用户在前端输入的问题。
   - `context`（JSON 数组）：后端 `/api/companies/match` 推送的企业关键信息。
2. 在“知识”模块中新增一个知识库，将 Workflow 输出的摘要、标签、核心产品等内容导入（可通过 API 或手动上传）。
3. 设置向量检索或关键词检索参数，推荐 `Top-K = 5`、相似度阈值 `0.5` 作为起点。

### 2.3 提示词模板

在系统 Prompt 中粘贴示例，指导模型输出结构化 JSON：

```
你是企业匹配助手。根据 `context` 列表和知识库检索结果，为 `query` 找到最匹配的企业。
如果无法确定，请返回 `match_score: 0`。
输出 JSON：
{
  "match_score": 0-1,
  "company_id": "",
  "company_name": "",
  "reason": "",
  "recommended_products": []
}
```

在回答模板中插入变量：

```json
{
  "match_score": {{match_score}},
  "company_id": "{{company_id}}",
  "company_name": "{{company_name}}",
  "reason": "{{reason}}",
  "recommended_products": {{recommended_products}}
}
```

发布后，后端会将以下 payload 发送至 ChatFlow：

```json
{
  "query": "寻找具备新能源电池管理系统经验的企业",
  "context": [
    {
      "company_id": "2c1b19d8-...",
      "name": "示例科技有限公司",
      "industries": ["制造业"],
      "business_overview": "工业互联网平台提供商...",
      "core_offerings": ["工业互联网平台-方案", "设备预测性维护-服务"]
    }
  ]
}
```

---

## 3. 凭证配置

1. 在 Dify 个人设置页生成 **API Key**，建议为该项目单独创建。
2. 在后端 `application.yml` 中设置：

```yaml
dify:
  api-key: "dify_xxx"
  base-url: "https://api.dify.ai/v1"
  workflow-code: "enterprise_onboarding_workflow"
  chatflow-code: "enterprise_match_chatflow"
```

3. 重启 Spring Boot 应用，前端页面即可通过后端接口触发 Workflow/ChatFlow。

---

## 4. 联调建议

- 使用 Postman 先调用 `/api/documents`、`/api/companies`、`/api/companies/match`，确认网络连通。
- 在 Dify 控制台的“调试”页观察 Workflow/ChatFlow 的实时日志，校对字段名称。
- 正式接入时，可将 Workflow 节点的 HTTP 请求指向对象存储、工单系统或 CRM，实现流程自动化。

至此，即可完成简洁易维护的企业资料采集与智能问答匹配解决方案。
