package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import java.time.LocalDateTime;

public interface WorkspaceService {

    /**
     * Aggregates completed-order turnover, order completion, average order value, and new-user metrics for the requested time range
     *
     * @param begin Start of the time range
     * @param end End of the time range
     * @return Turnover, order, and user metrics for the requested time range
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * Counts orders by workflow status for the workspace overview
     *
     * @return Order counts by status
     */
    OrderOverViewVO getOrderOverView();

    /**
     * Counts dishes by sale status for the workspace overview
     *
     * @return Dish counts by status
     */
    DishOverViewVO getDishOverView();

    /**
     * Counts set meals by sale status for the workspace overview
     *
     * @return Set-meal counts by status
     */
    SetmealOverViewVO getSetmealOverView();

}
