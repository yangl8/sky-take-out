package com.sky.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /** Looks up an order number within the current user's ownership scope
     *
     * @param orderNumber Merchant order number
     * @param userId Current user ID
     * @return The order when its number belongs to the specified user
     */
    @Select("select * from orders where number = #{orderNumber} and user_id = #{userId}")
    Orders getByNumberAndUserId(@Param("orderNumber") String orderNumber, @Param("userId") Long userId);

    /**
     * Updates an order only when its current state matches the expected state
     *
     * @param id Order ID
     * @param expectedStatus Expected current order status
     * @param expectedPayStatus Expected current payment status
     * @param orderTimeBefore Order-time cutoff for timeout cancellation; null for other transitions
     * @param targetOrder Target state and order fields to persist
     * @return Number of rows updated
     */
    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("expectedStatus") Integer expectedStatus,
                              @Param("expectedPayStatus") Integer expectedPayStatus,
                              @Param("orderTimeBefore") LocalDateTime orderTimeBefore,
                              @Param("targetOrder") Orders targetOrder);

    /**
     * Sorts delivery orders by estimated delivery time and other orders by creation time
     *
     * @param ordersPageQueryDTO Order search and pagination criteria
     * @return The requested page of matching orders
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /** Queries by order ID and user ID to restrict access to the current user's orders
     * @param id Order ID
     * @param userId Current user ID
     * @return The order when it belongs to the specified user
     */
    @Select("select * from orders where id = #{id} and user_id = #{userId}")
    Orders getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * Finds orders in a given state that were created before the cutoff time
     *
     * @param status Order status to match
     * @param orderTime Cutoff order time
     * @return Orders matching the status and cutoff
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List <Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * Sums order amounts using optional exclusive time bounds and an optional status filter
     *
     * @param map Optional time bounds and order status criteria
     * @return Sum of matching order amounts
     */
    Double sumByMap(Map map);

    /**
     * Counts orders using optional exclusive time bounds and an optional status filter
     *
     * @param map Optional time bounds and order status criteria
     * @return Number of matching orders
     */
    Integer countByMap(Map map);

    /**
     * Returns the top ten sold items from completed orders in the requested time range
     *
     * @param begin Inclusive start of the reporting period
     * @param end Exclusive end of the reporting period
     * @return Up to ten items ranked by sales
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
} 
