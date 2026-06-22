package com.kkdj.airouter.adapter;

import com.kkdj.airouter.exception.BusinessException;
import com.kkdj.airouter.exception.ErrorCode;
import com.kkdj.airouter.model.dto.chat.ChatRequest;
import com.kkdj.airouter.model.dto.chat.StreamChunk;
import com.kkdj.airouter.model.entity.Model;
import com.kkdj.airouter.model.entity.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智谱 AI 模型适配器（已适配阿里云百炼）
 * 通过阿里云百炼的 OpenAI 兼容接口调用 GLM 模型
 */
@Slf4j
@Component
public class ZhipuAIAdapter implements ModelAdapter {

    /**
     * 支持的提供者名称
     */
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("zhipu", "zhipuai", "glm");

    @Override
    public ChatResponse invoke(Model model, ModelProvider provider, ChatRequest chatRequest) {
        try {
            OpenAiChatModel chatModel = createChatModel(provider, model);
            Prompt prompt = buildPrompt(chatRequest, model.getModelKey());
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("智谱 AI 适配器调用模型 {} 失败", model.getModelKey(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用模型失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<ChatResponse> invokeStream(Model model, ModelProvider provider, ChatRequest chatRequest) {
        try {
            OpenAiChatModel chatModel = createChatModel(provider, model);
            Prompt prompt = buildPrompt(chatRequest, model.getModelKey());
            return chatModel.stream(prompt);
        } catch (Exception e) {
            log.error("智谱 AI 适配器流式调用模型 {} 失败", model.getModelKey(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<StreamChunk> invokeStreamChunk(Model model, ModelProvider provider, ChatRequest chatRequest) {
        return invokeStream(model, provider, chatRequest)
                .map(this::convertToStreamChunk)
                .filter(chunk -> !chunk.isEmpty());
    }

    /**
     * 将响应转换为统一的 StreamChunk 格式
     */
    private StreamChunk convertToStreamChunk(ChatResponse response) {
        StreamChunk.StreamChunkBuilder builder = StreamChunk.builder();

        // 提取 Token 信息
        if (ObjectUtils.isNotEmpty(response.getMetadata()) && ObjectUtils.isNotEmpty(response.getMetadata().getUsage())) {
            Usage usage = response.getMetadata().getUsage();
            Integer promptTokens = usage.getPromptTokens();
            Integer completionTokens = usage.getCompletionTokens();
            if (promptTokens != null && promptTokens > 0) {
                builder.promptTokens(promptTokens);
            }
            if (completionTokens != null && completionTokens > 0) {
                builder.completionTokens(completionTokens);
            }
        }

        // 提取内容
        if (ObjectUtils.isNotEmpty(response.getResult()) && ObjectUtils.isNotEmpty(response.getResult().getOutput())) {
            // 尝试从 metadata 中提取推理内容
            var metadata = response.getResult().getMetadata();
            if (metadata != null) {
                Object reasoningContent = metadata.get("reasoningContent");
                if (reasoningContent instanceof String reasoning && StringUtils.isNotBlank(reasoning)) {
                    builder.reasoningContent(reasoning);
                }
            }

            // 提取普通文本内容
            String text = response.getResult().getOutput().getText();
            if (text != null && !text.isEmpty()) {
                builder.text(text);
            }
        }

        StreamChunk chunk = builder.build();
        if (!chunk.hasText() && !chunk.hasReasoningContent()
                && chunk.getPromptTokens() == null && chunk.getCompletionTokens() == null) {
            return StreamChunk.empty();
        }
        return chunk;
    }

    @Override
    public boolean supports(String providerName) {
        return providerName != null && SUPPORTED_PROVIDERS.contains(providerName.toLowerCase());
    }

    /**
     * 创建 OpenAI ChatModel（通过阿里云百炼兼容接口调用 GLM 模型）
     */
    private OpenAiChatModel createChatModel(ModelProvider provider, Model model) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model.getModelKey())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * 构建 Prompt
     */
    private Prompt buildPrompt(ChatRequest chatRequest, String modelKey) {
        List<Message> messages = chatRequest.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(modelKey)
                .streamUsage(true);

        if (chatRequest.getTemperature() != null) {
            optionsBuilder.temperature(chatRequest.getTemperature());
        }
        if (chatRequest.getMaxTokens() != null) {
            optionsBuilder.maxTokens(chatRequest.getMaxTokens());
        }

        return new Prompt(messages, optionsBuilder.build());
    }

    /**
     * 转换消息类型
     */
    private Message convertMessage(com.kkdj.airouter.model.dto.chat.ChatMessage msg) {
        String role = msg.getRole();
        if ("system".equals(role)) {
            return new SystemMessage(msg.getContent());
        } else if ("assistant".equals(role)) {
            return new AssistantMessage(msg.getContent());
        } else {
            return new UserMessage(msg.getContent());
        }
    }
}
