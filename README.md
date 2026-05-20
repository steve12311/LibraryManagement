# LibraryManagement

基于 Spring Boot 3 + MyBatis-Plus + Spring Security 的图书管理系统后端项目，支持 RBAC 权限、库存与借阅管理、验证码登录、Redis 缓存、AI 辅助检索、用户批量导入导出、协同过滤推荐。

## 1. 项目特性

- 账号登录、图形验证码、JWT 双令牌（access + refresh）、Token 刷新与注销
- 用户/角色/菜单/部门（RBAC）权限体系，方法级权限注解 + 行级数据权限隔离
- 图书、库存、借阅、分类、出版社管理
- 用户批量导入/导出（Excel 模板 + 行级校验 + 失败明细）
- 文件上传哈希去重（SHA-256）与 `fileId` 黑盒访问，所有文件公开可读；双模式删除（物理删除 + 引用计数删除），头像/封面变更时通过 Redis Stream 异步清理旧文件
- Redis 缓存（角色权限、菜单路由、下拉选项），变更时自动清理刷新
- 防重复提交（`@RepeatSubmit` + Redisson 分布式锁）
- 操作审计日志（`@OperLog`），自动记录操作人、业务标识、结果码
- 认证事件日志（登录/注销/刷新令牌），记录 IP、会话模式、成败原因
- 数据权限行级隔离（`@DataPermission`），支持全部/本部门/本部门及子部门/仅本人 四级
- 后台数据大屏：概览 / 趋势 / 排行 / 近期事件统计接口
- Spring AI + DeepSeek + Ollama + Milvus 向量检索，Tool Calling（日期/库存/向量工具）
- DeepSeek V4 thinking mode 支持（`reasoning_effort` + `reasoning_content`）
- SSE 流式对话（`/chat`），支持多轮对话记忆
- 基于物品的协同过滤推荐（余弦相似度 + 共借矩阵），首页书目个性化排序
- 图书预约管理（预约/取消、FIFO 排队、自动过期、取书确认）
- 消息中心（Redis Stream 队列、Email/SMS 多渠道发送、模板变量、用户通知偏好）
- 借阅逾期通知（每日定时任务 + 管理员手动提醒、去重机制）

## 2. 技术栈

| 组件 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.6 |
| Spring Security | 由 Spring Boot 3.5.6 BOM 管理 |
| Spring AI | 1.1.5 |
| MyBatis-Plus | 3.5.14 |
| MySQL | 8+ |
| Redis / Redisson | 7+ / 4.1.0 |
| Milvus | 通过 `spring-ai-starter-vector-store-milvus` 集成 |
| Ollama | 通过 `spring-ai-starter-model-ollama` 提供 Embedding |
| MapStruct | 1.6.3 |
| Hutool | 5.8.34 |
| FastExcel | 1.3.0 |

## 3. 模块说明

```
managementsystem-admin       <- 启动类与控制层（API 入口）
managementsystem-framework   <- 安全过滤器链、AOP 切面（防重复提交/操作审计）、
                                Spring AI（DeepSeek 对话 + 向量检索 + Tool Calling）
managementsystem-spring-ai-deepseek-patch  <- DeepSeek V4 thinking mode 临时补丁
managementsystem-system      <- 业务层（service / mapper XML / MapStruct converter / model）
managementsystem-common      <- 通用组件：Result、PageResult、BaseEntity、BaseController、
                                注解（@RepeatSubmit/@OperLog/@DataPermission）、
                                JWT/Redis Token 管理、全局异常处理
```

## 4. 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8+
- Redis 7+
- Milvus（启用 AI 向量检索与向量重建时）
- Ollama（用于 Embedding 生成）
- DeepSeek API Key（大模型对话）

## 5. 快速启动

### 5.1 配置

编辑 `managementsystem-admin/src/main/resources/application.yml`：

- `spring.datasource.*`
- `spring.data.redis.*`
- `spring.ai.deepseek.api-key`
- `spring.ai.ollama.base-url`
- `spring.ai.vectorstore.milvus.*`
- `ai.catalog-vector-rebuild.*`
- `security.session.*`
- `oss.local.storage-path`
- `file.cache.*`（文件元数据缓存）

初始化数据库：`docs/sql/ms.sql`

### 5.2 编译

```bash
# 全量构建 + 测试
mvn clean verify

# 编译打包（跳过测试）
mvn clean package -DskipTests

# 仅编译检查（mapper/service 改动后快速验证）
mvn -pl managementsystem-system -am -DskipTests compile
```

