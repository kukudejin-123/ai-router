/**
 * 黑名单管理接口
 
 */
package com.kkdj.airouter.controller;

import com.kkdj.airouter.annotation.AuthCheck;
import com.kkdj.airouter.common.BaseResponse;
import com.kkdj.airouter.common.ResultUtils;
import com.kkdj.airouter.constant.UserConstant;
import com.kkdj.airouter.service.BlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 黑名单管理接口（管理员）
 */
@RestController
@RequestMapping("/admin/blacklist")
public class BlacklistController {

    @Resource
    private BlacklistService blacklistService;

    /**
     * 获取黑名单列表
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取黑名单列表")
    public BaseResponse<Set<String>> getBlacklist() {
        Set<String> blacklist = blacklistService.getAllBlacklist();
        return ResultUtils.success(blacklist);
    }

    /**
     * 添加 IP 到黑名单
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "添加 IP 到黑名单")
    public BaseResponse<Boolean> addToBlacklist(@RequestBody BlacklistRequest request) {
        blacklistService.addToBlacklist(request.getIp(), request.getReason());
        return ResultUtils.success(true);
    }

    /**
     * 从黑名单移除 IP
     */
    @PostMapping("/remove")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "从黑名单移除 IP")
    public BaseResponse<Boolean> removeFromBlacklist(@RequestBody BlacklistRequest request) {
        blacklistService.removeFromBlacklist(request.getIp());
        return ResultUtils.success(true);
    }

    /**
     * 检查 IP 是否在黑名单中
     */
    @GetMapping("/check")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "检查 IP 是否在黑名单中")
    public BaseResponse<Boolean> checkBlacklist(@RequestParam String ip) {
        boolean blocked = blacklistService.isBlocked(ip);
        return ResultUtils.success(blocked);
    }

    /**
     * 获取黑名单数量
     */
    @GetMapping("/count")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "获取黑名单数量")
    public BaseResponse<Long> getBlacklistCount() {
        long count = blacklistService.getBlacklistCount();
        return ResultUtils.success(count);
    }

    /**
     * 黑名单请求对象
     */
    @Data
    public static class BlacklistRequest {
        /**
         * IP 地址
         */
        private String ip;
        /**
         * 封禁原因
         */
        private String reason;
    }
}
