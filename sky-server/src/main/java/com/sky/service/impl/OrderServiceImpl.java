package com.sky.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
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
import com.sky.constant.StatusConstant;
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

    @Value("${sky.shop.delivery-fee:0}")
    private BigDecimal deliveryFee;

    private static final BigDecimal PACKING_FEE_PER_ITEM = BigDecimal.ONE;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.amap.key}")
    private String amapKey;

    /**
     * 申请订单退款
     *
     * @param currentOrder 已付款的订单
     * @throws Exception 微信退款请求失败
     */
    private void refundPaidOrder(Orders currentOrder) throws Exception {
        if (environment.acceptsProfiles(Profiles.of("dev & !prod"))) {
            log.info("开发环境模拟退款成功，订单号：{}", currentOrder.getNumber());
            return;
        }
        String refund = weChatPayUtil.refund(
                currentOrder.getNumber(),
                currentOrder.getNumber(),
                new BigDecimal(0.01),
                new BigDecimal(0.01));
        log.info("申请退款：{}", refund);
    }

    /**
     * 退款已付款订单
     *
     * @param currentOrder 已付款的订单
     * @throws OrderBusinessException 退款失败并需要回滚本地取消状态
     */
    private void refundPaidOrderOrFail(Orders currentOrder) {
        try {
            refundPaidOrder(currentOrder);
        } catch (Exception exception) {
            log.error("订单退款失败，订单号：{}", currentOrder.getNumber(), exception);
            throw new OrderBusinessException("退款失败，请检查微信支付配置后重试");
        }
    }

    /**
     * 提交用户订单
     *
     * @param ordersSubmitDTO 订单提交信息
     * @return 创建成功的订单信息
     */
    @Transactional 
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO){

        //1.处理各种业务异常（地址簿为空，购物车数据为空）
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookMapper.getByIdAndUserId(
                ordersSubmitDTO.getAddressBookId(), userId);
        if(addressBook == null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //检查用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        //查询当前用户的购物车数据
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.size() == 0){
            //抛出业务异常
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
        
        //2.向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders, "amount", "packAmount");
        orders.setAmount(serverOrderAmount);
        orders.setPackAmount(serverPackAmount);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(buildDeliveryAddress(addressBook));
        orders.setUserId(userId);

        orderMapper.insert(orders);

        //3.向订单明细表插入n条数据
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单id
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //4.清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //5.封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                        .id(orders.getId())
                        .orderTime(orders.getOrderTime())
                        .orderNumber(orders.getNumber())
                        .orderAmount(orders.getAmount())
                        .build();

        return orderSubmitVO;
    }

    /**
     * 根据当前商品信息构建订单明细
     *
     * @param shoppingCartList 当前用户的购物车商品
     * @return 使用当前名称、图片和单价构建的订单明细
     */
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
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getByNumberAndUserId(ordersPaymentDTO.getOrderNumber(), userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 处理微信支付成功回调
     *
     * @param outTradeNo 微信支付回调中的商户订单号
     * @throws OrderBusinessException 订单不存在或状态不允许支付完成
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders currentOrder = orderMapper.getByNumber(outTradeNo);
        if (currentOrder == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        processPaySuccess(currentOrder, true);
    }

    /**
     * 处理当前用户的模拟支付请求
     *
     * @param orderNumber 请求中的订单编号
     * @param orderId 请求中的订单ID
     * @throws OrderBusinessException 订单归属或状态不符合模拟支付条件
     */
    public void mockPaySuccessForUser(String orderNumber, Long orderId) {
        Orders currentOrder = orderMapper.getByIdAndUserId(orderId, BaseContext.getCurrentId());
        if (currentOrder == null || !Objects.equals(currentOrder.getNumber(), orderNumber)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        processPaySuccess(currentOrder, false);
    }

    /**
     * 更新支付成功订单的状态
     *
     * @param currentOrder 当前读取到的订单状态
     * @param paymentCallback 是否来自微信支付回调，决定竞争失败时的回调处理方式
     * @return 本次调用完成状态转换时返回 true，重复或已变化时返回 false
     * @throws OrderBusinessException 模拟支付遇到不允许的订单状态
     */
    private boolean processPaySuccess(Orders currentOrder, boolean paymentCallback) {
        if (Orders.CANCELLED.equals(currentOrder.getStatus())) {
            log.error("已取消订单收到支付成功通知，订单号：{}，需核对并补偿资金", currentOrder.getNumber());
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

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders targetOrder = Orders.builder()
                .id(currentOrder.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        int updatedRows = updateOrderState(currentOrder, targetOrder);
        if (updatedRows != 1) {
            Orders latestOrder = orderMapper.getById(currentOrder.getId());
            if (isPaidOrderInProgress(latestOrder)) {
                return false;
            }
            if (latestOrder != null && Orders.CANCELLED.equals(latestOrder.getStatus())) {
                log.error("支付成功与订单取消竞争，订单号：{}，订单保持取消状态，需核对并补偿资金",
                        currentOrder.getNumber());
                if (paymentCallback) {
                    return false;
                }
            }
            if (paymentCallback) {
                log.warn("支付回调未能更新订单状态，订单号：{}，当前状态已变化", currentOrder.getNumber());
                return false;
            }
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 只有数据库确认本次状态转换成功后才广播新订单
        Map map = new HashMap();
        map.put("type",1); //1表示来单提醒 2表示客户催单
        map.put("orderId", currentOrder.getId());
        map.put("orderNumber", currentOrder.getNumber());
        map.put("content", "订单号: " + currentOrder.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        return true;
    }

    /**
     * 判断订单是否已进入支付后的状态
     *
     * @param currentOrder 当前订单
     * @return 订单已支付且处于待接单至已完成状态时返回 true
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
     * 更新订单状态
     *
     * @param expectedCurrentOrder 预期的原订单状态及支付状态
     * @param targetOrder 需要写入的订单状态和相关字段
     * @return 实际更新的记录数
     */
    private int updateOrderState(Orders expectedCurrentOrder, Orders targetOrder) {
        return orderMapper.updateStatusIfCurrent(expectedCurrentOrder.getId(), expectedCurrentOrder.getStatus(),
                expectedCurrentOrder.getPayStatus(), null, targetOrder);
    }

    /**
     * 执行订单状态转换
     *
     * @param expectedCurrentOrder 预期的原订单状态及支付状态
     * @param targetOrder 需要写入的目标订单状态
     */
    private void requireStateTransition(Orders expectedCurrentOrder, Orders targetOrder) {
        if (updateOrderState(expectedCurrentOrder, targetOrder) != 1) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum 当前页码
     * @param pageSize 每页订单数
     * @param status 订单状态筛选条件
     * @param payStatus 支付状态筛选条件
     * @return 包含分页总数和当前页订单的查询结果
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status, Integer payStatus) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setPayStatus(payStatus);

        // 分页条件查询
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
     * 查询指定菜品的本月销量
     * @param dishIds 菜品id集合
     * @return 菜品id与本月销量的映射
     */
    public Map<Long, Integer> getDishMonthlySales(List<Long> dishIds) {
        return getMonthlySales(dishIds, true);
    }

    /**
     * 查询指定套餐的本月销量
     * @param setmealIds 套餐id集合
     * @return 套餐id与本月销量的映射
     */
    public Map<Long, Integer> getSetmealMonthlySales(List<Long> setmealIds) {
        return getMonthlySales(setmealIds, false);
    }

    /**
     * 查询指定菜品或套餐的本月销量
     * @param itemIds 菜品或套餐id集合
     * @param dish 是否查询菜品销量
     * @return 商品id与本月销量的映射
     */
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

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        return toOrderVO(orders, null);
    }

   /**   (non-Javadoc)
    * 用户端详情查询同时校验订单归属
    * @see com.sky.service.OrderService#detailsForUser(java.lang.Long)
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

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
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
     * 用户取消订单
     *
     * @param id 当前用户要取消的订单ID
     * @throws Exception 订单退款操作失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders currentOrder = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());

        // 校验订单是否存在
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

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 确认订单属于当前用户后再读取订单明细
        Orders orders = orderMapper.getByIdAndUserId(id, userId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO 包含待接单订单ID的请求对象
     */
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

    /**
     * 拒单
     *
     * @param ordersRejectionDTO 包含订单ID和拒单原因的请求对象
     * @throws Exception 订单退款操作失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders currentOrder = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (currentOrder == null
                || !Orders.TO_BE_CONFIRMED.equals(currentOrder.getStatus())
                || !Orders.PAID.equals(currentOrder.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
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
     * 取消订单
     *
     * @param ordersCancelDTO 包含订单ID和取消原因的请求对象
     * @throws Exception 订单退款操作失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
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
     * 开始派送订单
     *
     * @param id 要开始派送的订单ID
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
     * 完成订单
     *
     * @param id 要标记为完成的订单ID
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

    /**
     * 检查收货地址是否在配送范围内
     *
     * @param deliveryAddress 收货地址
     */
    void checkOutOfRange(String deliveryAddress) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("key",amapKey);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://restapi.amap.com/v3/geocode/geo", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("1")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONArray geocodes = jsonObject.getJSONArray("geocodes");
        //店铺经纬度坐标
        String shopLngLat = geocodes.getJSONObject(0).getString("location");

        map.put("address",deliveryAddress);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://restapi.amap.com/v3/geocode/geo", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("1")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        geocodes = jsonObject.getJSONArray("geocodes");
        //用户收货地址经纬度坐标
        String userLngLat = geocodes.getJSONObject(0).getString("location");

        map.remove("address");
        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);

        //路线规划
        String json = HttpClientUtil.doGet("https://restapi.amap.com/v5/direction/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("1")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject route = jsonObject.getJSONObject("route");
        JSONArray paths = route.getJSONArray("paths");
        Integer distance = paths.getJSONObject(0).getInteger("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**    (non-Javadoc)
     * 客户催单
     * @see com.sky.service.OrderService#reminder(java.lang.Long)
     */
    public void reminder(Long id){
        // 客户只能催办自己的订单
        Orders ordersDB = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());

        // 不区分订单不存在和不属于当前用户，避免泄露其他用户的订单信息
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap();
        map.put("type", 2); //1表示来单提醒， 2表示客户催单
        map.put("orderId", id);
        map.put("orderNumber", ordersDB.getNumber());
        map.put("content", "订单号: " + ordersDB.getNumber());

        //通过websocket向客户端浏览器推送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
