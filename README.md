# LibraryManagement

基于 Spring Boot 3 + MyBatis-Plus + Spring Security 的图书管理系统后端项目，支持 RBAC 权限、库存与借阅管理、验证码登录、Redis 缓存、AI 辅助检索。

## 1. 项目特性

- 账号登录、验证码、Token 刷新与注销
- 用户/角色/菜单（RBAC）权限体系
- 图书、库存、借阅、分类、出版社管理
- 文件上传哈希去重（SHA-256）与 `fileId` 黑盒访问
- Redis 缓存（角色权限、菜单路由、下拉选项等）
- 防重复提交（`@RepeatSubmit` + AOP）
- Spring AI + Milvus + Ollama 向量检索（`/chat` SSE 流式）

## 2. 技术栈

- Java 21
- Spring Boot 3.5.x
- Spring Security
- MyBatis-Plus 3.5.x
- MySQL 8+
- Redis 7+
- Redisson
- Spring AI（DeepSeek / Ollama）
- Milvus
- MapStruct
- Hutool

## 3. 模块说明

- `managementsystem-admin`：启动类与控制层（API 入口）
- `managementsystem-framework`：安全过滤器、AOP、AI 相关能力
- `managementsystem-system`：业务层（service/mapper/converter/model）
- `managementsystem-common`：通用组件（配置、常量、工具、Token、统一返回）

## 4. 目录结构

```text
LibraryManagement
├── managementsystem-admin
│   ├── src/main/java/org/dwtech/controller
│   └── src/main/resources/application.yml
├── managementsystem-framework
│   └── src/main/java/org/dwtech/framework
├── managementsystem-system
│   ├── src/main/java/org/dwtech/system
│   └── src/main/resources/mapper
├── managementsystem-common
│   └── src/main/java/org/dwtech/common
└── pom.xml
```

## 5. 环境要求

- JDK 21
- Maven 3.9+
- MySQL
- Redis
- Milvus（启用 AI 向量检索时）
- Ollama（用于 Embedding）
- DeepSeek API Key（用于大模型对话）

## 6. 快速启动

### 6.1 配置

编辑 `managementsystem-admin/src/main/resources/application.yml`：

- `spring.datasource.*`
- `spring.data.redis.*`
- `spring.ai.deepseek.api-key`
- `spring.ai.ollama.base-url`
- `milvus.*`
- `security.session.*`
- `oss.local.storage-path`
- `file.cache.*`（文件元数据缓存）

建议将密钥、密码改为环境变量注入，不要在仓库中明文保存。

### 6.2 编译

```bash
mvn clean package -DskipTests
```

### 6.3 运行

```bash
mvn -pl managementsystem-admin -am spring-boot:run
```

默认端口：`8080`

## 7. 主要接口分组

### 认证

- `GET /api/v1/auth/captcha`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh-token`
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

### 文件服务

- 上传文件：`POST /api/v1/files`
- 读取文件：`GET /api/v1/files/{fileId}`
- 删除文件：`DELETE /api/v1/files/{fileId}`

### AI

- 流式对话：`GET /chat`（SSE）

## 8. 缓存与幂等

- 使用 Spring Cache + Redis 做菜单、角色、选项数据缓存
- 角色权限变更后会清理并刷新相关缓存
- 文件下载链路支持 Redis 元数据缓存（`fileId -> 文件元数据`），并对不存在文件做短期空值缓存防穿透
- 写接口使用 `@RepeatSubmit` 防止短时间重复提交

## 9. 文件存储说明

- 本地存储场景下，物理文件按内容哈希去重存储，避免重复落盘
- 接口返回 `url=/{fileId}`，前端使用统一前缀 `/api/v1/files` 访问，不暴露真实文件路径
- 首次上线需执行 DDL：`docs/sql/20260226_file_dedup.sql`

## 10. 开发约定

- 统一返回：`Result` / `PageResult`
- 命名风格：参考 youlai-boot，方法与类名优先语义化
- 注释规范：已为 Java 文件补充类级文档注释，统一包含：
  - `@author steve12311`
  - `@since`（按文件首次提交日期）

## 11. 许可证

本项目使用仓库根目录 `LICENSE` 中定义的开源协议。
