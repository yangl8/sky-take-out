<template>
  <view class="success_content">
    <view class="success_info">
      <image
        class="success_icon"
        src="../../static/success.png"
        mode=""
      ></image>
      <view class="success_title">{{ $t("payment.successful") }}</view>
      <view class="word-box">
        <text class="word_bottom"
          ><text class="word_date">{{ $t("success.arrival", { time: arrivalTime }) }}</text></text
        >
      </view>
      <view class="success_desc">{{ $t("success.preparing") }}</view>
      <!-- 新添加 -->
      <view class="btns">
        <view class="go_dish defaultBtn" @click="goIndex()">{{ $t("success.home") }}</view>
        <view class="go_dish" @click="goOrder">{{ $t("success.order") }}</view>
      </view>
    </view>
  </view>
</template>
<script>
import { mapState } from "vuex";
import { getOrderDetail } from "@/pages/api/api.js";
import { formatDeliveryDateTime } from "@/utils/scheduledDeliveryTime.js";
export default {
  data() {
    return {
      arrivalTime: "",
      orderId: null,
    };
  },
  computed: {
    tableInfo: function () {
      return this.shopInfo();
    },
  },
  async onLoad(options) {
    this.orderId = options.orderId;
    for (let attempt = 0; attempt < 10; attempt += 1) {
      try {
        const result = await getOrderDetail(this.orderId);
        if (result && result.code === 1 && result.data && Number(result.data.payStatus) === 1) {
          this.arrivalTime = formatDeliveryDateTime(result.data.estimatedDeliveryTime);
          return;
        }
      } catch (error) {
        // 支付已成功；短暂重试，等待服务端支付记录完成。
      }
      if (attempt < 9) await new Promise(resolve => setTimeout(resolve, 500));
    }
  },
  onShow() {
    uni.setNavigationBarTitle({ title: this.$t("payment.successful") });
  },
  methods: {
    ...mapState(["shopInfo"]),
    // 回首页
    goIndex() {
      uni.reLaunch({
        url: "/pages/index/index",
      });
    },
    // 查看订单
    goOrder() {
      uni.redirectTo({
        url: "/pages/details/index?orderId=" + this.orderId + "&fromSuccess=1&sex=" + this.$store.state.gender,
      });
    },
  },
};
</script>
<style src="./../common/Navbar/navbar.scss" lang="scss" scoped></style>
<style lang="scss" scoped>
.success_content {
  padding-top: 260rpx;
  .success_info {
    text-align: center;
    .success_icon {
      width: 144rpx;
      height: 138rpx;
      text-align: center;
    }
    .success_title {
      font-size: 40rpx;
      font-family: PingFangSC, PingFangSC-Medium;
      font-weight: 500;
      color: #333333;
      margin-top: 16rpx;
      height: 56rpx;
      line-height: 56rpx;
    }
    .success_desc {
      font-size: 32rpx;
      margin-bottom: 40rpx;
      font-family: PingFangSC, PingFangSC-Regular;
      font-weight: 400;
      text-align: center;
      color: #666666;
      height: 44rpx;
      line-height: 44rp;
      margin-top: 24rpx;
    }
    .word-box {
      margin-top: 38rpx;
      height: 44rpx;
      line-height: 44rpx;
    }
    .word_bottom {
      font-family: PingFangSC, PingFangSC-Regular;
      font-weight: 400;
      text-align: center;
      font-size: 32rpx;
      color: #666666;
      .word_date {
        color: #ffc200;
      }
    }

    .btns {
      width: 100%;
      box-sizing: border-box;
      padding: 0 24rpx;
      display: flex;
      justify-content: center;
      gap: 24rpx;
      .go_dish {
        flex: 1 1 0;
        position: relative;
        font-size: 30rpx;
        // margin: 0 auto;
        min-width: 0;
        width: auto;
        padding: 0 16rpx;
        box-sizing: border-box;
        white-space: nowrap;
        font-size: 28rpx;
        height: 72rpx;
        line-height: 72rpx;
        margin-top: 20rpx;
        background: #ffc200;
        border-radius: 8rpx;
      }
      .defaultBtn {
        border: 1px solid #e5e4e4;
        background: none;
      }
    }
  }
}
</style>
