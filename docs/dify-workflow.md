# Dify Workflow & ChatFlow 创建手册

> 适配说明：Dify Workflow/ChatFlow 的输入变量仅支持文本、段落、下拉选项、数字、复选框、单文件与文件列表。后端已将企业数据压缩为这些类型的字段，请按照下方指引配置以保证联调成功。

---

## 1. Workflow：enterprise_onboarding_workflow

### 1.1 创建流程

1. 登录 Dify 控制台，进入 **Workflow** 页面点击“新建 Workflow”。
2. 选择“空白流程”，命名为 `enterprise_onboarding_workflow` 并保存。

### 1.2 配置输入变量

在 `Start` 节点新增下列变量，类型需与后端字段一致：

| 变量名 | 显示标签 | 类型 | 说明 |
| --- | --- | --- | --- |
| `company_id_text` | 企业 ID | 文本 | 后端生成的唯一编号，便于落库或回查。 |
| `company_name_text` | 企业名称 | 文本 | 直接用于展示或检索。 |
| `credit_code_text` | 统一社会信用代码 | 文本 | 18 位字符串。 |
| `establish_date_text` | 成立日期 | 文本 | 采用 `yyyy-MM-dd`。 |
| `scale_dropdown` | 企业规模 | 下拉选项 | 预设 `{1-49, 50-149, 150-499, 500+}`。 |
| `industry_paragraph` | 所属行业 | 段落 | 逗号分隔的行业标签。 |
| `company_type_dropdown` | 企业类型 | 下拉选项 | 预设 `{民营, 国企, 外资, 合资, 事业单位, 其他}`。 |
| `business_overview_paragraph` | 业务简介 | 段落 | 500-1500 字文本。 |
| `core_offerings_paragraph` | 核心产品/服务 | 段落 | 后端格式：每行一条“名称（类型）：描述”。 |
| `technology_stack_text` | 技术栈 | 文本 | 逗号分隔的技术关键词。 |
| `has_intellectual_property_checkbox` | 是否有知识产权 | 复选框 | `true/false`。 |
| `intellectual_property_paragraph` | 知识产权详情 | 段落 | 若为空则为“未提供知识产权信息”。 |
| `contact_name_text` | 联系人姓名 | 文本 | |
| `contact_title_text` | 联系人职务 | 文本 | |
| `contact_phone_text` | 联系电话 | 文本 | |
| `contact_email_text` | 工作邮箱 | 文本 | |
| `address_paragraph` | 地址 | 段落 | 由“国家 省 市 区 详细地址”组合。 |
| `business_license_file` | 营业执照 | 单文件 | 由前端上传。 |
| `attachment_file_list` | 其他附件 | 文件列表 | 可为空。 |

> 提示：无需手动创建 JSON 类型。表单默认保存为字符串/布尔值，后端会把数组拼接成可读段落。

点击“测试输入”，可使用示例数据验证：

```json
{
  "company_id_text": "4b6f2c43-...",
  "company_name_text": "示例科技有限公司",
  "credit_code_text": "9132XXXXXXXXXXXXXX",
  "establish_date_text": "2019-06-01",
  "scale_dropdown": "150-499",
  "industry_paragraph": "软件和信息服务业, 制造业",
  "company_type_dropdown": "民营",
  "business_overview_paragraph": "企业主营工业互联网平台......",
  "core_offerings_paragraph": "工业互联网平台（方案）：跨设备连接\n预测性维护（服务）：通过数据建模提升设备稳定性",
  "technology_stack_text": "Java, Spring Boot, MySQL",
  "has_intellectual_property_checkbox": true,
  "intellectual_property_paragraph": "发明专利：ZL2023XXXX（工业数据采集方法）",
  "contact_name_text": "张三",
  "contact_title_text": "法定代表人",
  "contact_phone_text": "+86-13500000000",
  "contact_email_text": "zhangsan@example.com",
  "address_paragraph": "中国 上海 上海 浦东新区 世纪大道100号",
  "business_license_file": "file-uuid-1",
  "attachment_file_list": ["file-uuid-2", "file-uuid-3"]
}
```

