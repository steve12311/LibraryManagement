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

package org.springframework.ai.deepseek.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.deepseek.api.DeepSeekApi.ChatCompletionChunk;
import org.springframework.ai.deepseek.api.DeepSeekApi.ChatCompletionChunk.ChunkChoice;
import org.springframework.ai.deepseek.api.DeepSeekApi.ChatCompletionMessage;
import org.springframework.ai.deepseek.api.DeepSeekApi.ChatCompletionMessage.ChatCompletionFunction;
import org.springframework.ai.deepseek.api.DeepSeekApi.ChatCompletionMessage.Role;
import org.springframework.ai.deepseek.api.DeepSeekApi.ChatCompletionMessage.ToolCall;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekStreamFunctionCallingHelperPatchTest {

	private final DeepSeekStreamFunctionCallingHelper helper = new DeepSeekStreamFunctionCallingHelper();

	@Test
	void shouldMergeReasoningContentWhenStreamingToolCallChunksAreMerged() {
		ChatCompletionChunk firstChunk = chunk(new ChatCompletionMessage("", Role.ASSISTANT, null, null,
				List.of(new ToolCall("call_vector", "function", new ChatCompletionFunction("searchCatalog", "{"))),
				null, "需要先"));
		ChatCompletionChunk secondChunk = chunk(new ChatCompletionMessage("", Role.ASSISTANT, null, null,
				List.of(new ToolCall(null, null, new ChatCompletionFunction(null, "}"))), null, "查询馆藏"));

		ChatCompletionChunk merged = this.helper.merge(firstChunk, secondChunk);

		ChatCompletionMessage message = merged.choices().get(0).delta();
		assertThat(message.reasoningContent()).isEqualTo("需要先查询馆藏");
		assertThat(message.toolCalls()).hasSize(1);
		assertThat(message.toolCalls().get(0).id()).isEqualTo("call_vector");
		assertThat(message.toolCalls().get(0).function().name()).isEqualTo("searchCatalog");
		assertThat(message.toolCalls().get(0).function().arguments()).isEqualTo("{}");
	}

	@Test
	void shouldAttachAccumulatedReasoningAndContentToMergedToolCallChunk() {
		ChatCompletionChunk toolCallChunk = chunk(new ChatCompletionMessage("", Role.ASSISTANT, null, null,
				List.of(new ToolCall("call_vector", "function", new ChatCompletionFunction("searchCatalog", "{}"))),
				null, null));

		ChatCompletionChunk chunk = this.helper.withAccumulatedMessageContent(toolCallChunk, "我先查询馆藏。",
				"需要先理解需求，再调用向量检索。");

		ChatCompletionMessage message = chunk.choices().get(0).delta();
		assertThat(message.content()).isEqualTo("我先查询馆藏。");
		assertThat(message.reasoningContent()).isEqualTo("需要先理解需求，再调用向量检索。");
		assertThat(message.toolCalls()).hasSize(1);
		assertThat(message.toolCalls().get(0).id()).isEqualTo("call_vector");
	}

	private ChatCompletionChunk chunk(ChatCompletionMessage message) {
		return new ChatCompletionChunk("chatcmpl-test", List.of(new ChunkChoice(null, 0, message, null)), 0L,
				"deepseek-v4-pro", null, null, "chat.completion.chunk", null);
	}

}
