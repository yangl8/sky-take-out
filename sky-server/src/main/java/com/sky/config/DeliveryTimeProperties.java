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
 * Centralizes daily business hours and delivery preparation time
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
     * Validates business-hour settings before they are used to check orders
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
     * Returns the configured business time zone
     *
     * @return Parsed time zone
     */
    public ZoneId getZoneId() {
        return ZoneId.of(timeZone);
    }
}
