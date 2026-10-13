package com.sky.controller.admin;

import java.time.LocalDate;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController 
@RequestMapping("/admin/report")
@Api(tags = "Reporting endpoints")
@Slf4j 
public class ReportController {
    
    @Autowired 
    private ReportService reportService;
    
    @GetMapping("/turnoverStatistics")
    @ApiOperation("Turnover statistics")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, 
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("Turnover statistics: {}, {}", begin, end);
        return Result.success(reportService.getTurnoverStatistics(begin, end));
    }

    @GetMapping("/userStatistics")
    @ApiOperation("User statistics")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, 
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("User statistics: {}, {}", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));
    }

    @GetMapping("/ordersStatistics")
    @ApiOperation("Order statistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, 
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("Order statistics: {}, {}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin, end));
    }

    @GetMapping("/top10")
    @ApiOperation("Top 10 items by sales")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin, 
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("Top 10 sales: {}, {}", begin, end);
        return Result.success(reportService.getSalesTop10(begin, end));
    }

    @GetMapping("/export")
    @ApiOperation("Export business report")
    public void export(HttpServletResponse response){
        reportService.exportBusinessData(response);
    }
}
