<!--购买页-->
<template>
  <view class="customer-box">
    <view class="wrap">
      <view class="contion">
        <view class="orderPay">
          <view>
            <view v-if="timeout">{{ $t("payment.timedOut") }}</view>
            <view v-else
              >{{ $t("payment.remaining") }}<text>{{ rocallTime }}</text></view
            >
          </view>
          <view class="money"
            >￥<text>{{ orderDataInfo.orderAmount }}</text></view
          >
          <view>{{ shopInfo().shopName }}<text v-if="orderDataInfo.orderNumber">-{{ orderDataInfo.orderNumber }}</text></view>
        </view>
      </view>
      <view class="box payBox">
        <view class="contion">
          <view class="example-body">
            <radio-group class="uni-list" @change="styleChange">
              <view class="uni-list-item">
                <view
                  class="uni-list-item__container"
                  v-for="(item, index) in payMethodList"
                  :key="item"
                >
                  <view class="uni-list-item__content">
                    <icon class="wechatIcon"></icon
                    ><text class="uni-list-item__content-title">{{
                      item
                    }}</text>
                  </view>
                  <view class="uni-list-item__extra">
                    <radio
                      :value="item"
                      color="#FFC200"
                      :checked="index == activeRadio"
                      class="radioIcon"
                    />
                  </view>
                </view>
              </view>
            </radio-group>
          </view>
        </view>
      </view>
      <view class="bottomBox btnBox">
        <button class="add_btn" type="primary" plain="true" @click="handleSave">
          {{ $t("payment.confirm") }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState, mapMutations } from "vuex";
import { paymentOrder, mockPaymentSuccess, cancelOrder, getOrderDetail } from "@/pages/api/api.js";
import { isLocalDevelopment } from "@/utils/env.js";
export default {
  data() {
    return {
      timeout: false,
      rocallTime: "",
      orderId: null,
      orderDataInfo: {},
      activeRadio: 0,
      payMethodList: [this.$t("payment.wechat")],
      times: null,
    };
  },
  created() {
    this.orderDataInfo = this.orderData();
  },
  mounted() {
    this.runTimeBack();
  },
  onLoad(options) {
    this.orderId = options.orderId;
  },
  onShow() {
    uni.setNavigationBarTitle({ title: this.$t("payment.title") });
  },
  methods: {
    ...mapState(["orderData", "shopInfo"]),
    ...mapMutations(["setSelectedOrder"]),
    // 支付详情
    handleSave() {
      if (this.timeout) {
        cancelOrder(this.orderId).then((res) => {});
        uni.redirectTo({
          url: "/pages/details/index?orderId=" + this.orderId,
        });
        return;
      }
      uni.showModal({
        title: this.$t("payment.confirmTitle"),
        content: this.$t("payment.confirmMessage"),
        confirmText: this.$t("common.confirm"),
        cancelText: this.$t("common.cancel"),
        success: ({ confirm }) => {
          if (confirm) {
            this.submitPayment();
          }
        },
      });
    },
    async submitPayment() {
        clearTimeout(this.times);
        if (isLocalDevelopment) {
          try {
            await mockPaymentSuccess(this.orderDataInfo.orderNumber, this.orderId);
            try {
              const detail = await getOrderDetail(this.orderId);
              if (detail.code === 1 && detail.data) {
                this.setSelectedOrder(detail.data);
              }
            } catch (detailError) {
              // 支付已经成功，详情页会再次请求订单数据
            }
            uni.showToast({ title: this.$t("payment.successful"), icon: "success" });
            setTimeout(() => {
              uni.redirectTo({
                url: "/pages/success/index?orderId=" + this.orderId,
              });
            }, 800);
          } catch (error) {
            uni.showToast({
              title: error.msg || this.$t("payment.failed"),
              icon: "none",
            });
          }
          return;
        }
        const params = {
          orderNumber: this.orderDataInfo.orderNumber,
          payMethod: this.activeRadio === 0 ? 1 : 2,
        };
        paymentOrder(params).then(async (res) => {
          if (res.code === 1) {
            const [err, payRes] = await uni.requestPayment({
              ...res.data,
              package: res.data.packageStr, // package 为微信支付必须的字段
            });
            console.log(err, payRes);
            if (err) {
              await uni.showToast({ title: this.$t("payment.failed"), icon: "error" });
              setTimeout(() => {
                // 下单失败!!
                uni.redirectTo({
                  url: "/pages/details/index?orderId=" + this.orderId,
                });
              }, 1500);
            } else {
              await uni.showToast({ title: this.$t("payment.successful"), icon: "success" });
              setTimeout(() => {
                // 下单成功!!
                uni.redirectTo({
                  url: "/pages/success/index?orderId=" + this.orderId,
                });
              }, 1500);
            }
          } else {
            uni.showToast({
              title: res.msg,
              duration: 1000,
              icon: "none",
            });
          }
        });
    },
    // // 订单倒计时
    runTimeBack() {
      const end = Date.parse(this.orderDataInfo.orderTime.replace(/-/g, "/"));
      const now = Date.parse(new Date());
      const m15 = 15 * 60 * 1000;
      const msec = m15 - (now - end);
      if (msec < 0) {
        this.timeout = true;
        clearTimeout(this.times);
      } else {
        let min = parseInt((msec / 1000 / 60) % 60);
        let sec = parseInt((msec / 1000) % 60);
        if (min < 10) {
          min = "0" + min;
        } else {
          min = min;
        }
        if (sec < 10) {
          sec = "0" + sec;
        } else {
          sec = sec;
        }
        this.rocallTime = min + ":" + sec;
        let that = this;
        if (min >= 0 && sec >= 0) {
          if (min === 0 && sec === 0) {
            this.timeout = true;
            clearTimeout(this.times);
            return;
          }
          this.times = setTimeout(function () {
            that.runTimeBack();
          }, 1000);
        }
      }
    },
  },
};
</script>
<style src="./../common/Navbar/navbar.scss" lang="scss" scoped></style>
<style src="./../order/style.scss" lang="scss"></style>
<style>
</style>
