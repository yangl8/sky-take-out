package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReportVO implements Serializable {

    //Date values, comma-separated (for example: 2022-10-01,2022-10-02,2022-10-03)
    private String dateList;

    //Daily order counts, comma-separated (for example: 260,210,215)
    private String orderCountList;

    //Daily completed order counts, comma-separated (for example: 20,21,10)
    private String validOrderCountList;

    private Integer totalOrderCount;

    private Integer validOrderCount;

    private Double orderCompletionRate;

}
