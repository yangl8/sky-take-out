<!-- 订单状态 -->
<template>
  <view>
    <view class="box">
      <view class="orderInfoTip">
        <view class="tit">{{ statusWord(orderDetailsData.status, orderDetailsData.payStatus) }} <text class="smw"
            v-if="timeout && orderDetailsData.status === 1">{{ $t("details.timedOut") }}</text></view>
        <view class="rejectionReason" v-if="orderDetailsData.status === 7">
          <text v-if="orderDetailsData.payStatus === 1 || orderDetailsData.payStatus === 2">{{ $t("details.refundSuccessful") }}</text>
          <text v-else-if="orderDetailsData.cancelReason">{{ orderDetailsData.cancelReason }}</text>
          <text v-else-if="orderDetailsData.rejectionReason">{{ orderDetailsData.rejectionReason }}</text>
        </view>
        <view v-if="!timeout && orderDetailsData.status === 1">
          <view class="time">
            <view class="timeIcon"></view>
            {{ $t("details.awaitingPayment") }}
            <text>{{ rocallTime }}</text>
            <text>{{ paymentTime }}</text>
          </view>
        </view>
        <view class="againBtn">
          <button class="new_btn" type="default" @click="handleCancel('center', orderDetailsData)" v-if="(!timeout && orderDetailsData.status === 1) ||
            [2, 3, 4].includes(orderDetailsData.status)
            ">
            {{ $t("details.cancelOrder") }}
          </button>
          <button class="new_btn btn" type="default" @click="handlePay(orderDetailsData.id)"
            v-if="!timeout && orderDetailsData.status === 1">
            {{ $t("details.payNow") }}
          </button>
          <button class="new_btn btn" type="default" @click="handleReminder('center', orderDetailsData.id)"
            v-if="orderDetailsData.status === 2">
            {{ $t("history.remind") }}
          </button>
          <button class="new_btn" type="default" @click="handleRefund('center')" v-if="orderDetailsData.status == 5">
            {{ $t("details.requestRefund") }}
          </button>
          <button class="new_btn" type="default" @click="oneMoreOrder(orderDetailsData.id)"
            v-if="[5, 6, 7].includes(orderDetailsData.status)">
            {{ $t("history.reorder") }}
          </button>
        </view>
      </view>
    </view>
    <view class="box timeTip" v-if="!timeout && orderDetailsData.status === 1">
      <view class="icon newIcon"></view>
      {{ $t("details.paymentNotice") }}
    </view>
    <view class="box timeTip" v-if="orderDetailsData.status === 6 && orderDetailsData.payStatus === 2">
      <view class="icon moneyIcon"></view>
      {{ $t("details.refundNotice") }}
    </view>
  </view>
</template>
<script>
import { statusWord } from "@/utils/index";
export default {
  // 获取父级传的数据
  props: {
    // 订单详情
    orderDetailsData: {
      type: Object,
      default: () => ({}),
    },
    // 倒计时间
    timeout: {
      type: Boolean,
      default: false,
    },
    // 支付时间
    rocallTime: {
      type: String,
      default: "",
    },
  },
  methods: {
    // 地址选择
    statusWord(status, payStatus) {
      this.$emit("statusWord", status);
      // return errr;
      return statusWord(status, null, this.$store.state.locale, payStatus);
    },
    //取消订单
    handleCancel(type, obj) {
      this.$emit("handleCancel", { type: type, obj: obj });
    },
    // 立即支付
    handlePay(id) {
      this.$emit("handlePay", id);
    },
    // 催单
    handleReminder(type, id) {
      this.$emit("handleReminder", { type: type, id: id });
    },
    // 申请退款
    handleRefund(type) {
      this.$emit("handleRefund", type);
    },
    // 再来一单
    oneMoreOrder(id) {
      this.$emit("oneMoreOrder", id);
    },
  },
};
</script>
<style src="../../order/style.scss" lang="scss"></style>
