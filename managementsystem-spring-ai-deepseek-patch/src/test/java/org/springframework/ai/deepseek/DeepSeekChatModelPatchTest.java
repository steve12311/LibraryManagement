/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.deepseek;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatOptions.Thinking;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.api.DeepSeekApi.ChatCompletionRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekChatModelPatchTest {

	private final DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
		.deepSeekApi(DeepSeekApi.builder().apiKey("test-api-key").build())
		.build();

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void shouldPropagateReasoningContentForDeepSeekV4ThinkingToolCalls() throws JsonProcessingException {
		DeepSeekChatOptions options = DeepSeekChatOptions.builder()
			.model("deepseek-v4-pro")
			.thinking(Thinking.enabled())
			.reasoningEffort("high")
			.build();
		DeepSeekAssistantMessage assistantMessage = new DeepSeekAssistantMessage.Builder()
			.content("")
			.reasoningContent("需要先查询馆藏，再根据库存组织推荐。")
			.toolCalls(List.of(new AssistantMessage.ToolCall("call_vector", "function", "searchCatalog", "{}")))
			.build();

		ChatCompletionRequest request = this.chatModel
			.createRequest(new Prompt(List.of(new UserMessage("推荐一些计算机基础入门书籍"), assistantMessage), options), true);

		assertThat(request.messages().get(1).reasoningContent()).isEqualTo("需要先查询馆藏，再根据库存组织推荐。");
		assertThat(request.thinking()).isEqualTo(Thinking.enabled());
		assertThat(request.reasoningEffort()).isEqualTo("high");

		String json = this.objectMapper.writeValueAsString(request);
		assertThat(json).contains("\"reasoning_content\":\"需要先查询馆藏，再根据库存组织推荐。\"");
		assertThat(json).contains("\"thinking\":{\"type\":\"enabled\"}");
		assertThat(json).contains("\"reasoning_effort\":\"high\"");
	}

	@Test
	void shouldNotPropagateReasoningContentWhenThinkingDisabled() {
		DeepSeekChatOptions options = DeepSeekChatOptions.builder()
			.model("deepseek-v4-pro")
			.thinking(Thinking.disabled())
			.reasoningEffort("high")
			.build();
		DeepSeekAssistantMessage assistantMessage = new DeepSeekAssistantMessage.Builder()
			.content("我会直接回答。")
			.reasoningContent("这段思考不应回传。")
			.toolCalls(List.of(new AssistantMessage.ToolCall("call_vector", "function", "searchCatalog", "{}")))
			.build();

		ChatCompletionRequest request = this.chatModel
			.createRequest(new Prompt(List.of(new UserMessage("推荐一些计算机基础入门书籍"), assistantMessage), options), true);

		assertThat(request.messages().get(1).reasoningContent()).isNull();
		assertThat(request.thinking()).isEqualTo(Thinking.disabled());
	}

	@Test
	void shouldKeepDeepSeekReasonerMultiRoundBehavior() {
		DeepSeekChatOptions options = DeepSeekChatOptions.builder()
			.model(DeepSeekApi.ChatModel.DEEPSEEK_REASONER.getValue())
			.build();
		DeepSeekAssistantMessage assistantMessage = new DeepSeekAssistantMessage.Builder()
			.content("最终答案。")
			.reasoningContent("deepseek-reasoner 普通多轮不回传 reasoning_content。")
			.build();

		ChatCompletionRequest request = this.chatModel
			.createRequest(new Prompt(List.of(new UserMessage("9.11 和 9.8 哪个大？"), assistantMessage), options), false);

		assertThat(request.messages().get(1).reasoningContent()).isNull();
	}

}
