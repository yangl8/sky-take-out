<template>
  <view>
    <!-- 导航 -->
    <uni-nav-bar @clickLeft="goBack" left-icon="back" leftIcon="arrowleft" :title="$t('details.title')" statusBar="true" fixed="true"
      color="#ffffff" backgroundColor="#333333"></uni-nav-bar>
    <!-- end -->
    <view class="order_content orderDetail" v-if="orderDetailsData.id">
      <view class="order_content_box" scroll-y="true" scroll-top="0rpx">
        <!-- 支付状态 -->
        <status ref="status" :timeout="timeout" :orderDetailsData="orderDetailsData" :rocallTime="rocallTime"
          @statusWord="statusWord" @paymentTime="paymentTime" @handlePay="handlePay" @handleReminder="handleReminder"
          @handleRefund="handleRefund" @handleCancel="handleCancel" @oneMoreOrder="oneMoreOrder"></status>
        <!-- end -->
        <!-- 订单详情 -->
        <order-detail :orderDataes="orderDataes" :orderDetailsData="orderDetailsData"
          :showDisplay="showDisplay" @toggleDisplay="toggleDisplay"></order-detail>
        <!-- end -->
        <!-- 联系商家 -->
        <view class="box contactMerchant">
          <button @click="handlePhone('bottom', orderDetailsData.shopTelephone)">
            <view class="phoneIcon"></view>
            {{ $t("details.contactMerchant") }}
          </button>
          <!-- 4 派送中 5 派送中 -->
          <button class="call-rider" v-if="[4, 5].includes(orderDetailsData.status)"
            @click="handlePhone('bottom', orderDetailsData.courierTelephone)">
            <view class="phoneIcon"></view>
            {{ $t("details.contactCourier") }}
          </button>
        </view>
        <!-- end -->
        <!-- 配送信息 -->
        <delivery-info :orderDetailsData="orderDetailsData"></delivery-info>
        <!-- end -->
        <!-- 订单信息 -->
        <order-info :orderDetailsData="orderDetailsData"></order-info>
        <!-- end -->
      </view>
      <!-- 联系商家弹层 -->
      <uni-popup ref="commonPopup" class="comPopupBox">
        <view class="popup-content">
          <view class="text">{{ textTip }}</view>
          <view class="btn" v-if="showConfirm">
            <view @click="closePopupInfo">{{ $t("common.confirm") }}</view>
          </view>
          <view class="btn" v-else>
            <view @click="closePopupInfo">{{ $t("details.wait") }}</view>
            <view @click="handlePhone('bottom')">{{ $t("details.call") }}</view>
          </view>
        </view>
      </uni-popup>
      <!-- 拨打电话弹层 -->
      <view class="container phoneCon">
        <uni-popup ref="phone" @change="change" class="popupBox">
          <view class="popup-content">
            <view>{{ phone }}</view>
            <view @click="call">{{ $t("common.call") }}</view>
            <view @click="closePopup" class="closePopup">{{ $t("common.cancel") }}</view>
          </view>
        </uni-popup>
      </view>
      <!-- end -->
    </view>
  </view>
</template>
<script src="../../utils/common.js"></script>
<script src="./index.js"></script>
<style src="./../common/Navbar/navbar.scss" lang="scss" scoped></style>
<style src="../order/style.scss" lang="scss"></style>
