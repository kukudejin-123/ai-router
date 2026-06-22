package com.kkdj.airouter.config;

import com.kkdj.airouter.mapper.UserMapper;
import com.kkdj.airouter.model.entity.User;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            User existing = userMapper.selectOneById(1L);
            if (existing != null) {
                log.info("数据库已有用户数据，跳过初始化");
                return;
            }

            String encodedPassword = passwordEncoder.encode("12345678");
            LocalDateTime now = LocalDateTime.now();

            User admin = new User();
            admin.setId(1L);
            admin.setUserAccount("admin");
            admin.setUserPassword(encodedPassword);
            admin.setUserName("管理员");
            admin.setUserRole("admin");
            admin.setUserStatus("active");
            admin.setUsedTokens(0L);
            admin.setTokenQuota(-1L);
            admin.setBalance(new java.math.BigDecimal("999999.0000"));
            admin.setIsDelete(0);
            admin.setEditTime(now);
            admin.setCreateTime(now);
            admin.setUpdateTime(now);
            userMapper.insert(admin);

            User normalUser = new User();
            normalUser.setId(2L);
            normalUser.setUserAccount("user");
            normalUser.setUserPassword(encodedPassword);
            normalUser.setUserName("普通用户");
            normalUser.setUserRole("user");
            normalUser.setUserStatus("active");
            normalUser.setUsedTokens(0L);
            normalUser.setTokenQuota(100000L);
            normalUser.setBalance(java.math.BigDecimal.ZERO);
            normalUser.setIsDelete(0);
            normalUser.setEditTime(now);
            normalUser.setCreateTime(now);
            normalUser.setUpdateTime(now);
            userMapper.insert(normalUser);

            User testUser = new User();
            testUser.setId(3L);
            testUser.setUserAccount("test");
            testUser.setUserPassword(encodedPassword);
            testUser.setUserName("测试账号");
            testUser.setUserRole("user");
            testUser.setUserStatus("active");
            testUser.setUsedTokens(0L);
            testUser.setTokenQuota(50000L);
            testUser.setBalance(java.math.BigDecimal.ZERO);
            testUser.setIsDelete(0);
            testUser.setEditTime(now);
            testUser.setCreateTime(now);
            testUser.setUpdateTime(now);
            userMapper.insert(testUser);

            log.info("默认用户创建完成（密码：12345678），共 3 个用户");
        } catch (Exception e) {
            log.warn("用户数据初始化跳过：{} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
