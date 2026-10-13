<template>
  <view class="box">
    <view class="orderBaseInfo">
      <view class="info-row">
        <view class="info-label">{{ $t("details.expectedTime") }}</view>
        <view class="info-value single-line">{{ expectedDeliveryTime }}</view>
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
import { formatDeliveryDateTime } from "@/utils/scheduledDeliveryTime.js";
export default {
  props: {
    orderDetailsData: {
      type: Object,
      default: () => ({}),
    },
  },
  computed: {
    expectedDeliveryTime() {
      const order = this.orderDetailsData || {};
      if (Number(order.deliveryStatus) === 1 && Number(order.payStatus) !== 1) {
        return this.$t("order.immediateDelivery");
      }
      return formatDeliveryDateTime(order.estimatedDeliveryTime) ||
        (Number(order.deliveryStatus) === 1 ? this.$t("order.immediateDelivery") : order.estimatedDeliveryTime || "");
    },
    cryptoName() {
      if (!this.orderDetailsData.consignee) return "";
      if (this.orderDetailsData.sex === undefined || this.orderDetailsData.sex === null || this.orderDetailsData.sex === "") {
        return this.orderDetailsData.consignee;
      }
      if (Number(this.orderDetailsData.sex) === 0) {
        return this.$t("common.sir", { name: this.orderDetailsData.consignee });
      } else {
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
