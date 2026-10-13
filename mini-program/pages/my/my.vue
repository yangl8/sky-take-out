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
          <view class="language-value" @click="openLanguagePicker">{{ languageNames[languageIndex] }}</view>
        </view>
        <order-info @goAddress="goAddress" @goOrder="goOrder"></order-info>
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
    <view v-if="showLanguagePicker" class="language-picker-mask" @click.self="closeLanguagePicker">
      <view class="language-picker-panel">
        <view class="language-picker-header">
          <text @click="closeLanguagePicker">{{ $t("common.cancel") }}</text>
          <text @click="confirmLanguage">{{ $t("common.confirm") }}</text>
        </view>
        <picker-view class="language-picker-view" :value="[selectedLanguageIndex]" @change="onLanguagePickerChange">
          <picker-view-column>
            <view v-for="(name, index) in languageNames" :key="index" class="language-picker-item">{{ name }}</view>
          </picker-view-column>
        </picker-view>
      </view>
    </view>
    <phone ref="phone" :phoneData="phoneData" @closePopup="closePhonePopup"></phone>
    </view>
  </view>
</template>

<script>
import { getOrderPage, repetitionOrder, delShoppingCart, reminderOrder, cancelOrder } from "../api/api.js";
import { mapMutations } from "vuex";
import { statusWord, getOvertime, userFacingErrorMessage } from "@/utils/index.js";
import { supportedLocales } from "@/locales";

import HeadInfo from "./components/headInfo.vue";
import OrderInfo from "./components/orderInfo.vue";
import OrderList from "./components/orderList.vue";
import Phone from "@/components/uni-phone/index.vue";
export default {
  data() {
    return {
      psersonUrl: "../../static/btn_waiter_sel.png",
      nickName: "",
      gender: "0",
      phoneNumber: "18500557668",
      phoneData: "",
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
      showLanguagePicker: false,
      selectedLanguageIndex: 0,
    };
  },
  components: {
    HeadInfo,
    OrderInfo,
    OrderList,
    Phone,
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
    this.selectedLanguageIndex = this.languageIndex;
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
    openLanguagePicker() {
      this.selectedLanguageIndex = this.languageIndex;
      this.showLanguagePicker = true;
    },
    closeLanguagePicker() {
      this.showLanguagePicker = false;
    },
    onLanguagePickerChange(event) {
      this.selectedLanguageIndex = Number(event.detail.value[0]);
    },
    confirmLanguage() {
      const locale = supportedLocales[this.selectedLanguageIndex];
      if (locale) {
        this.setLocale(locale.code);
      }
      this.closeLanguagePicker();
    },
    statusWord(obj) {
      return statusWord(obj.status, obj.time, this.$store.state.locale);
    },
    getOvertime(time) {
      return getOvertime(time);
    },
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
    goAddress() {
      this.setAddressBackUrl("/pages/my/my");
      // TODO
      uni.redirectTo({
        url: "/pages/address/address?form=" + "my",
      });
    },
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
      await delShoppingCart();
      repetitionOrder(id).then((res) => {
        if (res.code === 1) {
          uni.navigateBack({
            delta: routeIndex > -1 ? pages.length - routeIndex : 1,
          });
        }
      });
    },
    handleReminder(id) {
      reminderOrder(id).then((res) => {
        if (res.code === 1) {
          uni.showToast({ title: this.$t("popup.reminderSent"), icon: "none" });
        }
      });
    },
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
    quitClick() {},
    goDetail(order) {
      this.setAddressBackUrl("/pages/my/my");
      this.setSelectedOrder(order);
      uni.navigateTo({
        url: "/pages/details/index?orderId=" + order.id,
      });
    },
    dataAdd() {
      const pages = Math.ceil(this.pageInfo.total / 10);
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

  .language-picker-mask {
    position: fixed;
    z-index: 999;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: flex-end;
    background: rgba(0, 0, 0, 0.4);
  }

  .language-picker-panel {
    width: 100%;
    background: #fff;
    border-radius: 16rpx 16rpx 0 0;
  }

  .language-picker-header {
    display: flex;
    justify-content: space-between;
    padding: 28rpx 40rpx;
    border-bottom: 1rpx solid #eee;
    color: #ff9900;
    font-size: 32rpx;
  }

  .language-picker-view {
    height: 420rpx;
  }

  .language-picker-item {
    height: 84rpx;
    line-height: 84rpx;
    text-align: center;
    font-size: 28rpx;
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
