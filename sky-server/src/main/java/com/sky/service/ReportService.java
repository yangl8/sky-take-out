package com.sky.service;

import java.time.LocalDate;

import javax.servlet.http.HttpServletResponse;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

public interface ReportService {
    
    /**
     * Aggregates turnover from completed orders by day for the requested date range
     *
     * @param begin First date, inclusive
     * @param end Last date, inclusive
     * @return Daily turnover values
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * Aggregates new and total users by day for the requested date range
     *
     * @param begin First date, inclusive
     * @param end Last date, inclusive
     * @return Daily user counts
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * Aggregates total and completed orders by day for the requested date range
     *
     * @param begin First date, inclusive
     * @param end Last date, inclusive
     * @return Daily order counts and completion rates
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * Ranks the ten best-selling dishes for the requested date range
     *
     * @param begin First date, inclusive
     * @param end Last date, inclusive
     * @return Top-selling dish names and quantities
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * Exports the recent business summary using the configured workbook template
     *
     * @param response HTTP response used to deliver the workbook
     */
    void exportBusinessData(HttpServletResponse response);
}
