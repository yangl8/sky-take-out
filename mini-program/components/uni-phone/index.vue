<!--拨打电话-->
<template>
  <view class="container phoneCon">
    <uni-popup ref="popup" @change="change" class="popupBox">
      <view class="popup-content">
        <view>{{ phoneData }}</view>
        <view @click="call">{{ $t("common.call") }}</view>
        <view @click="closePopup" class="closePopup">{{ $t("common.cancel") }}</view>
      </view>
    </uni-popup>
  </view>
</template>
<style src="../../pages/index/style.scss" lang="scss" scoped></style>
<script>
import { call } from "../../utils/index.js";
export default {
  props: {
	//电话号码
    phoneData: {
      type: String,
      default: "",
    },
  },
  methods: {
    // 电话号码拨打
    call() {
      this.$refs.popup.close();
      call(this.phoneData, {
        fail: (error) => {
          const message = String((error && error.errMsg) || "").toLowerCase();
          if (message.includes("cancel")) return;
          uni.showToast({ title: this.$t("popup.callFailed"), icon: "none" });
        },
        simulatedDialog: {
          content: this.$t("popup.simulatedCall", { phone: this.phoneData }),
          confirmText: this.$t("common.modalCall"),
          cancelText: this.$t("common.modalCancel"),
        },
      });
    },
	// 取消关闭弹层
    closePopup() {
      this.$emit("closePopup");
    },
  },
};
</script>