### 5.3 运行

```bash
# 启动应用（自动编译依赖模块）
mvn -pl managementsystem-admin -am spring-boot:run

# 运行系统模块全部测试
mvn -pl managementsystem-system -am test

# 运行单个测试类
mvn -pl managementsystem-system -am test -Dtest=RoleMenuServiceImplTest
```

默认端口：`8080`

## 6. 主要接口分组

### 认证

- `GET    /api/v1/auth/captcha`
- `POST   /api/v1/auth/login`
- `POST   /api/v1/auth/refresh-token`
- `DELETE /api/v1/auth/logout`

### 系统管理

- 用户：`/api/v1/users/**`
- 角色：`/api/v1/roles/**`
- 菜单：`/api/v1/menus/**`
- 部门：`/api/v1/dept/**`

### 图书业务

- 图书：`/api/v1/book/**`
- 库存：`/api/v1/stock/**`
- 借阅：`/api/v1/borrow/**`
- 分类：`/api/v1/category/**`
- 出版社：`/api/v1/publish/**`
- 书架地图：`/api/v1/library-map/**`（楼层、书架、公开地图）
- 预约管理：`/api/v1/reservation/**`（用户预约/取消）、`/api/v1/admin/reservation/**`（管理员确认取书/取消/队列查询）
- 借阅提醒：`POST /api/v1/borrow/{borrowId}/remind`（管理员手动发送逾期/到期提醒）
- 消息中心：`/api/v1/messages/**`（用户消息列表/已读/偏好设置）

### 仪表盘

- 统计概览：`GET /api/v1/dashboard/overview`
- 趋势：`GET /api/v1/dashboard/trends`
- 排行：`GET /api/v1/dashboard/rankings`
- 近期事件：`GET /api/v1/dashboard/recent-events`

### 文件服务

- 上传：`POST   /api/v1/files`
- 读取：`GET    /api/v1/files/{fileId}`
- 删除：`DELETE /api/v1/files/{fileId}`

### AI

- 流式对话：`GET /chat`（SSE，Tool Calling：日期、库存、向量检索）

## 7. 缓存与幂等

- Spring Cache + Redis 缓存菜单路由、角色权限、下拉选项
- 角色/权限变更后自动清理并刷新相关缓存
- 文件下载链路支持 Redis 元数据缓存（`fileId → 文件元数据`），对不存在文件做短期空值缓存防穿透
- 写接口使用 `@RepeatSubmit` 通过 Redisson 分布式锁防止短时间重复提交
- 协同过滤相似度矩阵缓存（`cf:sim:{isbn}`，Hash），每日凌晨 3 点重建；用户推荐列表缓存 30 分钟，借还书时失效

## 8. 文件存储说明

- 物理文件按内容哈希（SHA-256）去重存储，避免重复落盘
- 上传接口返回的 `url` 可能为 `/{fileId}`，实际文件读取入口统一为 `/api/v1/files/{fileId}`，不暴露真实存储路径

## 9. 开发约定

- **统一返回**：`Result<T>`（单项）/ `PageResult<T>`（分页）
- **权限注解**：管理类接口使用 `@PreAuthorize("@ss.hasPermi('domain:entity:action')")`，文件/当前用户类接口可使用 `@PreAuthorize("isAuthenticated()")`
- **实体基类**：继承 `BaseEntity` 获得 `id`（自增主键）、`createTime`、`updateTime`（MyBatis-Plus 自动填充）
- **审计人字段**：`createBy`/`updateBy` 由需要审计的实体类自行声明，非 BaseEntity 统一提供
- **逻辑删除**：`isDeleted`（Integer，0=未删除，1=已删除），在 Mapper XML 中手动追加 WHERE 条件
- **作者标注**：`@author steve12311`，`@since`（首次提交日期）
- **模型分层**：entity（DB 映射）/ dto（内部传输）/ vo（前端视图）/ bo（业务聚合）/ query（查询参数）/ form（请求表单）
- **对象映射**：MapStruct Converter 统一处理 Entity ↔ DTO ↔ VO 转换
- **敏感信息**：密钥、密码等通过环境变量或 `.env` 文件注入，禁止硬编码；`.gitignore` 已配置排除敏感文件类型
- **文档同步**：功能变更后同步更新 `AGENTS.md`、`CLAUDE.md`、`README.md` 三个项目文档

## 10. 许可证

本项目使用仓库根目录 `LICENSE` 中定义的开源协议。
