package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrdersSubmitDTO implements Serializable {
    private Long addressBookId;
    private int payMethod;
    private String remark;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedDeliveryTime;
    /// Delivery mode: 1 immediate, 0 scheduled
    private Integer deliveryStatus;
    private Integer tablewareNumber;
    /// Tableware quantity mode: 1 match meal quantity, 0 specify quantity
    private Integer tablewareStatus;
    private Integer packAmount;
    private BigDecimal amount;
}
