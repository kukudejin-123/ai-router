# AI Router — 智能 AI 网关

多模型适配、API Key 管理、Token 配额、路由策略的 Spring Boot AI 网关项目。支持通义千问、DeepSeek、智谱 AI 等多模型提供商，并提供 BYOK（自带密钥）、流式对话、Stripe 支付、插件系统等功能。

## 功能特性

- **多模型适配** — 通义千问 / DeepSeek / 智谱 AI / OpenAI 兼容接口，统一接入
- **智能路由** — 支持固定路由、按成本优先、按延迟优先、自动路由四种策略
- **BYOK（自带密钥）** — 用户可绑定自己的 API Key，AES-GCM 加密存储
- **流式对话** — Server-Sent Events (SSE) 实时流式响应
- **AI 生图** — 异步图片生成，轮询结果，支持限流
- **Token 配额管理** — 用户级别 Token 配额控制与用量统计
- **Stripe 支付** — 充值续费，Webhook 自动回调
- **插件系统** — 网页搜索（SerpAPI）、PDF 解析、图片识别
- **监控指标** — Micrometer + Prometheus，含 AI 调用成功率/延迟指标
- **安全加固** — BCrypt 密码哈希、AES-GCM 加密、IP 黑名单、限流、CORS 可配置
- **接口文档** — Knife4j（Swagger）自动生成

## 技术栈

| 层面 | 技术 |
|------|------|
| 框架 | Spring Boot 3.5.9, Spring AI 1.1.2 |
| 语言 | Java 21 (--enable-preview) |
| ORM | MyBatis-Flex 1.11.1 |
| 数据库 | MySQL 8.0+, Redis (Spring Session + Jedis) |
| AI SDK | Spring AI Alibaba (DashScope), OpenAI |
| 支付 | Stripe Java SDK 31.x |
| 限流/熔断 | Resilience4j |
| 监控 | Micrometer + Prometheus |
| 工具 | Hutool 5.x, Lombok, Redisson |
| 文档 | Knife4j (OpenAPI 3) |

## 快速开始

### 前置条件

- JDK 21
- MySQL 8.0+
- Redis 6.x+
- Maven 3.9+

### 1. 创建数据库

```sql
create database if not exists ai_router;
```

执行 `sql/create_table.sql` 创建表结构。

### 2. 配置环境变量

复制环境变量模板：

```bash
cp .env.example .env
```

编辑 `.env`，填入真实密钥（以下为必填项）：

| 变量 | 说明 |
|------|------|
| `MYSQL_PASSWORD` | MySQL 数据库密码 |
| `DASHSCOPE_API_KEY` | 阿里云 DashScope API Key |
| `STRIPE_API_KEY` | Stripe 密钥（测试用 `sk_test_` 开头） |
| `STRIPE_WEBHOOK_SECRET` | Stripe Webhook 签名密钥 |
| `ENCRYPTION_SECRET_KEY` | AES 加密密钥（32字节） |

> ⚠️ `.env` 已配置在 `.gitignore` 中，不会提交到 Git。请妥善保管密钥。

### 3. 启动

```bash
mvn spring-boot:run
```

或通过 IDE 直接运行 `AiRouterApplication.java`。

应用启动后默认会在用户表创建 3 个测试账号：

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `12345678` | 管理员 |
| `user` | `12345678` | 普通用户 |
| `test` | `12345678` | 普通用户 |

### 4. 访问

- API 地址：`http://localhost:8123/api`
- 接口文档：`http://localhost:8123/api/doc.html`
- 健康检查：`http://localhost:8123/api/actuator/health`
- Prometheus 指标：`http://localhost:8123/api/actuator/prometheus`

## 环境变量配置

所有敏感配置均通过 `.env` 文件注入，支持以下变量：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_PASSWORD` | `root` | MySQL 密码 |
| `DASHSCOPE_API_KEY` | — | 阿里云通义千问 API Key |
| `STRIPE_API_KEY` | — | Stripe 密钥 |
| `STRIPE_WEBHOOK_SECRET` | — | Stripe Webhook 密钥 |
| `SERPAPI_API_KEY` | — | SerpAPI 网页搜索密钥 |
| `ENCRYPTION_SECRET_KEY` | `ai-router-secret-key-256` | AES-GCM 加密密钥 |
| `CORS_ALLOWED_ORIGINS` | `*` | 允许的跨域来源 |

生产环境请使用 `application-prod.yml` 配置，通过 `spring.profiles.active=prod` 激活。

## 项目结构

```
src/main/java/com/kkdj/airouter/
├── adapter/           # 模型适配器（DashScope/DeepSeek/OpenAI/ZhipuAI）
├── annotation/        # 自定义注解（@RateLimit）
├── aop/               # AOP 切面（限流实现）
├── common/            # 通用返回/分页
├── config/            # Spring 配置（CORS/安全/数据初始化）
├── constant/          # 常量定义
├── controller/        # REST 控制器
├── exception/         # 全局异常处理
├── filter/            # 过滤器（IP黑名单/TraceID）
├── mapper/            # MyBatis-Flex Mapper
├── metrics/           # 监控指标（AI调用统计）
├── model/             # DTO/Entity/Enum/VO
├── plugin/            # 插件（网页搜索/PDF解析/图片识别）
├── service/           # 业务逻辑层
├── strategy/          # 路由策略
├── task/              # 定时任务（健康检查）
└── utils/             # 工具类（AES加密）
```

## API 概览

| 路径 | 说明 |
|------|------|
| `POST /api/chat` | 对话（流式/非流式） |
| `POST /api/image/generate` | AI 生图 |
| `POST /api/user/register` | 用户注册 |
| `POST /api/user/login` | 用户登录 |
| `GET /api/user/search` | 用户管理（管理员） |
| `POST /api/recharge/create` | Stripe 创建支付会话 |
| `POST /api/stripe/webhook` | Stripe Webhook 回调 |
| `GET /api/model/list` | 查看可用模型 |
| `POST /api/model/adapter` | 切换模型适配器 |
| `POST /api/key/bind` | 用户绑定自有 API Key |
| `POST /api/plugin/*` | 插件调用（搜索/PDF/OCR） |
| `POST /api/blacklist/*` | IP 黑名单管理（管理员） |
| `GET /api/stats/*` | 用量统计 |
| `GET /api/actuator/*` | 健康检查与监控 |

详细接口文档请运行后访问 `http://localhost:8123/api/doc.html`。

## 路由策略

| 策略 | 说明 |
|------|------|
| 固定路由 | 指定使用某个模型提供商 |
| 成本优先 | 自动选择单价最低的模型 |
| 延迟优先 | 自动选择响应最快的模型 |
| 自动路由 | 综合评分（延迟 + 成本 + 可用性） |

## 安全机制

- **密码安全** — BCryptPasswordEncoder 哈希，登录时自动升级旧 MD5 密码
- **API Key 加密** — AES-256 GCM 模式，随机 IV，密文+IV 一起存储
- **限流保护** — 基于 Resilience4j + 自定义注解，支持接口级别和用户级别限流
- **IP 黑名单** — 过滤器拦截，Redis 持久化
- **CORS** — 可配置允许来源
- **防 SQL 注入** — 排序字段白名单校验

## 监控指标

通过 Micrometer 暴露以下 Prometheus 指标：

- `ai_request_total` — AI 调用总数（按模型/提供商/状态标签）
- `ai_request_duration_seconds` — 调用延迟直方图
- `ai_error_total` — 错误计数（按模型标签）

## 许可证

MIT License
