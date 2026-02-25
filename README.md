# LibraryManagement
图书管理系统毕业设计

## 架构分层（对齐 youlai-boot 风格）

### 模块
- `managementsystem-admin`：启动类与控制层（API 入口）
- `managementsystem-framework`：认证、安全过滤器、AOP 等框架能力
- `managementsystem-system`：业务域实现（service/mapper/converter/model）
- `managementsystem-common`：通用能力（base/model/constant/config/utils/token）

### 关键包结构
- `org.dwtech.system.model.{entity,form,query,vo,bo}`：系统与图书业务模型
- `org.dwtech.auth.model.{form,vo}`：认证业务模型（登录、验证码）
- `org.dwtech.common.base`：通用基类（`BaseEntity`、`BasePageQuery`、`BaseVO`）
- `org.dwtech.common.model`：通用模型（`Option`、`Avatar`）
