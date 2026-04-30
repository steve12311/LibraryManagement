# Spring AI DeepSeek 临时补丁模块

本模块基于 `org.springframework.ai:spring-ai-deepseek:1.1.5` 源码建立仓库内临时补丁，用于支持 DeepSeek V4 thinking mode 下的工具调用协议。

删除条件：上游 Spring AI 原生支持 `thinking`、`reasoning_effort` 以及 thinking tool-call 场景中的 `reasoning_content` 回传后，可以移除此模块，并恢复直接依赖官方 `spring-ai-deepseek`。
