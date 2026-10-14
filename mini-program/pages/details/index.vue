<template>
  <view>
    <uni-nav-bar @clickLeft="goBack" left-icon="back" leftIcon="arrowleft" :title="$t('details.title')" statusBar="true" fixed="true"
      color="#ffffff" backgroundColor="#333333"></uni-nav-bar>
    <!-- end -->
    <view class="order_content orderDetail" v-if="orderDetailsData.id">
      <view class="order_content_box" scroll-y="true" scroll-top="0rpx">
        <status ref="status" :timeout="timeout" :orderDetailsData="orderDetailsData" :rocallTime="rocallTime"
          @statusWord="statusWord" @paymentTime="paymentTime" @handlePay="handlePay" @handleReminder="handleReminder"
          @handleRefund="handleRefund" @handleCancel="handleCancel" @oneMoreOrder="oneMoreOrder"></status>
        <!-- end -->
        <order-detail :orderDataes="orderDataes" :orderDetailsData="orderDetailsData"
          :showDisplay="showDisplay" @toggleDisplay="toggleDisplay"></order-detail>
        <!-- end -->
        <view class="box contactMerchant">
          <button @click="handlePhone('bottom', orderDetailsData.shopTelephone)">
            <view class="phoneIcon"></view>
            {{ $t("details.contactMerchant") }}
          </button>
          <button class="call-rider" v-if="[4, 5].includes(orderDetailsData.status)"
            @click="handlePhone('bottom', orderDetailsData.courierTelephone)">
            <view class="phoneIcon"></view>
            {{ $t("details.contactCourier") }}
          </button>
        </view>
        <!-- end -->
        <delivery-info :orderDetailsData="orderDetailsData"></delivery-info>
        <!-- end -->
        <order-info :orderDetailsData="orderDetailsData"></order-info>
        <!-- end -->
      </view>
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
