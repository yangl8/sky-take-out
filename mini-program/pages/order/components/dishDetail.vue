<!--菜品信息-->
<template>
  <view class="box order_list">
    <view class="word_text">
      <text class="word_style">{{ shopInfo.shopName }}</text>
    </view>
    <view class="order-type">
      <view class="type_item" v-for="(obj, index) in orderDataes" :key="index">
        <view class="dish_img">
          <image mode="aspectFill" :src="obj.image" class="dish_img_url"></image>
        </view>
        <view class="dish_info">
          <view class="dish_name"> {{ $displayText(obj.name, 'name') }} </view>
          <view class="dish_dishFlavor" v-if="obj.dishFlavor">
            {{ $displayText(obj.dishFlavor, 'flavor') }}
          </view>
          <view class="dish_price">×
            <text v-if="obj.number && obj.number > 0" class="dish_number">{{
              obj.number
            }}</text>
          </view>
          <view class="dish_active">
            <text>￥</text> {{ obj.amount.toFixed(2) }}
          </view>
        </view>
      </view>
      <view class="iconUp">
        <view @click="$emit('toggleDisplay')" v-if="orderListDataes.length > 3">
          <text v-if="!showDisplay">{{ $t("order.expand") }}</text>
          <text v-else>{{ $t("order.collapse") }}</text>
          <image class="icon_img" :class="showDisplay ? 'icon_imgDown' : ''" src="../../../static/toRight.png" mode="aspectFit">
          </image>
        </view>
      </view>
      <view class="orderList">
        <view class="orderInfo">
          <text class="text">{{ $t("order.packingFee") }}</text><text class="may">￥</text>{{ orderDishNumber }}
        </view>
        <view class="orderInfo">
          <text class="text">{{ $t("order.deliveryFee") }}</text><text class="may">￥</text>{{ deliveryFee }}
        </view>
        <view class="totalMoney">
          {{ $t("order.total") }}<text class="text"><text>￥</text>{{ orderDishPrice.toFixed(2) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>
<script>
import { mapState } from "vuex";
export default {
  // 获取父级传的数据
  props: {
    // 菜品数据
    orderDataes: {
      type: Array,
      default: () => [],
    },
    // 是否显示更多收起
    showDisplay: {
      type: Boolean,
      default: false,
    },
    // 菜品数据
    orderListDataes: {
      type: Array,
      default: () => [],
    },
    // 打包费
    orderDishNumber: {
      type: Number,
      default: 0,
    },
    // 合计总钱数
    orderDishPrice: {
      type: Number,
      default: 0,
    },
  },
  computed: {
    ...mapState(["deliveryFee", "shopInfo"]),
  },
};
</script>
<style src="./../style.scss" lang="scss" scoped></style>
<style lang="scss" scoped>
.iconUp {
  text-align: center;
  font-size: 24rpx;
  color: #666;
  padding: 8rpx 0;
}

.icon_img {
  width: 30rpx;
  height: 30rpx;
  margin-left: 8rpx;
  transform: rotate(90deg);
  vertical-align: middle;
}

.icon_imgDown {
  transform: rotate(-90deg);
}
</style>
