<!--配送信息-->
<template>
  <view class="box">
    <view class="orderBaseInfo">
      <view class="info-row">
        <view class="info-label">{{ $t("details.expectedTime") }}</view>
        <view class="info-value single-line">{{
          orderDetailsData.deliveryStatus === 1
          ? $t("order.immediateDelivery")
          : orderDetailsData.estimatedDeliveryTime
        }}</view>
      </view>
      <view class="info-row">
        <view class="info-label">{{ $t("details.deliveryAddress") }}</view>
        <view class="info-value">
          <view class="nameInfo">
            <text>{{ cryptoName }}</text>
            {{ orderDetailsData.phone }}
          </view>
          <view>{{ $displayText(orderDetailsData.address, 'address') }}</view>
        </view>
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
  computed: {
    // 处理姓名 如x先生
    cryptoName() {
      if (!this.orderDetailsData.consignee) return "";
      if (this.orderDetailsData.sex === undefined || this.orderDetailsData.sex === null || this.orderDetailsData.sex === "") {
        return this.orderDetailsData.consignee;
      }
      if (Number(this.orderDetailsData.sex) === 0) {
        // 男
        return this.$t("common.sir", { name: this.orderDetailsData.consignee });
      } else {
        // 女
        return this.$t("common.madam", { name: this.orderDetailsData.consignee });
      }
    },
  },
};
</script>
<style src="../../order/style.scss" lang="scss"></style>
<style lang="scss" scoped>
.info-row { align-items: flex-start; }
.info-label {
  width: 320rpx !important;
  padding-right: 20rpx !important;
  box-sizing: border-box;
}
.info-value { flex: 1; min-width: 0; }
.single-line, .nameInfo { white-space: nowrap; }
</style>
