package com.sky.service;

public enum DeliveryTimeValidationResult {
    VALID,
    INVALID_DELIVERY_STATUS,
    DELIVERY_TIME_REQUIRED,
    PREPARATION_TIME_NOT_MET,
    OUTSIDE_BUSINESS_HOURS,
    CURRENTLY_CLOSED
}
