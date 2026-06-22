package com.kkdj.airouter.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.kkdj.airouter.model.entity.Model;
import com.kkdj.airouter.model.enums.HealthStatusEnum;
import com.kkdj.airouter.model.enums.ModelStatusEnum;
import com.kkdj.airouter.model.enums.RoutingStrategyTypeEnum;
import com.kkdj.airouter.service.ModelService;
import com.kkdj.airouter.strategy.RoutingStrategyInterface;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 成本优先路由策略
 * 选择成本最低的健康模型
 
 */
@Component
public class CostFirstRoutingStrategy implements RoutingStrategyInterface {

    @Resource
    private ModelService modelService;

    @Override
    public Model selectModel(String modelType, String requestedModel) {
        QueryWrapper queryWrapper = buildBaseQueryWrapper(modelType);
        // 按成本排序（inputPrice + outputPrice），取第一个
        queryWrapper.orderBy("(inputPrice + outputPrice)", true);
        queryWrapper.limit(1);
        
        return modelService.getOne(queryWrapper);
    }

    @Override
    public List<Model> getFallbackModels(String modelType, String requestedModel) {
        QueryWrapper queryWrapper = buildBaseQueryWrapper(modelType);
        // 按成本排序，跳过第一个
        queryWrapper.orderBy("(inputPrice + outputPrice)", true);
        
        List<Model> models = modelService.list(queryWrapper);
        // 跳过第一个（已被 selectModel 选中）
        return models.stream().skip(1).collect(Collectors.toList());
    }

    @Override
    public String getStrategyType() {
        return RoutingStrategyTypeEnum.COST_FIRST.getValue();
    }

    /**
     * 构建基础查询条件：状态为启用且健康状态为健康或降级或未知
     */
    private QueryWrapper buildBaseQueryWrapper(String modelType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", ModelStatusEnum.ACTIVE.getValue())
                .in("healthStatus", HealthStatusEnum.HEALTHY.getValue(),
                        HealthStatusEnum.DEGRADED.getValue(), HealthStatusEnum.UNKNOWN.getValue());

        if (StrUtil.isNotBlank(modelType)) {
            queryWrapper.eq("modelType", modelType);
        }

        return queryWrapper;
    }
}
