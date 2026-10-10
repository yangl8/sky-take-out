<!--历史订单-->
<template>
  <view class="history_order">
    <uni-nav-bar @clickLeft="goBack" left-icon="back" leftIcon="arrowleft" :title="$t('history.title')" statusBar="true" fixed="true"
      color="#ffffff" backgroundColor="#333333"></uni-nav-bar>
    <!-- 根据scrollinto和:id="'tab'+index"切换下方轮播 -->
    <scroll-view scroll-x class="scroll-row" :scroll-into-view="scrollinto" :scroll-with-animation="true" enable-flex>
      <view v-for="(item, index) in tabBars" :key="index" :id="'tab' + index" class="scroll-row-item"
        @click="changeTab(index)">
        <view :class="tabIndex == index ? 'scroll-row-item-act' : ''"><text class="line"></text>{{ item }}</view>
      </view>
    </scroll-view>
    <!--  滑块内容 对应的是顶部选项卡的切换 :current="tabIndex"  设置的是y方向上可以滚动-->
    <swiper :current="tabIndex" @change="onChangeSwiperTab" :style="{ height: scrollH + 'px' }">
      <swiper-item v-for="(item, index) in tabBars" :key="index">
        <!-- 垂直滚动区域  scroll和swiper的高度都要给且是一样的高度-->
        <scroll-view scroll-y="true" :style="{ height: scrollH + 'px' }" @scrolltolower="lower">
          <!-- 可垂直滚动区域 显示真正内容-->
          <view class="main recent_orders" v-if="recentOrdersList && recentOrdersList.length > 0">
            <!-- 历史订单列表 -->
            <view class="box order_lists" v-for="(item, index) in recentOrdersList" :key="index" :class="{
              'item-last': Number(index) + 1 === recentOrdersList.length,
            }">
              <!-- 时间和支付状态 -->
              <view class="date_type">
                <!-- 时间 -->
                <text class="time">{{ item.orderTime }}</text>
                <!-- 支付状态 -->
                <text class="type status" :class="{ status: item.status == 2 }">{{
                  statusWord(item.status, item.payStatus)
                }}</text>
              </view>
              <view class="payment_countdown" v-if="Number(item.status) === 1 && paymentRemaining(item.orderTime) > 0">
                {{ $t("payment.remaining") }} {{ formatPaymentRemaining(item.orderTime) }}
              </view>
              <!-- 点菜的内容 -->
              <view class="orderBox" v-if="item.orderDetailList && numes(item.orderDetailList).count > 0" @click="goDetail(item)">
                <view class="food_num">
                  <scroll-view scroll-x="true" class="pic" style="width: 100%; overflow: hidden; white-space: nowrap">
                    <view class="food_num_item" v-for="(num, y) in item.orderDetailList" :key="y">
                      <view class="img">
                        <image :src="num.image"></image>
                      </view>
                      <view class="food">{{ $displayText(num.name, 'name') }}</view>
                    </view>
                  </scroll-view>
                </view>
                <!-- 商品数量及金额 -->
                <view class="numAndAum">
                  <view><text>￥{{ item.amount.toFixed(2) }}</text></view>
                  <view><text>{{ $t("common.items", { count: numes(item.orderDetailList).count }) }}</text></view>
                </view>
              </view>
              <view class="againBtn" v-if="item.orderDetailList && numes(item.orderDetailList).count > 0">
                <button class="new_btn" type="default" @click="oneMoreOrder(item.id)"
                  v-if="[5, 6, 7].includes(item.status)">
                  {{ $t("history.reorder") }}
                </button>
                <button class="new_btn btn" type="default" @click="continuePayment(item)"
                  v-if="item.status === 1 && getOvertime(item.orderTime) > 0">
                  {{ $t("history.pay") }}
                </button>
                <button class="new_btn btn" type="default" @click="handleReminder('center', item.id)"
                  v-if="item.status === 2">
                  {{ $t("history.remind") }}
                </button>
                <button class="new_btn" type="default" @click="handleCancel(item)"
                  v-if="[1, 2, 3, 4].includes(item.status)">
                  {{ $t("details.cancelOrder") }}
                </button>
              </view>
            </view>
          </view>
          <empty
            v-else-if="isEmpty"
            :textLabel="$t('history.empty')"
          ></empty>
        </scroll-view>
      </swiper-item>
    </swiper>
    <uni-popup ref="commonPopup" class="comPopupBox">
      <view class="popup-content">
        <view class="text">{{ textTip }}</view>
        <view class="btn" v-if="showConfirm">
          <view @click="closePopup">{{ $t("common.confirm") }}</view>
        </view>
      </view>
    </uni-popup>
    <phone ref="phone" :phoneData="phoneData" @closePopup="closePhonePopup"></phone>
  </view>
</template>

