<template>
  <view>
    <uni-nav-bar
      @clickLeft="goBack"
      left-icon="back"
      leftIcon="arrowleft"
      :title="$t('my.title')"
      statusBar="true"
      fixed="true"
      color="#ffffff"
      backgroundColor="#ffc200"
    ></uni-nav-bar>

    <view class="my-center">
      <!-- 头像展示部分 -->
      <head
        :psersonUrl="psersonUrl"
        :nickName="nickName"
        :gender="gender"
        :phoneNumber="phoneNumber"
        :getPhoneNum="getPhoneNum"
      ></head>

      <view class="container">
        <view class="language-setting">
          <text>{{ $t("common.language") }}</text>
          <picker
            :value="languageIndex"
            :range="languageNames"
            @change="changeLanguage"
          >
            <view class="language-value">{{ languageNames[languageIndex] }}</view>
          </picker>
        </view>
        <!-- 地址和历史订单 -->
        <order-info @goAddress="goAddress" @goOrder="goOrder"></order-info>
        <!-- 最近订单 -->
        <!-- 最近订单title -->
        <view
          class="recent"
          v-if="recentOrdersList && recentOrdersList.length > 0"
        >
          <text class="order_line">{{ $t("my.recentOrders") }}</text>
        </view>
        <order-list
          :scrollH="scrollH"
          @lower="lower"
          @goDetail="goDetail"
          @oneOrderFun="oneOrderFun"
          @handleReminder="handleReminder"
          @handleCancel="handleCancel"
          @getOvertime="getOvertime"
          @statusWord="statusWord"
          @numes="numes"
          :loading="loading"
          :loadingText="loadingText"
          :recentOrdersList="recentOrdersList"
        ></order-list>
      </view>
    </view>
  </view>
</template>

<script>
import { getOrderPage, repetitionOrder, delShoppingCart, reminderOrder, cancelOrder } from "../api/api.js";
import { mapMutations } from "vuex";
import { statusWord, getOvertime } from "@/utils/index.js";
import { supportedLocales } from "@/locales";

import HeadInfo from "./components/headInfo.vue"; //头部
import OrderInfo from "./components/orderInfo.vue"; //地址
import OrderList from "./components/orderList.vue"; //最近订单
export default {
  data() {
    return {
      psersonUrl: "../../static/btn_waiter_sel.png",
      nickName: "",
      gender: "0",
      phoneNumber: "18500557668",
      recentOrdersList: [],
      sumOrder: {
        amount: 0,
        number: 0,
      },
      status: "",
      scrollH: 0,
      pageInfo: {
        page: 1,
        pageSize: 10,
        total: 0,
      },
      loadingText: "",
      loading: false,
    };
  },
  components: {
    HeadInfo,
    OrderInfo,
    OrderList,
  },
  computed: {
    languageNames() {
      return supportedLocales.map((locale) => this.$t(locale.labelKey));
    },
    languageIndex() {
      const index = supportedLocales.findIndex(
        (locale) => locale.code === this.$store.state.locale
      );
      return index === -1 ? 0 : index;
    },
  },
  filters: {
    getPhoneNum(str) {
      return str.replace(/\-/g, "");
    },
  },
  onLoad() {
    this.psersonUrl =
      this.$store.state.baseUserInfo &&
      this.$store.state.baseUserInfo.avatarUrl;
    this.nickName =
      this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.nickName;
    this.gender =
      this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.gender;
    this.phoneNumber =
      this.$store.state.shopPhone && this.$store.state.shopPhone;
  },
  onShow() {
    this.pageInfo.page = 1;
    this.recentOrdersList = [];
    this.loading = false;
    this.getList();
  },
  created() {},
  onReady() {
    uni.getSystemInfo({
      success: (res) => {
        this.scrollH = res.windowHeight - uni.upx2px(100);
      },
    });
  },
  methods: {
    ...mapMutations(["setAddressBackUrl", "setLocale", "setSelectedOrder"]),
    changeLanguage(event) {
      const locale = supportedLocales[Number(event.detail.value)];
      if (locale) {
        this.setLocale(locale.code);
      }
    },
    statusWord(obj) {
      return statusWord(obj.status, obj.time, this.$store.state.locale);
    },
    getOvertime(time) {
      return getOvertime(time);
    },
    // 获取列表数据
    getList() {
      const params = {
        pageSize: 10,
        page: this.pageInfo.page,
      };
      getOrderPage(params).then((res) => {
        if (res.code === 1) {
          this.recentOrdersList = this.recentOrdersList.concat(
            res.data.records
          );
          this.pageInfo.total = res.data.total;
          this.loadingText = "";
          this.loading = false;
        }
      });
    },
    // 去地址页面
    goAddress() {
      this.setAddressBackUrl("/pages/my/my");
      // TODO
      uni.redirectTo({
        url: "/pages/address/address?form=" + "my",
      });
    },
    // 去历史订单页面
    goOrder() {
      // TODO
      uni.navigateTo({
        url: "/pages/historyOrder/historyOrder",
      });
    },
    async oneOrderFun(id) {
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
    // 催单
    handleReminder(id) {
      reminderOrder(id).then((res) => {
        if (res.code === 1) {
          uni.showToast({ title: this.$t("popup.reminderSent"), icon: "none" });
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
        confirmText: this.$t("common.confirm"),
        cancelText: this.$t("common.cancel"),
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
            uni.showToast({ title: error.msg || this.$t("order.operationFailed"), icon: "none" });
          });
        }
      });
    },
    showMerchantPhone() {
      const phone = this.$store.state.shopPhone || (this.$store.state.shopInfo || {}).phone;
      uni.showModal({
        title: this.$t("details.contactMerchant"),
        content: this.$t("popup.contactMerchantCancel", { phone: phone || "-" }),
        confirmText: this.$t("common.call"),
        cancelText: this.$t("common.cancel"),
        success: ({ confirm }) => {
          if (confirm && phone) uni.makePhoneCall({ phoneNumber: String(phone) });
        },
      });
    },
    quitClick() {},
    // 去详情页面
    goDetail(order) {
      this.setAddressBackUrl("/pages/my/my");
      this.setSelectedOrder(order);
      uni.navigateTo({
        url: "/pages/details/index?orderId=" + order.id,
      });
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
    goBack() {
      uni.redirectTo({
        url: "/pages/index/index",
      });
    },
  },
};
</script>
<style lang="scss" scoped>
.my-center {
  background: #f6f6f6;
  height: 100%;

  .container {
    margin-top: 20rpx;
    height: calc(100% - 194rpx);
  }

  .language-setting {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20rpx;
    padding: 28rpx 30rpx;
    background: #ffffff;
    font-size: 28rpx;

    .language-value {
      color: #666666;
    }
  }

  .recent {
    padding: 0 30rpx;
    margin: 28rpx 0 18rpx;
    box-sizing: border-box;

    .order_line {
      display: block;
      font-size: 30rpx;
      line-height: 42rpx;
      font-weight: 500;
    }
  }
}
::v-deep .uni-navbar--border {
  border-width: 0 !important;
}
</style>
