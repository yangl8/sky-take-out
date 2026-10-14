<template>
  <div class="container">
    <h2 class="homeTitle">
      {{ $t('dashboard.todayData') }}<i>{{ days[1] }}</i
      ><span><router-link to="statistics">{{ $t('dashboard.detailedData') }}</router-link></span>
    </h2>
    <div class="overviewBox">
      <ul>
        <li>
          <p class="tit">{{ $t('dashboard.turnover') }}</p>
          <p class="num">¥ {{ overviewData.turnover }}</p>
        </li>
        <li>
          <p class="tit">{{ $t('dashboard.validOrders') }}</p>
          <p class="num">{{ overviewData.validOrderCount }}</p>
        </li>
        <li>
          <p class="tit">{{ $t('dashboard.orderCompletionRate') }}</p>
          <p class="num">
            {{ (overviewData.orderCompletionRate * 100).toFixed(0) }}%
          </p>
        </li>
        <li>
          <p class="tit">{{ $t('dashboard.averageOrderValue') }}</p>
          <p class="num">¥ {{ overviewData.unitPrice }}</p>
        </li>

        <li>
          <p class="tit">{{ $t('dashboard.newUsers') }}</p>
          <p class="num">{{ overviewData.newUsers }}</p>
        </li>
      </ul>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator'
import { getday } from '@/utils/formValidate'
@Component({
  name: 'Overview',
})
export default class extends Vue {
  @Prop() private overviewData!: any
  get days() {
    return getday()
  }
}
</script>
