package com.kkdj.airouter.service;

import com.kkdj.airouter.model.entity.Model;

import java.math.BigDecimal;

/**
 * 费用计算服务
 
 */
public interface BillingService {

    /**
     * 计算单次请求费用
     *
     * @param model            模型
     * @param promptTokens     输入Token数
     * @param completionTokens 输出Token数
     * @return 费用（元）
     */
    BigDecimal calculateCost(Model model, int promptTokens, int completionTokens);

    /**
     * 根据模型ID计算费用
     *
     * @param modelId          模型ID
     * @param promptTokens     输入Token数
     * @param completionTokens 输出Token数
     * @return 费用（元）
     */
    BigDecimal calculateCost(Long modelId, int promptTokens, int completionTokens);

    /**
     * 获取用户总消费金额
     *
     * @param userId 用户ID
     * @return 总消费金额（元）
     */
    BigDecimal getUserTotalCost(Long userId);

    /**
     * 获取用户今日消费金额
     *
     * @param userId 用户ID
     * @return 今日消费金额（元）
     */
    BigDecimal getUserTodayCost(Long userId);
}
