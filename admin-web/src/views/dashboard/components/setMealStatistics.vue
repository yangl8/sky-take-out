<template>
  <div class="container">
    <h2 class="homeTitle">
      {{ $t('dashboard.setMealOverview') }}<span><router-link to="setmeal">{{ $t('dashboard.setMealManagement') }}</router-link></span>
    </h2>
    <div class="orderviewBox">
      <ul>
        <li>
          <span class="status"><i class="iconfont icon-open"></i>{{ $t('catalog.onSale') }}</span>
          <span class="num">{{ setMealData.sold }}</span>
        </li>
        <li>
          <span class="status"><i class="iconfont icon-stop"></i>{{ $t('catalog.offSale') }}</span>
          <span class="num">{{ setMealData.discontinued }}</span>
        </li>
        <li class="add">
          <router-link to="setmeal/add">
            <i></i>
            <p>{{ $t('catalog.newSetmeal') }}</p>
          </router-link>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator'
@Component({
  name: 'SetMeal',
})
export default class extends Vue {
  @Prop() private setMealData!: any
}
</script>