<script>
import {
  getOrderPage,
  repetitionOrder,
  reminderOrder,
  cancelOrder,
  delShoppingCart
} from "../api/api.js";
import { mapMutations } from "vuex";
import Empty from "@/components/empty/empty";
import Phone from "@/components/uni-phone/index.vue";
import { statusWord, getOvertime, userFacingErrorMessage } from "@/utils/index.js";
export default {
  components: {
    Empty,
    Phone,
  },
  data() {
    return {
      recentOrdersList: [],
      pageInfo: {
        page: 1,
        pageSize: 10,
        total: 0,
      },
      status: "",
      payStatus: "",
      loadingType: 0,
      showTitle: false,
      scrollinto: "tab0",
      scrollH: 0,
      tabIndex: 0,
      clockNow: Date.now(),
      countdownTimer: null,
      // tabBars: ["全部订单", "待付款", "退款", "已完成", "派送中"],
      // 状态对应的接口和参数
      urlMap: {
        0: {
          fn: getOrderPage,
          key: "status",
        },
        1: {
          fn: getOrderPage,
          key: "status",
        },
        2: {
          fn: getOrderPage,
          key: "payStatus",
        },
      },
      textTip: "",
      showConfirm: false,
      isEmpty: false,
      phoneData: "",
    };
  },
  computed: {
    tabBars() {
      return [
        this.$t("history.all"),
        this.$t("history.pendingPayment"),
        this.$t("history.refund"),
      ];
    },
  },
  onLoad(options) {
    if (options && options.tab === "1") {
      this.tabIndex = 1;
      this.scrollinto = "tab1";
      this.status = 1;
      this.payStatus = 0;
    }
    this.getList();
    this.startPaymentCountdown();
  },
  onShow() {
    this.startPaymentCountdown();
  },
  onHide() {
    clearInterval(this.countdownTimer);
  },
  onUnload() {
    this.showTitle = false;
    clearInterval(this.countdownTimer);
  },
  onReady() {
    uni.getSystemInfo({
      success: (res) => {
        this.scrollH = res.windowHeight - uni.upx2px(100);
      },
    });
  },
  onPullDownRefresh() {
    this.pageInfo.page = 1;
    this.loadingType = 0;
    this.recentOrdersList = [];
    this.finished = false;
    this.getList();
    uni.stopPullDownRefresh();
    this.showTitle = true;
  },
  onReachBottom() {
    if (this.recentOrdersList.length < Number(this.pageInfo.total)) {
      this.pageInfo.page++;
      this.loadingStatus = "loading";
      this.getList(this.status);
      this.showTitle = true;
    }
  },
  methods: {
    ...mapMutations(["setAddressBackUrl", "setSelectedOrder"]),
    numes(list) {
      let count = 0;
      let total = 0;
      list.length > 0 &&
        list.forEach((obj) => {
          count += Number(obj.number);
          total += Number(obj.number) * Number(obj.amount);
        });
      return { count: count, total: total };
    },
    statusWord(status, payStatus) {
      return statusWord(status, null, this.$store.state.locale, payStatus);
    },
    getOvertime(time) {
      return getOvertime(time);
    },
    startPaymentCountdown() {
      clearInterval(this.countdownTimer);
      this.clockNow = Date.now();
      this.countdownTimer = setInterval(() => {
        this.clockNow = Date.now();
      }, 1000);
    },
    paymentRemaining(time) {
      const createdAt = Date.parse(String(time).replace(/-/g, "/"));
      return Number.isFinite(createdAt) ? Math.max(0, 15 * 60 * 1000 - (this.clockNow - createdAt)) : 0;
    },
    formatPaymentRemaining(time) {
      const seconds = Math.ceil(this.paymentRemaining(time) / 1000);
      const minutes = String(Math.floor(seconds / 60)).padStart(2, "0");
      const remainingSeconds = String(seconds % 60).padStart(2, "0");
      return `${minutes}:${remainingSeconds}`;
    },
    // 获取历史订单列表
    getList() {
      const key = this.urlMap[this.tabIndex].key;
      const fn = this.urlMap[this.tabIndex].fn;
      const params = {
        pageSize: 10,
        page: this.pageInfo.page,
      };
      params[key] = this[key]
      uni.showLoading({ title: this.$t("common.loading"), mask: true });
      fn(params)
        .then((res) => {
          if (res.code === 1) {
          this.recentOrdersList = this.recentOrdersList.concat(
            res.data.records
          );
          this.pageInfo.total = res.data.total;
          this.isEmpty = true;
          }
        })
        .catch(() => {})
        .then(() => {
          uni.hideLoading();
        });
    },
    // 再来一单
    async oneMoreOrder(id) {
      let pages = getCurrentPages();
      let routeIndex = pages.findIndex(
        (item) => item.route === "pages/index/index"
      );
      // 先清空购物车
      await delShoppingCart();
      repetitionOrder(id).then((res) => {
        if (res.code === 1) {
          uni.navigateBack({
            delta: routeIndex > -1 ? pages.length - routeIndex : 1,
          });
        }
      });
    },
    // tab选项卡切换轮播
    changeTab(index) {
      // 点击的还是当前数据的时候直接return
      if (this.tabIndex == index) {
        return;
      }
      this.tabIndex = index;
      if (index === 1) {
        // 待付款
        this.status = 1;
        this.payStatus = 0
      } else if (index === 2) {
        // 退款
        this.status = 6;
        this.payStatus = 2
      }
      else {
        // 全部
        this.status = "";
        this.payStatus = ""
      }
      this.pageInfo.page = 1;
      this.recentOrdersList = [];
      this.isEmpty = false;
      this.getList();
      // 滑动
      this.scrollinto = "tab" + index;
    },
    onChangeSwiperTab(e) {
      this.changeTab(e.detail.current);
    },
    dataAdd() {
      const pages = Math.ceil(this.pageInfo.total / 10); //计算总页数
      if (this.pageInfo.page === pages) {
        this.loadingText = this.$t("common.noMore");
        this.loading = true;
      } else {
        this.pageInfo.page++;
        this.getList();
      }
    },

    lower() {
      this.loadingText = this.$t("common.loadingMore");
      this.loading = true;
      this.dataAdd();
    },
    // 去详情页面
    goDetail(order) {
      this.setAddressBackUrl("/pages/historyOrder/historyOrder");
      this.setSelectedOrder(order);
      uni.navigateTo({ url: "/pages/details/index?orderId=" + order.id });
    },
    // 继续支付时进入支付页，不经过订单详情
    continuePayment(order) {
      this.setSelectedOrder(order);
      uni.navigateTo({ url: "/pages/pay/index?orderId=" + order.id });
    },
    // 催单
    handleReminder(type, id) {
      reminderOrder(id).then((res) => {
        if (res.code === 1) {
          this.showConfirm = true;
          this.textTip = this.$t("popup.reminderSent");
          this.$refs.commonPopup.open(type);
          this.getList(this.status);
        }
      });
    },
    // 取消订单
    handleCancel(order) {
      if ([3, 4].includes(order.status)) {
        this.showMerchantPhone();
        return;
      }
      uni.showModal({
        title: this.$t("popup.cancelOrderTitle"),
        content: this.$t("popup.cancelOrderConfirm"),
        confirmText: this.$t("common.modalConfirm"),
        cancelText: this.$t("common.modalCancel"),
        success: ({ confirm }) => {
          if (!confirm) return;
          cancelOrder(order.id).then((res) => {
            if (res.code === 1) {
              uni.showToast({ title: this.$t("popup.orderCancelled"), icon: "none" });
              this.pageInfo.page = 1;
              this.recentOrdersList = [];
              this.getList();
            }
          }).catch((error) => {
            uni.showToast({ title: userFacingErrorMessage(error && error.msg, this.$store.state.locale), icon: "none" });
          });
        }
      });
    },
    showMerchantPhone() {
      const phone = this.$store.state.shopPhone || (this.$store.state.shopInfo || {}).phone;
      uni.showModal({
        title: this.$t("details.contactMerchant"),
        content: this.$t("popup.contactMerchantCancel", { phone: phone || "-" }),
        confirmText: this.$t("common.modalCall"),
        cancelText: this.$t("common.modalCancel"),
        success: ({ confirm }) => {
          if (!confirm || !phone) return;
          this.phoneData = String(phone);
          this.$nextTick(() => this.$refs.phone.$refs.popup.open("bottom"));
        },
      });
    },
    closePhonePopup(type) {
      this.$refs.phone.$refs.popup.close(type);
    },
    // 关闭弹层
    closePopup(type) {
      this.$refs.commonPopup.close(type);
    },
    // 返回我的
    goBack() {
      uni.redirectTo({
        url: "/pages/my/my",
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.history_order {
  height: 100%;

  .recent_orders {
    padding-top: 8rpx;
  }
}

.payment_countdown {
  margin: 4rpx 0 12rpx;
  color: #f28c28;
  font-size: 24rpx;
  text-align: right;
}

.scroll-row {
  height: 88rpx;
  line-height: 88rpx;
  background-color: #fff;
  padding: 0 30rpx;
  box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.06);
  width: 100vw;
  box-sizing: border-box;
  flex-wrap: nowrap;
  overflow: auto;
  display: flex;
}

.scroll-row-item {
  margin-right: 88rpx;
  color: #666;
  display: inline-block;
  font-size: 28rpx;
  flex-shrink: 0;
}

.scroll-row-item-act {
  color: #333;
  position: relative;
  font-weight: 600;

  .line {
    width: 32rpx;
    height: 8rpx;
    display: block;
    background: #ffc200;
    border-radius: 8rpx;
    transform: translate(-50%, -50%);
    position: absolute;
    bottom: -4rpx;
    left: 50%;
  }
}
</style>
