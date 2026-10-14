<template>
  <view class="empty-box">
    <image class="empty-img" src="./../../static/no_adress.png" mode="" />
    <view class="empty-des">{{ textLabel || $t("address.empty") }}</view>
  </view>
</template>
<script>
export default {
  props:{
    'textLabel':{
      type: String,
      default: ''
    }
  }
}
</script>
<style lang="scss" scoped>
.empty-box{
  width: 100%;
  text-align: center;
  transform: translate(0,50%);
  .empty-img{
    width: 480rpx;
    height: 296rpx;
  }
  .empty-des{
    height: 44rpx;
    line-height: 44rpx;
    margin-top: 40rpx;
    font-size: 32rpx;
    font-family: PingFangSC, PingFangSC-Medium;
    font-weight: 500;
    color: #666;
  }
}
</style>
