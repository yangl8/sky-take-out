package com.sky.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.sky.config.DeliveryTimeProperties;

/**
 * Validates current business hours and scheduled delivery times
 */
@Service
public class DeliveryTimeValidationService {

    public static final int DELIVERY_STATUS_SCHEDULED = 0;
    public static final int DELIVERY_STATUS_NOW = 1;

    private final DeliveryTimeProperties properties;
    private final Clock clock;

    /**
     * Creates the delivery time validator
     *
     * @param properties Daily business hours and preparation-time settings
     * @param clock Source of the current time
     */
    public DeliveryTimeValidationService(DeliveryTimeProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Validates the current opening status or the requested scheduled delivery time
     *
     * @param deliveryStatus Delivery mode: 1 for immediate delivery, 0 for scheduled delivery
     * @param estimatedDeliveryTime Requested scheduled delivery time
     * @return Validation result
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
     * Checks whether the current time falls within business hours, including both boundaries
     *
     * @return {@code true} when the current time is within business hours
     */
    public boolean isCurrentlyWithinBusinessHours() {
        LocalTime currentTime = currentTime().toLocalTime();
        return !currentTime.isBefore(properties.getOpeningTime())
                && !currentTime.isAfter(properties.getClosingTime());
    }

    /**
     * Converts the injected clock instant to the configured business time zone
     *
     * @return Current local date and time in the configured zone
     */
    private LocalDateTime currentTime() {
        return LocalDateTime.ofInstant(clock.instant(), properties.getZoneId());
    }
}
