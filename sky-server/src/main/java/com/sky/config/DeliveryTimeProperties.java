package com.sky.config;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 集中维护服务端每日营业时段和配送准备时间
 */
@Data
@Component
@ConfigurationProperties(prefix = "sky.shop.delivery-time")
public class DeliveryTimeProperties {

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime openingTime = LocalTime.of(10, 0);
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime closingTime = LocalTime.of(22, 0);
    private String timeZone = "Asia/Shanghai";
    private int minimumPreparationMinutes = 30;

    /**
     * 校验营业时间配置，避免无效时段进入订单校验流程
     */
    @PostConstruct
    public void validate() {
        if (openingTime == null || closingTime == null || !openingTime.isBefore(closingTime)) {
            throw new IllegalStateException("店铺营业时间配置无效");
        }
        if (timeZone == null || timeZone.trim().isEmpty()) {
            throw new IllegalStateException("店铺时区配置不能为空");
        }
        ZoneId.of(timeZone);
        if (minimumPreparationMinutes < 0) {
            throw new IllegalStateException("配送准备时间配置不能为负数");
        }
        if (minimumPreparationMinutes > Duration.between(openingTime, closingTime).toMinutes()) {
            throw new IllegalStateException("配送准备时间超出店铺营业时段");
        }
    }

    /**
     * 获取配置的营业时区
     *
     * @return 解析后的时区
     */
    public ZoneId getZoneId() {
        return ZoneId.of(timeZone);
    }
}
