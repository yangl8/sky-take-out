package com.sky.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.convert.ApplicationConversionService;

import com.sky.config.DeliveryTimeProperties;

class DeliveryTimeValidationServiceTest {

    private TimeZone originalDefaultTimeZone;

    /**
     * 恢复测试前进程使用的默认时区
     */
    @AfterEach
    void restoreDefaultTimeZone() {
        if (originalDefaultTimeZone != null) {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    /**
     * 验证当前营业状态包含每日开门与关门边界
     */
    @Test
    void currentBusinessHoursIncludeOpeningAndClosingBoundaries() {
        assertCurrentBusinessHours("2026-10-10T01:59:59Z", false);
        assertCurrentBusinessHours("2026-10-10T02:00:00Z", true);
        assertCurrentBusinessHours("2026-10-10T02:30:00Z", true);
        assertCurrentBusinessHours("2026-10-10T13:59:59Z", true);
        assertCurrentBusinessHours("2026-10-10T14:00:00Z", true);
        assertCurrentBusinessHours("2026-10-10T14:00:01Z", false);
    }

    /**
     * 验证预约允许开门准备时间和闭店时间两个边界
     */
    @Test
    void scheduledDeliveryAllowsBothEarliestAndLatestBoundaries() {
        DeliveryTimeValidationService service = service("2026-10-10T01:59:00Z");

        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 10, 10, 30)));
        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 10, 22, 0)));
    }

    /**
     * 验证预约不得早于当前时刻加最短准备时间
     */
    @Test
    void scheduledDeliveryMustMeetCurrentPreparationTime() {
        DeliveryTimeValidationService service = service("2026-10-10T02:30:00Z");

        assertEquals(DeliveryTimeValidationResult.PREPARATION_TIME_NOT_MET,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED,
                        LocalDateTime.of(2026, 10, 10, 10, 59)));
        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED,
                        LocalDateTime.of(2026, 10, 10, 11, 0)));
    }

    /**
     * 验证预约不得早于对应营业日开门后的准备时间
     */
    @Test
    void scheduledDeliveryMustBeAtLeastThirtyMinutesAfterBusinessOpens() {
        DeliveryTimeValidationService service = service("2026-10-09T02:00:00Z");

        assertEquals(DeliveryTimeValidationResult.OUTSIDE_BUSINESS_HOURS,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 10, 10, 29)));
        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 10, 10, 30)));
    }

    /**
     * 验证当前打烊时仍允许下一个营业日的有效预约
     */
    @Test
    void closedNowStillAllowsNextDayAppointmentWithinBusinessHours() {
        DeliveryTimeValidationService service = service("2026-10-10T14:30:00Z");

        assertFalse(service.isCurrentlyWithinBusinessHours());
        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 11, 12, 0)));
        assertEquals(DeliveryTimeValidationResult.OUTSIDE_BUSINESS_HOURS,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 11, 8, 0)));
    }

    /**
     * 验证关门后的同日时间无效且跨日预约按次日时段判断
     */
    @Test
    void timeAfterClosingCannotRollIntoNextDayButNextBusinessDayIsValid() {
        DeliveryTimeValidationService service = service("2026-10-10T13:45:00Z");

        assertEquals(DeliveryTimeValidationResult.OUTSIDE_BUSINESS_HOURS,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 10, 22, 15)));
        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 11, 10, 30)));
    }

    /**
     * 验证预约日期跨午夜时按预约日期的营业时段校验
     */
    @Test
    void dateRolloverUsesTheRequestedBusinessDay() {
        DeliveryTimeValidationService service = service("2026-10-10T15:45:00Z");

        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 11, 10, 30)));
    }

    /**
     * 验证空预约时间、过去时间和无效配送类型均返回失败结果
     */
    @Test
    void scheduledDeliveryRejectsMissingPastAndInvalidTimes() {
        DeliveryTimeValidationService service = service("2026-10-10T02:00:00Z");

        assertEquals(DeliveryTimeValidationResult.DELIVERY_TIME_REQUIRED,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, null));
        assertEquals(DeliveryTimeValidationResult.PREPARATION_TIME_NOT_MET,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 10, 10, 0)));
        assertEquals(DeliveryTimeValidationResult.OUTSIDE_BUSINESS_HOURS,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 10, 22, 1)));
        assertEquals(DeliveryTimeValidationResult.INVALID_DELIVERY_STATUS,
                service.validate(2, LocalDateTime.of(2026, 10, 10, 12, 0)));
        assertEquals(DeliveryTimeValidationResult.INVALID_DELIVERY_STATUS,
                service.validate(null, LocalDateTime.of(2026, 10, 10, 12, 0)));
    }

    /**
     * 验证时间判断不受 JVM 默认时区影响且固定时钟结果稳定
     */
    @Test
    void timeZoneDoesNotDependOnJvmDefaultAndFixedClockIsDeterministic() {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        DeliveryTimeValidationService service = service("2026-10-10T02:00:00Z");

        assertTrue(service.isCurrentlyWithinBusinessHours());
        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED,
                        LocalDateTime.of(2026, 10, 10, 10, 30)));
    }

    /**
     * 验证营业时段配置无效时启动校验会失败
     */
    @Test
    void invalidBusinessTimeConfigurationIsRejected() {
        DeliveryTimeProperties properties = new DeliveryTimeProperties();
        properties.setOpeningTime(LocalTime.of(22, 0));
        properties.setClosingTime(LocalTime.of(10, 0));

        assertThrows(IllegalStateException.class, properties::validate);

        properties.setOpeningTime(LocalTime.of(10, 0));
        properties.setClosingTime(LocalTime.of(22, 0));
        properties.setMinimumPreparationMinutes(721);
        assertThrows(IllegalStateException.class, properties::validate);
    }

    /**
     * 验证 application.yml 中 HH:mm:ss 格式的营业时间可绑定为 LocalTime
     */
    @Test
    void yamlStyleBusinessTimesBindToLocalTime() {
        Map<String, String> values = new HashMap<>();
        values.put("sky.shop.delivery-time.opening-time", "10:00:00");
        values.put("sky.shop.delivery-time.closing-time", "22:00:00");
        values.put("sky.shop.delivery-time.time-zone", "Asia/Shanghai");
        values.put("sky.shop.delivery-time.minimum-preparation-minutes", "30");

        DeliveryTimeProperties properties = new Binder(Collections.singletonList(new MapConfigurationPropertySource(values)), null,
                ApplicationConversionService.getSharedInstance())
                .bind("sky.shop.delivery-time", Bindable.of(DeliveryTimeProperties.class))
                .get();

        assertEquals(LocalTime.of(10, 0), properties.getOpeningTime());
        assertEquals(LocalTime.of(22, 0), properties.getClosingTime());
        assertEquals("Asia/Shanghai", properties.getTimeZone());
        assertEquals(30, properties.getMinimumPreparationMinutes());
    }

    /**
     * 使用固定时钟断言当前是否处于营业时段
     *
     * @param instant 固定的 UTC 时刻
     * @param expected 预期营业状态
     */
    private void assertCurrentBusinessHours(String instant, boolean expected) {
        assertEquals(expected, service(instant).isCurrentlyWithinBusinessHours());
    }

    /**
     * 创建使用统一营业规则和固定时钟的校验服务
     *
     * @param instant 固定的 UTC 时刻
     * @return 配送时间校验服务
     */
    private DeliveryTimeValidationService service(String instant) {
        DeliveryTimeProperties properties = new DeliveryTimeProperties();
        properties.setOpeningTime(LocalTime.of(10, 0));
        properties.setClosingTime(LocalTime.of(22, 0));
        properties.setTimeZone("Asia/Shanghai");
        properties.setMinimumPreparationMinutes(30);
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new DeliveryTimeValidationService(properties, clock);
    }
}
