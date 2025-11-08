# Dify Workflow & ChatFlow 配置指南

本文档说明如何在 Dify 中创建与本仓库配套的 Workflow 与 ChatFlow，并提供示例 payload 结构。

## 1. Workflow：企业入库处理（enterprise_onboarding_workflow）

### 1.1 目标

* 校验企业基础信息（名称与统一社会信用代码一致性、18 位格式校验）。
* 将企业能力画像（核心产品、技术栈、知识产权、附件）写入知识库或数据库。
* 根据行业、规模等标签生成标准化摘要，供后续 ChatFlow 检索使用。

### 1.2 节点设计

| 节点 | 类型 | 描述 |
| --- | --- | --- |
| **Start** | 输入节点 | 接收后端发送的 JSON payload。 |
| **Validate License** | Python/Script | 校验统一社会信用代码格式，查询工商 API（可选）。 |
| **Summarize Profile** | LLM Node | 基于业务简介、核心产品生成 300 字摘要。 |
| **Generate Tags** | LLM Node | 归纳行业标签、能力标签，输出结构化 JSON。 |
| **Persist Data** | Webhook/HTTP | 调用企业主数据服务或数据库，写入企业信息及文件 ID。 |
| **Notify** | Email/Webhook | 通知运营或业务人员审核（可选）。 |
| **End** | 结束节点 | 返回处理状态与摘要、标签。 |

### 1.3 输入 / 输出示例

**输入 payload（后端 `CompanyService` 构造）**

```json
{
  "company_id": "2c1b19d8-...",
  "name": "示例科技有限公司",
  "unified_social_credit_code": "9132XXXXXXXXXXXXXX",
  "establishment_date": "2019-06-01",
  "scale": "150-499",
  "industries": ["软件和信息服务业", "制造业"],
  "company_type": "民营",
  "business_overview": "...500-1500 字...",
  "technology_stack": ["Java", "Spring Boot", "MySQL"],
  "core_offerings": [
    {
      "name": "工业互联网平台",
      "type": "方案",
      "description": "..."
    }
  ],
  "intellectual_properties": [
    {
      "type": "发明专利",
      "registration_number": "ZL2023XXXX",
      "description": "工业数据采集方法"
    }
  ],
  "contact": {
    "name": "张三",
    "title": "法定代表人",
    "phone": "+86-13500000000",
    "work_email": "zhangsan@example.com"
  },
  "address": {
    "country": "中国",
    "province": "上海",
    "city": "上海",
    "district": "浦东新区",
    "street": "世纪大道100号"
  },
  "business_license_file_id": "9f2d...",
  "attachments": ["a11b...", "c32d..."]
}
```

**输出建议**

```json
{
  "status": "SUCCESS",
  "company_id": "2c1b19d8-...",
  "summary": "该公司专注于...",
  "tags": ["工业互联网", "设备互联", "制造业数字化"],
  "data_store_record_id": "ent_20240601001"
}
```

后端可以扩展为监听 Workflow 的回调，更新企业状态。

## 2. ChatFlow：企业匹配问答（enterprise_match_chatflow）

### 2.1 目标

* 接收用户查询文本，通过向量检索或工作流信息匹配合适的企业。
* 返回命中的企业摘要、标签、核心产品列表等。

### 2.2 配置步骤

1. **知识来源**：
   * 绑定 Workflow 生成的企业摘要与标签，可存入 Dify 知识库或外部向量数据库。
   * 将 `company_id` 作为 metadata，便于回传给前端。
2. **提示词模板**：
   * 指示模型输出结构化 JSON，例如：

   ```text
   你是企业信息匹配助手。根据用户需求，从给定的企业列表中选择最匹配的公司，输出 JSON：
   {
     "match_score": 0-1,
     "company_id": "",
     "company_name": "",
     "reason": "",
     "recommended_products": []
   }
   ```
3. **输入变量**：
   * `query`：用户问题（由后端传入）。
   * `context`：后端缓存的企业关键信息（名称、行业、核心产品名称等）。

### 2.3 输入 / 输出示例

后端 `CompanyService.matchCompany` 发送的 payload：

```json
{
  "query": "寻找具备新能源电池管理系统经验的企业",
  "context": [
    {
      "company_id": "2c1b19d8-...",
      "name": "示例科技有限公司",
      "industries": ["制造业"],
      "business_overview": "...",
      "core_offerings": ["工业互联网平台-方案", "设备预测性维护-服务"]
    }
  ]
}
```

ChatFlow 返回示例：

```json
{
  "match_score": 0.87,
  "company_id": "2c1b19d8-...",
  "company_name": "示例科技有限公司",
  "reason": "拥有工业互联网平台与电池维护方案案例，符合新能源电池管理需求。",
  "recommended_products": ["工业互联网平台", "设备预测性维护"]
}
```

前端 `MatchPanel` 会将该 JSON 直接呈现，实际生产环境可根据 `company_id` 再请求详情页。

## 3. 安全与扩展建议

* **鉴权**：在 Dify 控制台创建专用 API Key，并在后端安全存储（可使用环境变量/密钥管理服务）。
* **限流**：为 `/api/companies`、`/api/companies/match` 增加限流与审计，防止滥用。
* **回调/状态机**：根据 Workflow 执行结果更新企业状态（待审核、审核中、已入库等）。
* **多模型策略**：可根据行业类型路由到不同模型或提示词，提高匹配准确率。

通过上述配置，即可完成“企业信息采集 + Workflow 入库 + ChatFlow 智能匹配”的闭环方案。
