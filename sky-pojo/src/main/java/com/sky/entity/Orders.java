package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_CONFIRMED = 2;
    public static final Integer CONFIRMED = 3;
    public static final Integer DELIVERY_IN_PROGRESS = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;

    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    private String number;

    /// Order status: 1 pending payment, 2 pending acceptance, 3 accepted, 4 out for delivery, 5 completed, 6 cancelled, 7 refunded
    private Integer status;

    private Long userId;

    private Long addressBookId;

    private LocalDateTime orderTime;

    private LocalDateTime checkoutTime;

    /// Payment method: 1 WeChat Pay, 2 Alipay
    private Integer payMethod;

    /// Payment status: 0 unpaid, 1 paid, 2 refunded
    private Integer payStatus;

    private BigDecimal amount;

    private String remark;

    private String userName;

    private String phone;

    private String address;

    private String consignee;

    private String cancelReason;

    private String rejectionReason;

    private LocalDateTime cancelTime;

    private LocalDateTime estimatedDeliveryTime;

    /// Delivery mode: 1 immediate, 0 scheduled
    private Integer deliveryStatus;

    private LocalDateTime deliveryTime;

    private int packAmount;

    private int tablewareNumber;

    /// Tableware quantity mode: 1 match meal quantity, 0 specify quantity
    private Integer tablewareStatus;
}
