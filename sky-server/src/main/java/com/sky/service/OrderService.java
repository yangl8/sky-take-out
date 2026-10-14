package com.sky.service;

import java.util.List;
import java.util.Map;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * Creates an order from the current cart using current catalog data
     *
     * @param ordersSubmitDTO Delivery details for the order
     * @return The created order number and payment amount
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * Creates a payment request after checking that the order belongs to the current user
     *
     * @param ordersPaymentDTO Order number and ID to pay
     * @return Payment parameters for the client
     * @throws Exception If the payment provider request fails
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * Applies a successful payment callback using the callback timestamp and offset
     *
     * @param outTradeNo Merchant order number from the payment provider
     * @param successTime Payment success timestamp and offset from the callback
     */
    void paySuccess(String outTradeNo, java.time.OffsetDateTime successTime);

    /**
     * Applies a development payment only to an order owned by the current user
     *
     * @param orderNumber Order number to mark as paid
     * @param orderId ID of the order to mark as paid
     */
    void mockPaySuccessForUser(String orderNumber, Long orderId);

    /**
     * Returns a page of orders for the current user, optionally filtered by order and payment status
     *
     * @param page Page number
     * @param pageSize Number of orders per page
     * @param status Optional order status
     * @param payStatus Optional payment status
     * @return The matching page of orders
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status, Integer payStatus);

    /**
     * Retrieves current-month sales for the requested dishes
     *
     * @param dishIds Dish IDs
     * @return Sales counts keyed by dish ID
     */
    Map<Long, Integer> getDishMonthlySales(List<Long> dishIds);

    /**
     * Retrieves current-month sales for the requested set meals
     *
     * @param setmealIds Set-meal IDs
     * @return Sales counts keyed by set-meal ID
     */
    Map<Long, Integer> getSetmealMonthlySales(List<Long> setmealIds);

    /**
     * Retrieves order details for administrative use
     *
     * @param id Order ID
     * @return Order details
     */
    OrderVO details(Long id);

    /**
     * Returns details only when the order belongs to the current user
     *
     * @param id Order ID
     * @return Order details when the order belongs to the current user
     */
    OrderVO detailsForUser(Long id);

    /**
     * Cancels an eligible unpaid order owned by the current user
     *
     * @param id Order ID to cancel
     * @throws Exception If a required refund operation fails
     */
    void userCancelById(Long id) throws Exception;

    /**
     * Adds the items from an order to the current user's shopping cart
     *
     * @param id Order ID to repeat
     */
    void repetition(Long id);

    /**
     * Searches orders using administrative filters
     *
     * @param ordersPageQueryDTO Pagination and order filters
     * @return The matching page
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * Counts orders awaiting confirmation, delivery, or completion
     *
     * @return Current order counts by workflow status
     */
    OrderStatisticsVO statistics();

    /**
     * Accepts an order that is awaiting confirmation
     *
     * @param ordersConfirmDTO Confirmation request containing the order ID
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * Rejects an eligible order and refunds it when required
     *
     * @param ordersRejectionDTO Order ID and rejection reason
     * @throws Exception If a required refund operation fails
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * Cancels an eligible order on behalf of the merchant and refunds it when required
     *
     * @param ordersCancelDTO Order ID and cancellation reason
     * @throws Exception If a required refund operation fails
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * Advances an accepted order to delivery
     *
     * @param id Order ID to dispatch
     */
    void delivery(Long id);

    /**
     * Completes an order that is currently in delivery
     *
     * @param id Order ID to complete
     */
    void complete(Long id);

    /**
     * Sends an update reminder for an order owned by the current user
     *
     * @param id Order ID to remind the merchant about
     */
    void reminder(Long id);
}
