package com.kkdj.airouter.controller;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.kkdj.airouter.annotation.AuthCheck;
import com.kkdj.airouter.annotation.RateLimit;
import com.kkdj.airouter.common.BaseResponse;
import com.kkdj.airouter.common.ResultUtils;
import com.kkdj.airouter.constant.UserConstant;
import com.kkdj.airouter.exception.BusinessException;
import com.kkdj.airouter.exception.ErrorCode;
import com.kkdj.airouter.model.dto.image.ImageGenerationRequest;
import com.kkdj.airouter.model.dto.image.ImageGenerationResponse;
import com.kkdj.airouter.model.entity.ApiKey;
import com.kkdj.airouter.model.entity.ImageGenerationRecord;
import com.kkdj.airouter.model.entity.User;
import com.kkdj.airouter.service.ApiKeyService;
import com.kkdj.airouter.service.ImageGenerationService;
import com.kkdj.airouter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 图片生成控制器
 
 */
@RestController
@RequestMapping("/v1/images")
@Slf4j
public class ImageController {

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private UserService userService;

    @Resource
    private ApiKeyService apiKeyService;

    /**
     * 生成图片（OpenAI 兼容接口）
     */
    @PostMapping("/generations")
    @Operation(summary = "生成图片")
    @RateLimit(type = RateLimit.LimitType.API_KEY, limit = 10)
    public ImageGenerationResponse generateImage(
            @RequestBody ImageGenerationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {

        Long userId = null;
        Long apiKeyId = null;

        // 认证：优先使用 API Key，否则使用 Session
        if (StrUtil.isNotBlank(authorization) && authorization.startsWith("Bearer ")) {
            String apiKeyValue = authorization.substring(7);
            ApiKey apiKey = apiKeyService.getByKeyValue(apiKeyValue);

            if (apiKey == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 无效或已失效");
            }
            if (!"active".equals(apiKey.getStatus())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 已被禁用");
            }

            userId = apiKey.getUserId();
            apiKeyId = apiKey.getId();
        } else {
            // 尝试从 Session 获取用户信息
            try {
                User loginUser = userService.getLoginUser(httpRequest);
                userId = loginUser.getId();
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录或提供有效的 API Key");
            }
        }

        String clientIp = httpRequest.getRemoteAddr();

        // 调用服务生成图片
        return imageGenerationService.generateImage(request, userId, apiKeyId, clientIp);
    }

    /**
     * 获取我的图片生成记录
     */
    @GetMapping("/my/records")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的图片生成记录")
    public BaseResponse<Page<ImageGenerationRecord>> getMyRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ImageGenerationRecord> page = imageGenerationService.listUserRecords(
                loginUser.getId(), pageNum, pageSize);
        return ResultUtils.success(page);
    }
}
