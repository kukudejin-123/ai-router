package com.kkdj.airouter.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkdj.airouter.annotation.RateLimit;
import com.kkdj.airouter.exception.BusinessException;
import com.kkdj.airouter.exception.ErrorCode;
import com.kkdj.airouter.model.dto.chat.ChatMessage;
import com.kkdj.airouter.model.dto.chat.ChatRequest;
import com.kkdj.airouter.model.dto.chat.ChatResponse;
import com.kkdj.airouter.model.entity.ApiKey;
import com.kkdj.airouter.service.ApiKeyService;
import com.kkdj.airouter.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Chat Completions 接口（兼容 OpenAI 格式）
 
 */
@RestController
@RequestMapping("/v1/chat")
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * Chat Completions 接口
     * 支持流式和非流式响应
     */
    @PostMapping(value = "/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    @Operation(summary = "Chat Completions")
    @RateLimit(type = RateLimit.LimitType.API_KEY, limit = 60)
    public Object chatCompletions(@RequestBody ChatRequest request,
                                  @RequestHeader(value = "Authorization", required = false) String authorization,
                                  HttpServletRequest httpRequest) {
        // 1. 验证 API Key
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少或无效的 Authorization Header");
        }

        // 去掉 "Bearer " 前缀
        String apiKeyValue = authorization.substring(7);
        ApiKey apiKey = apiKeyService.getByKeyValue(apiKeyValue);

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 无效或已失效");
        }

        // 2. 参数校验
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "messages 不能为空");
        }

        // 3. 获取客户端IP和User-Agent
        String clientIp = JakartaServletUtil.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 4. 判断是否为流式请求
        Boolean stream = request.getStream();
        if (stream != null && stream) {
            // 流式响应 - 转换为 SSE 格式
            return chatService.chatStream(request, apiKey.getUserId(), apiKey.getId(), clientIp, userAgent)
                    .map(streamResponse -> {
                        try {
                            // 将 StreamResponse 转换为 JSON
                            String json = objectMapper.writeValueAsString(streamResponse);
                            return json + "\n\n";
                        } catch (Exception e) {
                            log.error("Failed to serialize stream response", e);
                            return "";
                        }
                    });
        } else {
            // 非流式响应
            return chatService.chat(request, apiKey.getUserId(), apiKey.getId(), clientIp, userAgent);
        }
    }

    /**
     * Chat Completions 接口 - 支持文件上传
     * 使用 multipart/form-data 格式，自动识别图片/PDF 并注入插件上下文
     */
    @PostMapping(value = "/completions/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    @Operation(summary = "Chat Completions（支持文件上传）")
    @RateLimit(type = RateLimit.LimitType.API_KEY, limit = 60)
    public Object chatCompletionsWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "messages") String messagesJson,
            @RequestParam(value = "stream", required = false, defaultValue = "false") Boolean stream,
            @RequestParam(value = "routing_strategy", required = false) String routingStrategy,
            @RequestParam(value = "plugin_key", required = false) String pluginKey,
            @RequestParam(value = "enable_reasoning", required = false) Boolean enableReasoning,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) throws IOException {

        // 1. 验证 API Key
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少或无效的 Authorization Header");
        }
        String apiKeyValue = authorization.substring(7);
        ApiKey apiKey = apiKeyService.getByKeyValue(apiKeyValue);
        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 无效或已失效");
        }

        // 2. 解析消息列表
        List<ChatMessage> messages = objectMapper.readValue(messagesJson, new TypeReference<List<ChatMessage>>() {});
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "messages 不能为空");
        }

        // 3. 构建 ChatRequest
        ChatRequest request = new ChatRequest();
        request.setMessages(messages);
        request.setModel(model);
        request.setStream(stream);
        request.setRoutingStrategy(routingStrategy);
        request.setPluginKey(pluginKey);
        request.setEnableReasoning(enableReasoning);

        // 4. 处理上传的文件
        if (file != null && !file.isEmpty()) {
            request.setFileBytes(file.getBytes());
            String contentType = file.getContentType();
            request.setFileType(contentType);

            // 根据文件类型自动设置插件（如果未指定）
            if (StrUtil.isBlank(request.getPluginKey())) {
                if (contentType != null) {
                    if (contentType.startsWith("image/")) {
                        request.setPluginKey("image_recognition");
                    } else if ("application/pdf".equals(contentType)) {
                        request.setPluginKey("pdf_parser");
                    }
                }
            }

            log.info("文件上传: name={}, size={}, type={}, plugin={}",
                    file.getOriginalFilename(), file.getSize(), contentType, request.getPluginKey());
        }

        // 5. 设置默认模型
        if (StrUtil.isBlank(request.getModel())) {
            request.setModel("qwen-plus");
        }

        // 6. 获取客户端IP和User-Agent
        String clientIp = JakartaServletUtil.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 7. 判断是否为流式请求
        if (stream != null && stream) {
            return chatService.chatStream(request, apiKey.getUserId(), apiKey.getId(), clientIp, userAgent)
                    .map(streamResponse -> {
                        try {
                            String json = objectMapper.writeValueAsString(streamResponse);
                            return json + "\n\n";
                        } catch (Exception e) {
                            log.error("Failed to serialize stream response", e);
                            return "";
                        }
                    });
        } else {
            ChatResponse response = chatService.chat(request, apiKey.getUserId(), apiKey.getId(), clientIp, userAgent);
            return response;
        }
    }
}
