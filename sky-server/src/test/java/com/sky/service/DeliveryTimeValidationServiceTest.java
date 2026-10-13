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
     * Restores the process default time zone after the test
     */
    @AfterEach
    void restoreDefaultTimeZone() {
        if (originalDefaultTimeZone != null) {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    /**
     * Verifies that opening and closing times are inclusive
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
     * Verifies scheduled deliveries at the opening-plus-preparation and closing boundaries
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
     * Verifies the minimum preparation time from now
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
     * Verifies the minimum preparation time from the scheduled business day opening
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
     * Verifies that a valid next-day booking is accepted while the shop is currently closed
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
     * Verifies same-day times after closing are rejected and next-day bookings use the next day schedule
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
     * Verifies bookings are checked against the business hours of their delivery date across midnight
     */
    @Test
    void dateRolloverUsesTheRequestedBusinessDay() {
        DeliveryTimeValidationService service = service("2026-10-10T15:45:00Z");

        assertEquals(DeliveryTimeValidationResult.VALID,
                service.validate(DeliveryTimeValidationService.DELIVERY_STATUS_SCHEDULED, LocalDateTime.of(2026, 10, 11, 10, 30)));
    }

    /**
     * Verifies missing or past booking times and unsupported delivery modes are rejected
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
     * Verifies time checks are independent of the JVM default zone and deterministic with a fixed clock
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
     * Verifies invalid business-hour configuration fails validation
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
     * Checks current business hours with a fixed clock
     *
     * @param instant Fixed UTC instant
     * @param expected Expected open status
     */
    private void assertCurrentBusinessHours(String instant, boolean expected) {
        assertEquals(expected, service(instant).isCurrentlyWithinBusinessHours());
    }

    /**
     * Creates a validator with shared business-hour rules and a fixed clock
     *
     * @param instant Fixed UTC instant
     * @return Delivery time validator
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
