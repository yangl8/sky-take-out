<!--订单信息-->
<template>
  <view class="box">
    <view class="orderBaseInfo">
      <view>
        <view class="info-label">{{ $t("details.orderNumber") }}</view>
        <view class="info-value">{{ orderDetailsData.number }}</view>
      </view>
      <view>
        <view class="info-label">{{ $t("details.orderTime") }}</view>
        <view class="info-value">{{ orderDetailsData.orderTime }}</view>
      </view>
      <view>
        <view class="info-label">{{ $t("details.paymentMethod") }}</view>
        <view class="info-value">{{ orderDetailsData.payMethod === 1 ? $t("details.wechat") : $t("details.alipay") }}</view>
      </view>
      <view>
        <view class="info-label">{{ $t("details.orderRemark") }}</view>
        <view class="info-value orderinfo-remak">{{
          orderDetailsData.remark ? orderDetailsData.remark : $t("details.noInformation")
        }}</view>
      </view>
      <view>
        <view class="info-label">{{ $t("details.tablewareQuantity") }}</view>
        <view class="info-value">{{
          orderDetailsData.tablewareStatus === 1
            ? orderDetailsData.tablewareNumber
            : orderDetailsData.tablewareNumber
        }}</view>
      </view>
    </view>
  </view>
</template>
<script>
export default {
  // 获取父级传的数据
  props: {
    // 订单详情
    orderDetailsData: {
      type: Object,
      default: () => ({}),
    },
  },
};
</script>
<style src="../../order/style.scss" lang="scss"></style>
<style lang="scss" scoped>
.info-label {
  width: 320rpx !important;
  padding-right: 20rpx !important;
  box-sizing: border-box;
}
.info-value {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
}
</style>
