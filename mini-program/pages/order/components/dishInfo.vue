<template>
  <view class="box order_list">
    <view class="uniInfo">
      <view @click="goRemark">
        <uni-list>
          <uni-list-item showArrow :title="$t('order.remark')" class="uniListItem">
            <template v-slot:footer>
              <text class="temarkText">{{
                remark ? remark : $t("order.contactlessRecommended")
              }}</text>
            </template>
          </uni-list-item>
        </uni-list>
      </view>
      <!-- end -->
      <view @click="openPopuos('bottom')">
        <uni-list>
          <uni-list-item showArrow :title="$t('order.tablewareQuantity')" class="uniListItem">
            <template v-slot:footer>
              <text>{{ $t("order.selectedInStore", { value: tablewareData }) }}</text>
            </template>
          </uni-list-item>
        </uni-list>
      </view>
      <!-- end -->
      <view class="invoiceBox">
        <uni-list>
          <uni-list-item :title="$t('order.invoice')" class="uniListItem">
            <template v-slot:footer>
              <text>{{ $t("order.contactMerchantInvoice") }}</text>
            </template>
          </uni-list-item>
        </uni-list>
      </view>
      <!-- end -->
      <view class="container">
        <uni-popup ref="popup" @change="change" class="popupBox">
          <view class="popup-content">
            <view class="popupTitle">
              <text>{{ $t("order.regulationTitle") }} </text>
              <text>{{ $t("order.regulationText") }}</text>
            </view>
            <view class="popupCon">
              <view class="popupBtn">
                <text @click="closePopup">{{ $t("common.cancel") }}</text>
                <text>{{ $t("order.selectTableware") }}</text>
                <text @click="handlePiker">{{ $t("common.confirm") }}</text>
              </view>
              <pikers
                :baseData="baseData"
                ref="piker"
                @changeCont="changeCont"
              ></pikers>
            </view>
            <view class="popupSet">
              <view>{{ $t("order.futureTableware") }}</view>
              <view>
                <radio-group @change="handleRadio">
                  <label v-for="item in radioGroup" :key="item">
                    <radio
                      :value="item"
                      color="#FFC200"
                      :checked="item == activeRadio"
                    />{{ item }}
                  </label>
                </radio-group>
              </view>
            </view>
          </view>
        </uni-popup>
        <!-- end -->
      </view>
    </view>
  </view>
</template>
<script>
import { editHoppingCart } from '../../api/api'
import Pikers from '@/components/uni-piker/index.vue'
export default {
  props: {
    remark: {
      type: String,
      default: '',
    },
    tablewareData: {
      type: String,
      default: '',
    },
    radioGroup: {
      type: Array,
      default: () => [],
    },
    activeRadio: {
      type: String,
      default: '',
    },
    baseData: {
      type: Array,
      default: () => [],
    },
  },
  components: { Pikers },
  methods: {
    goRemark () {
      this.$emit("goRemark")
    },
    openPopuos (type) {
      this.$refs.popup.open(type)
    },
    change () {
      this.$emit("change")
    },
    closePopup (type) {
      this.$refs.popup.close(type)
    },
    handlePiker () {
      this.$emit('handlePiker')
      this.closePopup()
    },
    changeCont (val) {
      this.$emit("changeCont", val)
    },
    handleRadio (e) {
      this.$emit("handleRadio", e)
    },
  },
};
</script>
<style src="./../style.scss" lang="scss" scoped></style>
