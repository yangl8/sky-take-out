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
public class UserReportVO implements Serializable {

    //Date values, comma-separated (for example: 2022-10-01,2022-10-02,2022-10-03)
    private String dateList;

    //Total user counts, comma-separated (for example: 200,210,220)
    private String totalUserList;

    //New user counts, comma-separated (for example: 20,21,10)
    private String newUserList;

}
