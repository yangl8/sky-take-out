<template>
  <div class="container">
    <h2 class="homeTitle">
      {{ $t('dashboard.orderManagement') }}<i>{{ days[1] }}</i
      ><span><router-link to="/order">{{ $t('dashboard.orderDetails') }}</router-link></span>
    </h2>
    <div class="orderviewBox">
      <ul>
        <li>
          <span class="status"
            ><i class="iconfont icon-waiting"></i>{{ $t('orders.awaitingAcceptance') }}</span
          >
          <span class="num tip"
            ><router-link to="/order?status=2">{{
              orderviewData.waitingOrders
            }}</router-link></span
          >
        </li>
        <li>
          <span class="status"
            ><i class="iconfont icon-staySway"></i>{{ $t('orders.awaitingDelivery') }}</span
          >
          <span class="num tip"
            ><router-link to="/order?status=3">{{
              orderviewData.deliveredOrders
            }}</router-link></span
          >
        </li>
        <li>
          <span class="status"
            ><i class="iconfont icon-complete"></i>{{ $t('orders.completed') }}</span
          >
          <span class="num"
            ><router-link to="/order?status=5">{{
              orderviewData.completedOrders
            }}</router-link></span
          >
        </li>
        <li>
          <span class="status"><i class="iconfont icon-cancel"></i>{{ $t('orders.cancelled') }}</span>
          <span class="num"
            ><router-link to="/order?status=6">{{
              orderviewData.cancelledOrders
            }}</router-link></span
          >
        </li>
        <li>
          <span class="status"><i class="iconfont icon-all"></i>{{ $t('orders.allOrders') }}</span>
          <span class="num"
            ><router-link to="/order">{{
              orderviewData.allOrders
            }}</router-link></span
          >
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator'
import { getday } from '@/utils/formValidate'
@Component({
  name: 'Orderview',
})
export default class extends Vue {
  @Prop() private orderviewData!: any
  get days() {
    return getday()
  }
}
</script>

<style lang="scss" scoped>
.orderviewBox {
  ul {
    flex-wrap: wrap;
    row-gap: 20px;

    li {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      align-items: center;
      column-gap: 12px;
      min-width: 220px;

      .status {
        display: flex;
        align-items: center;
        min-width: 0;
        white-space: nowrap;
      }

      .num {
        float: none;
        justify-self: end;
      }
    }
  }
}
</style>
