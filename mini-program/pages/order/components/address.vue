<template>
  <view class="container new_address">
    <view class="top" @click="goAddress">
      <view v-if="!address" class="address_name_disabled">
        {{ $t("order.selectAddress") }}
      </view>
      <!-- end -->
      <view v-else class="address_name">
        <view class="address" :class="{ 'has-tag': !!tagLabel }">
          <text v-if="tagLabel" class="tag" :class="'tag' + tagLabel">{{ addressLabel }}</text>
          <view class="word">{{ $displayText(address, 'address') }}</view>
        </view>
        <view class="name">
          <text class="name_1">{{ cryptoName }}</text>
          <text class="name_2">{{ phoneNumber }}</text>
        </view>
        <view v-if="address" class="infoTip"
          >{{ $t("order.contactlessRecommended") }}</view
        >
      </view>
      <view class="address_image">
        <view class="to_right"></view>
      </view>
      <!-- end -->
    </view>
    <view class="bottom">
      <div class="bottomTime" @click="openTimePopuo('bottom')">
        <text class="time_name_disabled">{{ $t("order.deliverNow") }}</text>
        <view class="address_image">
          <text class="">{{ $t("order.deliverySelection", { day: arrivalDay, time: arrivalTime }) }}</text>
          <view class="to_right"></view>
        </view>
      </div>

      <view v-if="address" class="infoTip"
        >{{ $t("order.deliveryNotice") }}</view
      >
    </view>
    <!-- end -->
    <uni-popup ref="timePopup" @change="change" class="popupBox">
      <view class="popup-content">
        <view class="pickerCon">
          <view class="dayBox">
            <scroll-view
              scroll-x="true"
              :scroll-into-view="scrollinto"
              :scroll-with-animation="true"
            >
              <view
                v-for="(item, index) in popleft"
                :key="index"
                :id="'tab' + index"
                class="scroll-row-item"
                @click="dateChange(index)"
              >
                <view v-for="(val, i) in weeks" :key="i">
                  <view
                    :class="tabIndex == index ? 'scroll-row-day' : ''"
                    v-if="index === i"
                    ><text class="line"></text>{{ item
                    }}<text class="week">({{ val }})</text></view
                  >
                </view>
              </view>
            </scroll-view>
          </view>
          <view class="timeBox">
            <scroll-view
              class="card_order_list"
              scroll-y="true"
              scroll-top="40rpx"
            >
              <view
                v-for="(val, i) in newDateData"
                :key="i"
                class="item"
                :class="selectValue === i ? 'city-column_select' : ''"
                @click="timeClick(val, i)"
                >{{ val }}</view
              >
            </scroll-view>
          </view>
        </view>
        <view class="btns" @click="onsuer">{{ $t("common.cancel") }}</view>
      </view>
    </uni-popup>
    <!-- end -->
  </view>
</template>
<script>
export default {
  props: {
    tagLabel: {
      type: String,
      default: "",
    },
    addressLabel: {
      type: String,
      default: "",
    },
    address: {
      type: String,
      default: "",
    },
    nickName: {
      type: String,
      default: "",
    },
    gender: {
      type: Number,
      default: -1,
    },
    phoneNumber: {
      type: String,
      default: "",
    },
    arrivalTime: {
      type: String,
      default: "",
    },
    arrivalDay: {
      type: String,
      default: "",
    },
    tabIndex: {
      type: Number,
      default: 0,
    },
    selectValue: {
      type: Number,
      default: 0,
    },
    popleft: {
      type: Array,
      default: () => [],
    },
    weeks: {
      type: Array,
      default: () => [],
    },
    newDateData: {
      type: Array,
      default: () => [],
    },
  },
  methods: {
    goAddress() {
      this.$emit("goAddress");
    },
    openTimePopuo(type) {
      this.$refs.timePopup.open(type);
    },
    //
    change() {
      this.$emit("change");
    },
    dateChange(index) {
      this.$emit("dateChange", index);
    },
    timeClick(val, i) {
      this.$emit("timeClick", { val: val, i: i });
      this.onsuer();
    },
    onsuer(type) {
      this.$refs.timePopup.close(type);
    },
  },
  computed: {
    cryptoName() {
      if (Number(this.$store.state.gender) === 0) {
        return this.$t("common.sir", { name: this.nickName });
      } else {
        return this.$t("common.madam", { name: this.nickName });
      }
    },
  },
};
</script>
<style src="./../style.scss" lang="scss" scoped></style>
