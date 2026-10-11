package com.sky.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.sky.config.DeliveryTimeProperties;

/**
 * 校验立即配送营业状态与预约配送时间
 */
@Service
public class DeliveryTimeValidationService {

    public static final int DELIVERY_STATUS_SCHEDULED = 0;
    public static final int DELIVERY_STATUS_NOW = 1;

    private final DeliveryTimeProperties properties;
    private final Clock clock;

    /**
     * 创建配送时间校验服务
     *
     * @param properties 每日营业时段及准备时间配置
     * @param clock 当前时间来源
     */
    public DeliveryTimeValidationService(DeliveryTimeProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 校验配送类型对应的当前营业状态或预约送达时间
     *
     * @param deliveryStatus 配送状态，1 表示立即配送，0 表示预约配送
     * @param estimatedDeliveryTime 预约送达时间
     * @return 配送时间校验结果
     */
    public DeliveryTimeValidationResult validate(Integer deliveryStatus, LocalDateTime estimatedDeliveryTime) {
        if (Integer.valueOf(DELIVERY_STATUS_NOW).equals(deliveryStatus)) {
            return isCurrentlyWithinBusinessHours()
                    ? DeliveryTimeValidationResult.VALID
                    : DeliveryTimeValidationResult.CURRENTLY_CLOSED;
        }
        if (!Integer.valueOf(DELIVERY_STATUS_SCHEDULED).equals(deliveryStatus)) {
            return DeliveryTimeValidationResult.INVALID_DELIVERY_STATUS;
        }
        if (estimatedDeliveryTime == null) {
            return DeliveryTimeValidationResult.DELIVERY_TIME_REQUIRED;
        }

        LocalDateTime now = currentTime();
        LocalDateTime earliestByPreparation = now.plusMinutes(properties.getMinimumPreparationMinutes());
        if (estimatedDeliveryTime.isBefore(earliestByPreparation)) {
            return DeliveryTimeValidationResult.PREPARATION_TIME_NOT_MET;
        }

        LocalTime earliestBusinessDelivery = properties.getOpeningTime()
                .plusMinutes(properties.getMinimumPreparationMinutes());
        LocalDateTime earliestByOpening = LocalDateTime.of(
                estimatedDeliveryTime.toLocalDate(), earliestBusinessDelivery);
        LocalDateTime latestByClosing = LocalDateTime.of(
                estimatedDeliveryTime.toLocalDate(), properties.getClosingTime());
        if (estimatedDeliveryTime.isBefore(earliestByOpening)
                || estimatedDeliveryTime.isAfter(latestByClosing)) {
            return DeliveryTimeValidationResult.OUTSIDE_BUSINESS_HOURS;
        }
        return DeliveryTimeValidationResult.VALID;
    }

    /**
     * 判断当前北京时间是否处于每日营业时段，起止边界均包含
     *
     * @return 当前时刻处于营业时段时返回 true
     */
    public boolean isCurrentlyWithinBusinessHours() {
        LocalTime currentTime = currentTime().toLocalTime();
        return !currentTime.isBefore(properties.getOpeningTime())
                && !currentTime.isAfter(properties.getClosingTime());
    }

    /**
     * 将注入时钟的当前时刻转换为配置时区的本地日期时间
     *
     * @return 配置时区下的当前日期时间
     */
    private LocalDateTime currentTime() {
        return LocalDateTime.ofInstant(clock.instant(), properties.getZoneId());
    }
}
