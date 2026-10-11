<!--选择多规格弹层-->
<template>
  <!-- 餐品详情 -->
  <view class="dish_detail_pop" v-if="dishDetailes.type == 1">
    <image
      mode="aspectFill"
      class="div_big_image"
      :src="dishDetailes.image"
    ></image>
    <view class="title">{{ $displayText(dishDetailes.name, 'name') }}</view>
    <view class="desc">{{ $displayText(dishDetailes.description, 'description') }}</view>
    <view class="but_item">
      <view class="price">
        <text class="ico">￥</text>
        {{ dishDetailes.price.toFixed(2) }}
      </view>
      <view
        class="active"
        v-if="dishDetailes.flavors.length === 0 && dishDetailes.dishNumber > 0"
      >
        <image
          src="../../../static/btn_red.png"
          @click="redDishAction(dishDetailes, '普通')"
          class="dish_red"
          mode=""
        ></image>
        <text class="dish_number">{{ dishDetailes.dishNumber }}</text>
        <image
          src="../../../static/btn_add.png"
          class="dish_add"
          @click="addDishAction(dishDetailes, '普通')"
          mode=""
        ></image>
      </view>

      <view class="active" v-if="dishDetailes.flavors.length > 0"
        ><view class="dish_card_add" @click="moreNormDataesHandle(dishDetailes)"
          >{{ $t("home.selectOptionsShort") }}</view
        ></view
      >
      <view
        class="active"
        v-if="
          dishDetailes.dishNumber === 0 && dishDetailes.flavors.length === 0
        "
      >
        <view class="dish_card_add" @click="addDishAction(dishDetailes, '普通')"
          >{{ $t("home.addToCart") }}</view
        >
      </view>
    </view>
    <view class="close" @click="dishClose"
      ><image
        class="close_img"
        src="../../../static/but_close.png"
        mode=""
      ></image
    ></view>
  </view>
  <!-- end -->
  <!-- 套餐详情 -->
  <view class="dish_detail_pop" v-else>
    <scroll-view class="dish_items" scroll-y="true" scroll-top="0rpx">
      <view
        class="dish_item"
        v-for="(item, index) in dishMealData"
        :key="index"
      >
        <image class="div_big_image" :src="item.image" mode=""></image>
        <view class="title">
          {{ $displayText(item.name, 'name') }}
          <text style="">X{{ item.copies }}</text>
        </view>
        <view class="desc">{{ $displayText(item.description, 'description') }}</view>
      </view>
    </scroll-view>
    <view class="but_item">
      <view class="price">
        <text class="ico">￥</text>
        {{ dishDetailes.price }}
      </view>
      <view
        class="active"
        v-if="dishDetailes.dishNumber && dishDetailes.dishNumber > 0"
      >
        <image
          src="../../../static/btn_red.png"
          @click="redDishAction(dishDetailes, '普通')"
          class="dish_red"
          mode=""
        ></image>
        <text class="dish_number">{{ dishDetailes.dishNumber }}</text>
        <image
          src="../../../static/btn_add.png"
          class="dish_add"
          @click="addDishAction(dishDetailes, '普通')"
          mode=""
        ></image>
      </view>
      <view class="active" v-else-if="dishDetailes.dishNumber == 0"
        ><view
          class="dish_card_add"
          @click="addDishAction(dishDetailes, '普通')"
          >{{ $t("home.addToCart") }}</view
        ></view
      >
    </view>
    <view class="close" @click="dishClose"
      ><image
        class="close_img"
        src="../../../static/but_close.png"
        mode=""
      ></image
    ></view>
  </view>
  <!-- end -->
</template>
<script>
export default {
  // 获取父级传的数据
  props: {
    dishDetailes: {
      type: Object,
      default: () => ({}),
    },
    openDetailPop: {
      type: Boolean,
      default: false,
    },
    dishMealData: {
      type: Array,
      default: () => [],
    },
  },
  methods: {
    // 加入购物车
    addDishAction(obj, item) {
      console.log(obj, item);
      this.$emit("addDishAction", obj, item);
    },
    redDishAction(obj, item) {
      this.$emit("redDishAction", obj, item);
    },
    // 选择规格
    moreNormDataesHandle(obj) {
      this.$emit("moreNormDataesHandle", obj);
    },
    // 关闭菜单详情
    dishClose() {
      this.$emit("dishClose");
    },
  },
};
</script>
<style lang="scss" scoped>
.dish_detail_pop {
  width: calc(100vw - 160rpx);
  box-sizing: border-box;
  position: relative;
  top: 50%;
  left: 50%;
  padding: 40rpx;
  transform: translateX(-50%) translateY(-50%);
  background: #fff;
  border-radius: 20rpx;
  .div_big_image {
    width: 100%;
    height: 320rpx;
    border-radius: 10rpx;
  }
  .title {
    font-size: 34rpx;
    line-height: 44rpx;
    text-align: center;
    font-weight: bold;
    margin: 12rpx 0 8rpx;
    word-break: break-word;
  }
  .desc {
    font-size: 26rpx;
    line-height: 36rpx;
    word-break: break-word;
  }
  .dish_items {
    height: 60vh;
  }
  .but_item {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 20rpx;
    .price {
      text-align: left;
      color: #e94e3c;
      line-height: 88rpx;
      box-sizing: border-box;
      font-size: 48rpx;
      font-weight: bold;
      .ico {
        font-size: 28rpx;
      }
    }
    .active {
      display: flex;
      flex-shrink: 0;
      .dish_add,
      .dish_red {
        display: block;
        width: 72rpx;
        height: 72rpx;
      }
      .dish_number {
        padding: 0 10rpx;
        line-height: 72rpx;
        font-size: 30rpx;
        font-family: PingFangSC, PingFangSC-Medium;
        font-weight: 500;
      }
      .dish_card_add {
        width: auto;
        min-width: 200rpx;
        max-width: 260rpx;
        min-height: 60rpx;
        line-height: 36rpx;
        padding: 12rpx 22rpx;
        box-sizing: border-box;
        text-align: center;
        font-weight: 500;
        font-size: 28rpx;
        opacity: 1;
        background: #ffc200;
        border-radius: 40rpx;
      }
    }
  }
}
.close {
  position: absolute;
  bottom: -180rpx;
  left: 50%;
  transform: translateX(-50%);
  .close_img {
    width: 88rpx;
    height: 88rpx;
  }
}
</style>
