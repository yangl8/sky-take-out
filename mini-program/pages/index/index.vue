<template>
  <view>
    <navBar></navBar>
    <!-- end -->
    <view class="home_content" :style="{ paddingTop: ht + 'px' }">
      <view class="restaurant_info_box">
        <view class="restaurant_info">
          <view class="info_top">
            <view class="info_top_left">
              <image class="logo_ruiji" src="../../static/logo_ruiji.png"></image>
            </view>
            <view class="info_top_right">
              <view class="right_title">
                <text>{{ $t("home.brand") }}</text>
                <view class="businessStatus" v-if="shopStatus === 1">{{ $t("home.open") }}</view>
                <view class="businessStatus close" v-else-if="shopStatus === 0">{{ $t("home.closed") }}</view>
              </view>
              <view class="right_details">
                <view class="details_flex">
                  <image class="top_icon" src="../../static/money.png"></image>
                  <text class="icon_text">{{ $t("home.deliveryFee", { amount: deliveryFee() }) }}</text>
                </view>
              </view>
            </view>
          </view>
          <view class="info_bottom">
            <view>
              <view class="word">{{ $t("home.description") }}</view>
              <view class="address">
                <icon></icon>
                <text class="address_text">{{ shopInfo().shopAddress ? $displayText(shopInfo().shopAddress, 'address') : $t("home.loadingAddress") }}</text>
              </view>
            </view>
            <view>
              <view class="phone" @click="handlePhone('bottom')">
                <icon class="phoneIcon"></icon>
              </view>
            </view>
          </view>
          <view class="shop_closed_notice" v-if="shopClosedNoticeVisible">
            <text>{{ $t("home.shopClosedNotice") }}</text>
            <text class="notice_close" @click.stop="dismissShopClosedNotice">×</text>
          </view>
        </view>
      </view>
      <!-- end -->
      <view class="restaurant_menu_list">
        <view class="type_list">
          <scroll-view scroll-y scroll-with-animation class="u-tab-view menu-scroll-view" :scroll-top="scrollTop + 100"
            :scroll-into-view="itemId">
            <view class="type_item" id="target" :class="[typeIndex == index ? 'active' : '']"
              v-for="(item, index) in typeListData" :key="index" @tap.stop="swichMenu(item, index)">
              <view class="item" :class="item.name.length > 5 ? 'allLine' : ''">{{ $displayText(item.name, 'category') }}</view>
            </view>
          </scroll-view>
        </view>
        <scroll-view class="vegetable_order_list" scroll-y="true" scroll-top="0rpx"
          v-if="dishListItems && dishListItems.length > 0">
          <view class="type_item" v-for="(item, index) in dishListItems" :key="index">
            <view class="dish_img" @click="openDetailHandle(item)">
              <image mode="aspectFill" :src="item.image" class="dish_img_url"></image>
            </view>
            <view class="dish_info">
              <view class="dish_name" @click="openDetailHandle(item)">{{
                $displayText(item.name, 'name')
              }}</view>
              <view class="dish_label" @click="openDetailHandle(item)">{{
                $displayText(item.description || item.name, item.description ? 'description' : 'name')
              }}</view>
              <view class="dish_label dish_sales" @click="openDetailHandle(item)">{{ $t("home.monthlySales", { count: item.monthlySales || 0 }) }}</view>
              <view class="dish_price">
                <text class="ico">￥</text>
                {{ item.price.toFixed(2) }}
              </view>
              <view class="dish_action_area">
                <view class="dish_active" v-if="!item.flavors || item.flavors.length === 0">
                  <image v-if="item.dishNumber >= 1" src="../../static/btn_red.png" @click="redDishAction(item, '普通')"
                    class="dish_red"></image>
                  <text v-if="item.dishNumber > 0" class="dish_number">{{
                    item.dishNumber
                  }}</text>
                  <image src="../../static/btn_add.png" class="dish_add" @click="addDishAction(item, '普通')"></image>
                </view>
                <view class="dish_active_btn" v-else>
                  <text class="dish_cart_quantity">
                    {{ $t("home.cartQuantity", { count: item.dishNumber }) }}
                  </text>
                  <view class="check_but" @click="moreNormDataesHandle(item)">{{ $t("home.selectOptionsShort") }}</view>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
        <view class="no_dish" v-else>
          <view v-if="typeListData.length > 0">{{ $t("home.emptyCategory") }}</view>
        </view>
      </view>
      <view class="mask-box"></view>
      <view class="footer_order_buttom" v-if="orderListData().length === 0">
        <view class="order_number">
          <image src="../../static/btn_waiter_nor.png" class="order_number_icon" mode=""></image>
        </view>
        <view class="order_price">
          <text class="ico">￥</text>
          0
        </view>
        <view class="order_but">{{ $t("home.checkout") }}</view>
      </view>
      <!-- end -->
      <view class="footer_order_buttom order_form" v-else>
        <view class="orderCar" @click="() => (openOrderCartList = !openOrderCartList)">
          <view class="order_number">
            <image src="../../static/btn_waiter_sel.png" class="order_number_icon" mode=""></image>
            <view class="order_dish_num">{{ orderDishNumber }}</view>
          </view>
          <view class="order_price">
            <text class="ico">￥</text>
            {{ orderDishPrice.toFixed(2) }}
          </view>
        </view>
        <view class="order_but" @click="goOrder()">{{ $t("home.checkout") }}</view>
      </view>
      <!-- end -->
      <view class="pop_mask" v-if="openMoreNormPop && moreNormDishdata">
        <popMask :moreNormDishdata="moreNormDishdata" :moreNormdata="moreNormdata" :flavorDataes="flavorDataes"
          @checkMoreNormPop="checkMoreNormPop" @addShop="addShop" @closeMoreNorm="closeMoreNorm"></popMask>
      </view>
      <view class="pop_mask" v-show="openDetailPop" style="z-index: 9999">
        <dishDetail :dishDetailes="dishDetailes" :openDetailPop="openDetailPop" :dishMealData="dishMealData"
          @redDishAction="redDishAction" @addDishAction="addDishAction" @moreNormDataesHandle="moreNormDataesHandle"
          @dishClose="dishClose"></dishDetail>
      </view>
      <view class="pop_mask" v-show="openOrderCartList" @click="openOrderCartList = !openOrderCartList">
        <popCart :openOrderCartLis="openOrderCartList" :orderAndUserInfo="orderAndUserInfo"
          @clearCardOrder="clearCardOrder" @addDishAction="addDishAction" @redDishAction="redDishAction"></popCart>
      </view>
      <view class="pop_mask" v-show="loaddingSt">
        <view class="lodding">
          <image class="lodding_ico" src="../../static/lodding.gif" mode=""></image>
        </view>
      </view>
      <phone ref="phone" :phoneData="phoneData" @closePopup="closePopup"></phone>
      <!-- end -->
    </view>
  </view>
</template>
<script src="./index.js"></script>
<style src="./style.scss" lang="scss" scoped></style>
<style scoped>
/* #ifdef MP-WEIXIN || APP-PLUS */
::v-deep ::-webkit-scrollbar {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
  -webkit-appearance: none;
  background: transparent;
  color: transparent;
}

/* #endif */
</style>
