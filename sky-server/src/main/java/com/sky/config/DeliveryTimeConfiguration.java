package com.sky.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 提供可注入的系统时钟，供时间规则使用并支持固定时钟测试
 */
@Configuration
public class DeliveryTimeConfiguration {

    /**
     * 创建不依赖服务器默认时区的系统时钟
     *
     * @return UTC 系统时钟，业务时区由配送时间配置显式转换
     */
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
