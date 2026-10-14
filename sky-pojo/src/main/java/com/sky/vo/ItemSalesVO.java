package com.sky.vo;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemSalesVO implements Serializable {

    private Long itemId;

    private Integer monthlySales;
}
