<template>
  <div class="container">
    <h2 class="homeTitle">
      {{ $t('dashboard.dishOverview') }}<span><router-link to="dish">{{ $t('dashboard.dishManagement') }}</router-link></span>
    </h2>
    <div class="orderviewBox">
      <ul>
        <li>
          <span class="status"><i class="iconfont icon-open"></i>{{ $t('catalog.onSale') }}</span>
          <span class="num">{{ dishesData.sold }}</span>
        </li>
        <li>
          <span class="status"><i class="iconfont icon-stop"></i>{{ $t('catalog.offSale') }}</span>
          <span class="num">{{ dishesData.discontinued }}</span>
        </li>
        <li class="add">
          <router-link to="/dish/add">
            <i></i>
            <p>{{ $t('catalog.newDish') }}</p>
          </router-link>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator'
import orderList from './orderList.vue'
@Component({
  components: { orderList },
  name: 'cuisineview',
})
export default class extends Vue {
  @Prop() private dishesData!: any
}
</script>

<style lang="scss" scoped>
.orderviewBox li {
  position: relative;
  min-height: 182px;
}

.orderviewBox .num {
  position: absolute;
  right: 20px;
  bottom: 12px;
  float: none;
}
</style>
