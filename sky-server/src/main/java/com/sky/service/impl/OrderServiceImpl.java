package com.sky.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.ShopStatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Dish;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.DeliveryTimeValidationResult;
import com.sky.service.DeliveryTimeValidationService;
import com.sky.constant.StatusConstant;
import com.sky.config.DeliveryTimeProperties;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.vo.ItemSalesVO;
import com.sky.websocket.WebSocketServer;

import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j
public class OrderServiceImpl implements OrderService{

    @Autowired 
    private OrderMapper orderMapper;
    @Autowired 
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private Environment environment;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DeliveryTimeValidationService deliveryTimeValidationService;

    @Autowired
    private DeliveryTimeProperties deliveryTimeProperties;

    @Autowired
    private Clock clock;

    @Value("${sky.shop.delivery-fee:0}")
    private BigDecimal deliveryFee;

    private static final BigDecimal PACKING_FEE_PER_ITEM = BigDecimal.ONE;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.amap.key}")
    private String amapKey;

    /**
     * Requests an order refund
     *
     * @param currentOrder Order that has been paid
     * @throws Exception WeChat refund request fails
     */
    private void refundPaidOrder(Orders currentOrder) throws Exception {
        if (environment.acceptsProfiles(Profiles.of("dev & !prod"))) {
            log.info("Development refund simulation succeeded for order {}", currentOrder.getNumber());
            return;
        }
        String refund = weChatPayUtil.refund(
                currentOrder.getNumber(),
                currentOrder.getNumber(),
                new BigDecimal(0.01),
                new BigDecimal(0.01));
        log.info("Requesting refund: {}", refund);
    }

    /**
     * Refunds a paid order
     *
     * @param currentOrder Order that has been paid
     * @throws OrderBusinessException Refund failure that requires rolling back the local cancellation
     */
    private void refundPaidOrderOrFail(Orders currentOrder) {
        try {
            refundPaidOrder(currentOrder);
        } catch (Exception exception) {
            log.error("Refund failed for order {}", currentOrder.getNumber(), exception);
            throw new OrderBusinessException("退款失败，请检查微信支付配置后重试");
        }
    }

    /**
     * Creates an order from the current user's cart and calculates its amount from current catalog prices
     *
     * @param ordersSubmitDTO Delivery details and requested delivery time
     * @return Created order number and server-calculated payment amount
     */
    @Transactional 
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO){

        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookMapper.getByIdAndUserId(
                ordersSubmitDTO.getAddressBookId(), userId);
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        validateDeliveryRules(ordersSubmitDTO);

        checkOutOfRange(addressBook);

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        List<OrderDetail> orderDetailList = buildOrderDetailsWithCurrentPrices(shoppingCartList);
        BigDecimal goodsAmount = BigDecimal.ZERO;
        long totalItemCount = 0;
        for (OrderDetail orderDetail : orderDetailList) {
            goodsAmount = goodsAmount.add(orderDetail.getAmount()
                    .multiply(BigDecimal.valueOf(orderDetail.getNumber())));
            if (totalItemCount > Integer.MAX_VALUE - (long) orderDetail.getNumber()) {
                throw new OrderBusinessException("订单商品数量超出可处理范围");
            }
            totalItemCount += orderDetail.getNumber();
        }
        if (deliveryFee == null || deliveryFee.signum() < 0) {
            throw new OrderBusinessException("配送费配置无效，无法下单");
        }
        int serverPackAmount = (int) totalItemCount;
        BigDecimal packingAmount = PACKING_FEE_PER_ITEM.multiply(BigDecimal.valueOf(totalItemCount));
        BigDecimal serverOrderAmount = goodsAmount.add(packingAmount).add(deliveryFee);
        
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders, "amount", "packAmount");
        orders.setAmount(serverOrderAmount);
        orders.setPackAmount(serverPackAmount);
        orders.setOrderTime(currentServerLocalTime());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(buildDeliveryAddress(addressBook));
        orders.setUserId(userId);

        orderMapper.insert(orders);

        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orders.getId());
        }

        orderDetailMapper.insertBatch(orderDetailList);

        shoppingCartMapper.deleteByUserId(userId);

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                        .id(orders.getId())
                        .orderTime(orders.getOrderTime())
                        .orderNumber(orders.getNumber())
                        .orderAmount(orders.getAmount())
                        .build();

        return orderSubmitVO;
    }

    /**
     * Validates the delivery mode, scheduled time, and shop status for immediate delivery
     *
     * @param ordersSubmitDTO Delivery details submitted by the user
     * @throws OrderBusinessException Invalid delivery details, closed business hours, or unavailable shop status
     */
    private void validateDeliveryRules(OrdersSubmitDTO ordersSubmitDTO) {
        DeliveryTimeValidationResult result = deliveryTimeValidationService.validate(
                ordersSubmitDTO.getDeliveryStatus(), ordersSubmitDTO.getEstimatedDeliveryTime());
        switch (result) {
            case INVALID_DELIVERY_STATUS:
                throw new OrderBusinessException(MessageConstant.DELIVERY_TYPE_INVALID);
            case DELIVERY_TIME_REQUIRED:
                throw new OrderBusinessException(MessageConstant.DELIVERY_TIME_REQUIRED);
            case PREPARATION_TIME_NOT_MET:
                throw new OrderBusinessException(MessageConstant.DELIVERY_PREPARATION_TIME_NOT_MET);
            case OUTSIDE_BUSINESS_HOURS:
                throw new OrderBusinessException(MessageConstant.DELIVERY_TIME_OUTSIDE_BUSINESS_HOURS);
            case CURRENTLY_CLOSED:
                throw new OrderBusinessException(MessageConstant.SHOP_CLOSED_FOR_IMMEDIATE_DELIVERY);
            case VALID:
                break;
            default:
                throw new OrderBusinessException(MessageConstant.DELIVERY_TYPE_INVALID);
        }

        if (!Integer.valueOf(DeliveryTimeValidationService.DELIVERY_STATUS_NOW)
                .equals(ordersSubmitDTO.getDeliveryStatus())) {
            return;
        }

        Object shopStatus;
        try {
            shopStatus = redisTemplate.opsForValue().get(ShopStatusConstant.KEY);
        } catch (RuntimeException exception) {
            log.error("Could not read shop status; rejecting immediate-delivery order creation", exception);
            throw new OrderBusinessException(MessageConstant.SHOP_STATUS_UNAVAILABLE);
        }
        if (!Integer.valueOf(1).equals(shopStatus)) {
            throw new OrderBusinessException(MessageConstant.SHOP_CLOSED_FOR_IMMEDIATE_DELIVERY);
        }
    }

    private List<OrderDetail> buildOrderDetailsWithCurrentPrices(List<ShoppingCart> shoppingCartList) {
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cartItem : shoppingCartList) {
            if (cartItem.getNumber() == null || cartItem.getNumber() <= 0) {
                throw new OrderBusinessException("购物车商品数量必须大于零");
            }

            boolean dishItem = cartItem.getDishId() != null;
            boolean setmealItem = cartItem.getSetmealId() != null;
            if (dishItem == setmealItem) {
                throw new OrderBusinessException("购物车商品信息无效，请刷新后重试");
            }

            String itemName;
            String itemImage;
            BigDecimal currentPrice;
            if (dishItem) {
                Dish currentDish = dishMapper.getById(cartItem.getDishId());
                if (currentDish == null || !StatusConstant.ENABLE.equals(currentDish.getStatus())) {
                    throw new OrderBusinessException("菜品不存在或已停售，请刷新后重试");
                }
                itemName = currentDish.getName();
                itemImage = currentDish.getImage();
                currentPrice = currentDish.getPrice();
            } else {
                Setmeal currentSetmeal = setmealMapper.getById(cartItem.getSetmealId());
                if (currentSetmeal == null || !StatusConstant.ENABLE.equals(currentSetmeal.getStatus())) {
                    throw new OrderBusinessException("套餐不存在或已停售，请刷新后重试");
                }
                itemName = currentSetmeal.getName();
                itemImage = currentSetmeal.getImage();
                currentPrice = currentSetmeal.getPrice();
            }

            if (currentPrice == null || currentPrice.signum() < 0) {
                throw new OrderBusinessException("商品价格无效，无法下单");
            }

            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cartItem, orderDetail, "id", "orderId", "name", "image", "amount");
            orderDetail.setName(itemName);
            orderDetail.setImage(itemImage);
            orderDetail.setAmount(currentPrice);
            orderDetailList.add(orderDetail);
        }
        return orderDetailList;
    }

    /**
     * Creates a payment request for an order
     *
     * @param ordersPaymentDTO Order number and ID to pay
     * @return Payment parameters for the client
     * @throws Exception If the payment provider request fails
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getByNumberAndUserId(ordersPaymentDTO.getOrderNumber(), userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        validatePaymentEligibility(order);
        User user = userMapper.getById(userId);

        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(),
                new BigDecimal(0.01),
                "苍穹外卖订单",
                user.getOpenid()
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * Processes a successful WeChat payment callback
     *
     * @param outTradeNo Merchant order number from the WeChat callback
     * @param successTime Payment success time, including its offset, from the WeChat callback
     * @throws OrderBusinessException Order is missing or cannot transition to paid
     */
    public void paySuccess(String outTradeNo, OffsetDateTime successTime) {

        Orders currentOrder = orderMapper.getByNumber(outTradeNo);
        if (currentOrder == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (successTime == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        LocalDateTime paidAt = LocalDateTime.ofInstant(successTime.toInstant(), deliveryTimeProperties.getZoneId());
        processPaySuccess(currentOrder, paidAt, true);
    }

    public void mockPaySuccessForUser(String orderNumber, Long orderId) {
        Orders currentOrder = orderMapper.getByIdAndUserId(orderId, BaseContext.getCurrentId());
        if (currentOrder == null || !Objects.equals(currentOrder.getNumber(), orderNumber)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        validatePaymentEligibility(currentOrder);
        processPaySuccess(currentOrder, currentBusinessTime(), false);
    }

    /**
     * Transitions an order after successful payment
     *
     * @param currentOrder Order state read before the update
     * @param paidAt Trusted payment time used for checkout and immediate-delivery ETA
     * @param paymentCallback Whether this is a WeChat callback, which determines handling when a competing transition wins
     * @return {@code true} when this call performs the transition; {@code false} when it is repeated or a competing state change wins
     * @throws OrderBusinessException If the stored order state or payment timestamp is invalid
     */
    private boolean processPaySuccess(Orders currentOrder, LocalDateTime paidAt, boolean paymentCallback) {
        if (Orders.CANCELLED.equals(currentOrder.getStatus())) {
            log.error("Received a successful payment callback for cancelled order {}; funds require reconciliation", currentOrder.getNumber());
            if (paymentCallback) {
                return false;
            }
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        if (isPaidOrderInProgress(currentOrder)) {
            return false;
        }

        if (!Orders.PENDING_PAYMENT.equals(currentOrder.getStatus())
                || !Orders.UN_PAID.equals(currentOrder.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        if (paidAt == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders targetOrder = Orders.builder()
                .id(currentOrder.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(paidAt)
                .build();
        if (Integer.valueOf(DeliveryTimeValidationService.DELIVERY_STATUS_NOW)
                .equals(currentOrder.getDeliveryStatus())) {
            targetOrder.setEstimatedDeliveryTime(paidAt.plusMinutes(
                    deliveryTimeProperties.getMinimumPreparationMinutes()));
        }

        int updatedRows = updateOrderState(currentOrder, targetOrder);
        if (updatedRows != 1) {
            Orders latestOrder = orderMapper.getById(currentOrder.getId());
            if (isPaidOrderInProgress(latestOrder)) {
                return false;
            }
            if (latestOrder != null && Orders.CANCELLED.equals(latestOrder.getStatus())) {
                log.error("Payment and cancellation raced for order {}; cancellation prevailed, funds require reconciliation",
                        currentOrder.getNumber());
                if (paymentCallback) {
                    return false;
                }
            }
            if (paymentCallback) {
                log.warn("Payment callback could not update order {}; its state has changed", currentOrder.getNumber());
                return false;
            }
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // Broadcast the new order only after the conditional database update succeeds
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId", currentOrder.getId());
        map.put("orderNumber", currentOrder.getNumber());
        map.put("content", "订单号: " + currentOrder.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        return true;
    }

    private void validatePaymentEligibility(Orders order) {
        if (!Orders.PENDING_PAYMENT.equals(order.getStatus())
                || !Orders.UN_PAID.equals(order.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_PAYMENT_NOT_ALLOWED);
        }
        if (order.getOrderTime() == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_PAYMENT_EXPIRED);
        }
        // order_time is stored in the JVM local zone by the existing order flow; interpret it in the same zone when checking the payment deadline
        Instant orderCreatedAt = order.getOrderTime().atZone(ZoneId.systemDefault()).toInstant();
        if (clock.instant().isAfter(orderCreatedAt.plusSeconds(15 * 60L))) {
            throw new OrderBusinessException(MessageConstant.ORDER_PAYMENT_EXPIRED);
        }

        DeliveryTimeValidationResult result = deliveryTimeValidationService.validate(
                order.getDeliveryStatus(), order.getEstimatedDeliveryTime());
        switch (result) {
            case VALID:
                break;
            case CURRENTLY_CLOSED:
                throw new OrderBusinessException(MessageConstant.SHOP_CLOSED_FOR_IMMEDIATE_PAYMENT);
            case PREPARATION_TIME_NOT_MET:
                throw new OrderBusinessException(MessageConstant.SCHEDULED_ORDER_PREPARATION_TIME_NOT_MET);
            case DELIVERY_TIME_REQUIRED:
                throw new OrderBusinessException(MessageConstant.DELIVERY_TIME_REQUIRED);
            case OUTSIDE_BUSINESS_HOURS:
                throw new OrderBusinessException(MessageConstant.DELIVERY_TIME_OUTSIDE_BUSINESS_HOURS);
            default:
                throw new OrderBusinessException(MessageConstant.DELIVERY_TYPE_INVALID);
        }

        if (!Integer.valueOf(DeliveryTimeValidationService.DELIVERY_STATUS_NOW)
                .equals(order.getDeliveryStatus())) {
            return;
        }
        Object shopStatus;
        try {
            shopStatus = redisTemplate.opsForValue().get(ShopStatusConstant.KEY);
        } catch (RuntimeException exception) {
            log.error("Could not read shop status; rejecting immediate-delivery payment", exception);
            throw new OrderBusinessException(MessageConstant.SHOP_STATUS_UNAVAILABLE);
        }
        if (!Integer.valueOf(1).equals(shopStatus)) {
            throw new OrderBusinessException(MessageConstant.SHOP_CLOSED_FOR_IMMEDIATE_PAYMENT);
        }
    }

    /**
     * Returns the current time in the configured business zone
     *
     * @return Current date and time in the business zone
     */
    private LocalDateTime currentBusinessTime() {
        return LocalDateTime.ofInstant(clock.instant(), deliveryTimeProperties.getZoneId());
    }

    /**
     * Returns server-local time to match the existing timezone-free order field
     *
     * @return Current date and time in the JVM default zone
     */
    private LocalDateTime currentServerLocalTime() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }

    /**
     * Checks whether an order has reached a post-payment state
     *
     * @param currentOrder Current order
     * @return Returns true when the order is paid and between pending acceptance and completed
     */
    private boolean isPaidOrderInProgress(Orders currentOrder) {
        if (currentOrder == null || !Orders.PAID.equals(currentOrder.getPayStatus())
                || currentOrder.getStatus() == null) {
            return false;
        }
        return currentOrder.getStatus() >= Orders.TO_BE_CONFIRMED
                && currentOrder.getStatus() <= Orders.COMPLETED;
    }

    /**
     * Updates an order state
     *
     * @param expectedCurrentOrder Expected current order and payment statuses
     * @param targetOrder Target order state and related fields
     * @return Number of rows updated
     */
    private int updateOrderState(Orders expectedCurrentOrder, Orders targetOrder) {
        return orderMapper.updateStatusIfCurrent(expectedCurrentOrder.getId(), expectedCurrentOrder.getStatus(),
                expectedCurrentOrder.getPayStatus(), null, targetOrder);
    }

    /**
     * Applies a conditional state update so concurrent requests cannot overwrite a newer order state
     *
     * @param expectedCurrentOrder Expected current order and payment statuses
     * @param targetOrder Target order state
     */
    private void requireStateTransition(Orders expectedCurrentOrder, Orders targetOrder) {
        if (updateOrderState(expectedCurrentOrder, targetOrder) != 1) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * Returns a page of orders for the current user
     *
     * @param pageNum Page number
     * @param pageSize Page size
     * @param status Order status filter
     * @param payStatus Payment status filter
     * @return Page total and current-page orders
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status, Integer payStatus) {
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setPayStatus(payStatus);

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        long total = page == null ? 0 : page.getTotal();
        if (page != null && !page.isEmpty()) {
            List<Long> orderIds = new ArrayList<>();
            for (Orders orders : page) {
                orderIds.add(orders.getId());
            }
            List<OrderDetail> details = orderDetailMapper.getByOrderIds(orderIds);
            Map<Long, List<OrderDetail>> detailsByOrderId = new HashMap<>();
            for (OrderDetail detail : details) {
                detailsByOrderId.computeIfAbsent(detail.getOrderId(), key -> new ArrayList<>()).add(detail);
            }

            for (Orders orders : page) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetails = detailsByOrderId.get(orders.getId());
                orderVO.setOrderDetailList(orderDetails == null
                        ? new ArrayList<>() : new ArrayList<>(orderDetails));
                orderVO.setDeliveryFee(deliveryFee);
                list.add(orderVO);
            }
        }
        return new PageResult(total, list);
    }

    /**
     * Returns current-month sales for the specified dishes
     * @param dishIds Dish IDs
     * @return Mapping of dish IDs to current-month sales
     */
    public Map<Long, Integer> getDishMonthlySales(List<Long> dishIds) {
        return getMonthlySales(dishIds, true);
    }

    /**
     * Returns current-month sales for the specified set meals
     * @param setmealIds Set-meal IDs
     * @return Mapping of set-meal IDs to current-month sales
     */
    public Map<Long, Integer> getSetmealMonthlySales(List<Long> setmealIds) {
        return getMonthlySales(setmealIds, false);
    }

    private Map<Long, Integer> getMonthlySales(List<Long> itemIds, boolean dish) {
        if (CollectionUtils.isEmpty(itemIds)) {
            return new HashMap<>();
        }

        LocalDateTime begin = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = begin.plusMonths(1);
        List<ItemSalesVO> sales = dish
                ? orderDetailMapper.getDishMonthlySales(itemIds, begin, end)
                : orderDetailMapper.getSetmealMonthlySales(itemIds, begin, end);

        return sales.stream().collect(Collectors.toMap(
                ItemSalesVO::getItemId,
                ItemSalesVO::getMonthlySales
        ));
    }

    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        return toOrderVO(orders, null);
    }

    /**
     * {@inheritDoc}
     */
    public OrderVO detailsForUser(Long id) {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        return toOrderVO(orders, userId);
    }

    private OrderVO toOrderVO(Orders orders, Long userId) {

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        orderVO.setDeliveryFee(deliveryFee);

        // Older orders may not have persisted the delivery address. Rebuild it
        // from the address book so the management detail view can still show it.
        if ((orderVO.getAddress() == null || orderVO.getAddress().trim().isEmpty())
                && orderVO.getAddressBookId() != null) {
            AddressBook addressBook = userId == null
                    ? addressBookMapper.getById(orderVO.getAddressBookId())
                    : addressBookMapper.getByIdAndUserId(orderVO.getAddressBookId(), userId);
            if (addressBook != null) {
                orderVO.setAddress(buildDeliveryAddress(addressBook));
            }
        }

        return orderVO;
    }

    private String buildDeliveryAddress(AddressBook addressBook) {
        return valueOrEmpty(addressBook.getProvinceName())
                + valueOrEmpty(addressBook.getCityName())
                + valueOrEmpty(addressBook.getDistrictName())
                + valueOrEmpty(addressBook.getDetail());
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Cancels an order for the current user
     *
     * @param id Order ID to cancel for the current user
     * @throws Exception Order refund operation fails
     */
    @Transactional(rollbackFor = Exception.class)
    public void userCancelById(Long id) throws Exception {
        Orders currentOrder = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());

        if (currentOrder == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        boolean pendingUnpaid = Orders.PENDING_PAYMENT.equals(currentOrder.getStatus())
                && Orders.UN_PAID.equals(currentOrder.getPayStatus());
        boolean acceptedPaid = Orders.TO_BE_CONFIRMED.equals(currentOrder.getStatus())
                && Orders.PAID.equals(currentOrder.getPayStatus());
        if (!pendingUnpaid && !acceptedPaid) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders targetOrder = Orders.builder()
                .id(currentOrder.getId())
                .status(Orders.CANCELLED)
                .payStatus(acceptedPaid ? Orders.REFUND : Orders.UN_PAID)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();
        requireStateTransition(currentOrder, targetOrder);

        if (acceptedPaid) {
            refundPaidOrderOrFail(currentOrder);
        }
    }

    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        Orders orders = orderMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * Searches orders
     *
     * @param ordersPageQueryDTO Order search and pagination criteria
     * @return The requested page of matching orders
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * Builds the item summary for an order
     *
     * @param orders Order whose item names and quantities are summarized
     * @return Semicolon-separated item names and quantities for the order
     */
    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        return String.join("", orderDishList);
    }

    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders expectedCurrentOrder = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .build();
        Orders targetOrder = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        requireStateTransition(expectedCurrentOrder, targetOrder);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders currentOrder = orderMapper.getById(ordersRejectionDTO.getId());

        if (currentOrder == null
                || !Orders.TO_BE_CONFIRMED.equals(currentOrder.getStatus())
                || !Orders.PAID.equals(currentOrder.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders targetOrder = Orders.builder()
                .id(currentOrder.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        requireStateTransition(currentOrder, targetOrder);
        refundPaidOrderOrFail(currentOrder);
    }

    /**
     * Cancels an order
     *
     * @param ordersCancelDTO Request containing the order ID and cancellation reason
     * @throws Exception Order refund operation fails
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders currentOrder = orderMapper.getById(ordersCancelDTO.getId());
        if (currentOrder == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        boolean pendingUnpaid = Orders.PENDING_PAYMENT.equals(currentOrder.getStatus())
                && Orders.UN_PAID.equals(currentOrder.getPayStatus());
        boolean paidCancelable = (Orders.CONFIRMED.equals(currentOrder.getStatus())
                || Orders.DELIVERY_IN_PROGRESS.equals(currentOrder.getStatus())
                || Orders.COMPLETED.equals(currentOrder.getStatus()))
                && Orders.PAID.equals(currentOrder.getPayStatus());
        if (!pendingUnpaid && !paidCancelable) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders targetOrder = Orders.builder()
                .id(currentOrder.getId())
                .status(Orders.CANCELLED)
                .payStatus(paidCancelable ? Orders.REFUND : Orders.UN_PAID)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        requireStateTransition(currentOrder, targetOrder);
        if (paidCancelable) {
            refundPaidOrderOrFail(currentOrder);
        }
    }

    /**
     * Starts order delivery
     *
     * @param id Order ID to start delivering
     */
    public void delivery(Long id) {
        Orders expectedCurrentOrder = Orders.builder()
                .id(id)
                .status(Orders.CONFIRMED)
                .payStatus(Orders.PAID)
                .build();
        Orders targetOrder = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        requireStateTransition(expectedCurrentOrder, targetOrder);
    }

    /**
     * Completes an order
     *
     * @param id Order ID to mark as completed
     */
    public void complete(Long id) {
        Orders expectedCurrentOrder = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .payStatus(Orders.PAID)
                .build();
        Orders targetOrder = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        requireStateTransition(expectedCurrentOrder, targetOrder);
    }

    void checkOutOfRange(AddressBook addressBook) {
        String deliveryAddress = DeliveryLocationValidator.buildGeocodingAddress(addressBook);
        String shopCoordinate = geocodeAddress(shopAddress, shopAddress, null, "店铺地址");
        String userCoordinate = geocodeAddress(
                deliveryAddress, addressBook.getDetail(), addressBook.getDistrictCode(), "收货地址");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", amapKey);
        parameters.put("origin", shopCoordinate);
        parameters.put("destination", userCoordinate);
        String response;
        try {
            response = requestAmap("https://restapi.amap.com/v5/direction/driving", parameters);
        } catch (RuntimeException exception) {
            log.error("AMap driving-route request failed", exception);
            throw new OrderBusinessException("配送路线服务暂不可用");
        }
        BigDecimal distance = DeliveryLocationValidator.resolveDrivingDistance(response, shopCoordinate, userCoordinate);
        DeliveryLocationValidator.requireWithinDeliveryRange(distance);
    }

    /**
     * Geocodes the address with AMap and validates its administrative area and match precision
     *
     * @param address Complete address sent to AMap
     * @param requestedDetail Address details that must match the AMap result
     * @param expectedDistrictCode Expected district code; omitted for the merchant address
     * @param addressLabel Address label
     * @return Validated longitude and latitude
     * @throws OrderBusinessException AMap service failure or imprecise address match
     */
    private String geocodeAddress(String address, String requestedDetail, String expectedDistrictCode, String addressLabel) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key", amapKey);
        parameters.put("output", "json");
        parameters.put("address", address);
        String response;
        try {
            response = requestAmap("https://restapi.amap.com/v3/geocode/geo", parameters);
        } catch (RuntimeException exception) {
            log.error("{}AMap geocoding request failed", addressLabel, exception);
            throw new OrderBusinessException(addressLabel + "定位服务暂不可用");
        }
        return DeliveryLocationValidator.resolveGeocodeCoordinate(
                response, requestedDetail, expectedDistrictCode, addressLabel);
    }

    /**
     * Sends a request to the AMap Web service
     *
     * @param url AMap service URL
     * @param parameters HTTP query parameters
     * @return HTTP response body
     */
    String requestAmap(String url, Map<String, String> parameters) {
        return HttpClientUtil.doGet(url, parameters);
    }

    /**
     * Handles a customer request for an order update
     *
     * @param id Order ID to look up within the current user's ownership scope
     */
    public void reminder(Long id){
        // Customers can request updates only for their own orders
        Orders ordersDB = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());

        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("orderNumber", ordersDB.getNumber());
        map.put("content", "订单号: " + ordersDB.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
