package com.kkdj.airouter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 主类（项目启动入口）

 */
@SpringBootApplication(exclude = {
        // 排除 DashScope 自动配置（使用适配器手动调用）
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration.class,
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration.class,
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration.class,
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeImageAutoConfiguration.class,
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioSpeechAutoConfiguration.class,
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAudioTranscriptionAutoConfiguration.class,
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeRerankAutoConfiguration.class,
        com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeVideoAutoConfiguration.class
})
@MapperScan("com.kkdj.airouter.mapper")
@EnableAsync
@EnableScheduling
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

}