### 1.3 推荐节点编排

| 顺序 | 节点类型 | 配置要点 |
| --- | --- | --- |
| 1 | **LLM 节点：入库摘要** | Prompt 示例：`请基于提供的企业字段，生成 300 字以内摘要并列出两条亮点。` 输入使用上述文本变量。 |
| 2 | **LLM 节点：标签提取** | Prompt 示例：`根据行业、技术栈、核心产品提取 3 个行业标签、3 个能力标签、3 个技术关键词，以 JSON 数组返回。` |
| 3 | **HTTP 请求节点** | 把原始字段、摘要与标签推送至自有数据库或知识库。 |
| 4 | **结束节点** | 输出 `status`、`summary`、`tags` 等字段供前端确认。 |

发布 Workflow 后，在“设置 → API”中复制 `Workflow Code`，填入后端 `application.yml` 的 `dify.workflow-code`。

---

## 2. ChatFlow：enterprise_match_chatflow

### 2.1 创建流程

1. 进入 **ChatFlow** 页面点击“新建 ChatFlow”，选择“空白会话”。
2. 命名为 `enterprise_match_chatflow`，在“配置”中启用 **API Access** 并记录 `ChatFlow Code`。

### 2.2 输入与知识源

在 `Inputs` 面板创建：

| 变量名 | 类型 | 说明 |
| --- | --- | --- |
| `query` | 文本 | 用户的提问。 |
| `context` | 段落 | 后端拼接的企业目录文本，格式示例见下方。 |

可在“知识”模块中连接 Workflow 写入的摘要/标签数据源，用于补充检索。

### 2.3 提示词与输出

系统 Prompt 示例：

```
你是企业匹配助手。输入中包含：
- `query`：用户需求文本
- `context`：多个企业的概览，使用"---"分隔
请结合知识库命中结果输出 JSON：
{
  "match_score": 0-1 之间的小数，
  "company_id": "命中的企业ID或空字符串",
  "company_name": "命中的企业名称或空字符串",
  "reason": "简要说明匹配原因",
  "recommended_products": ["可推荐的产品或服务"]
}
若无法匹配，请将 `match_score` 设为 0。
```

回答模板保持 JSON 结构：

```json
{
  "match_score": {{match_score}},
  "company_id": "{{company_id}}",
  "company_name": "{{company_name}}",
  "reason": "{{reason}}",
  "recommended_products": {{recommended_products}}
}
```

后端 `/api/companies/match` 会发送如下数据：

```json
{
  "query": "寻找具备新能源电池管理系统经验的企业",
  "context": "企业ID：4b6f2c43-...\n企业名称：示例科技有限公司\n行业：制造业\n规模：150-499\n业务简介：工业互联网平台...\n核心产品/服务：工业互联网平台（方案）：跨设备连接\n技术栈：Java, Spring Boot, MySQL\n\n---\n\n企业ID：9c88...\n企业名称：新能源装备股份有限公司\n..."
}
```

---

## 3. 凭证配置

1. 在 Dify 个人设置中创建专用 **API Key**。
2. 在 `backend/src/main/resources/application.yml` 设置：

```yaml
dify:
  api-key: "dify_xxx"
  base-url: "https://api.dify.ai/v1"
  workflow-code: "enterprise_onboarding_workflow"
  chatflow-code: "enterprise_match_chatflow"
```

3. 重启 Spring Boot 应用后即可通过后端接口驱动 Workflow/ChatFlow。

---

## 4. 联调建议

- 使用 Postman 依次验证 `/api/documents`、`/api/companies`、`/api/companies/match`。
- 在 Dify 控制台观察 Workflow 与 ChatFlow 的运行日志，确认变量名与类型匹配。
- 若需扩展字段，可在后端继续拼接为文本/段落或新增布尔、下拉型变量，保持输入类型兼容。

通过以上配置，即可在受限的输入类型下完成企业资料入库与问答匹配的闭环。
