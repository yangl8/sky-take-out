package com.sky.constant;

public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String DELIVERY_TYPE_INVALID = "配送方式无效，无法下单";
    public static final String DELIVERY_TIME_REQUIRED = "请选择有效的预约送达时间";
    public static final String DELIVERY_PREPARATION_TIME_NOT_MET = "预约送达时间不足最短准备时间";
    public static final String DELIVERY_TIME_OUTSIDE_BUSINESS_HOURS = "预约送达时间不在允许营业时段内";
    public static final String SHOP_CLOSED_FOR_IMMEDIATE_DELIVERY = "店铺当前未营业，无法提交立即配送订单";
    public static final String SHOP_STATUS_UNAVAILABLE = "店铺营业状态暂不可用，请稍后重试";
    public static final String ORDER_PAYMENT_EXPIRED = "订单超过15分钟付款期限，无法继续支付";
    public static final String ORDER_PAYMENT_NOT_ALLOWED = "订单当前状态不允许支付";
    public static final String SCHEDULED_ORDER_PREPARATION_TIME_NOT_MET = "预约送达时间不足30分钟准备时间，无法支付";
    public static final String SHOP_CLOSED_FOR_IMMEDIATE_PAYMENT = "店铺当前未营业，无法发起立即配送支付";

}
