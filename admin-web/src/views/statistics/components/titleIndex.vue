<template>
  <div class="title-index">
    <div class="month">
      <ul class="tabs">
        <li
          class="li-tab"
          v-for="(item, index) in tabsParam"
          @click="toggleTabs(index)"
          :class="{ active: index === nowIndex }"
          :key="index"
        >
          {{ $t(item) }}
          <span></span>
        </li>
      </ul>
    </div>
    <div class="get-time">
      <p>
        {{ $t('statistics.selectedPeriod') }}{{ tateData[0] }} {{ $t('statistics.to') }}
        {{ tateData[tateData.length - 1] }}
      </p>
    </div>
    <el-button
      icon="iconfont icon-download"
      class="right-el-button"
      @click="handleExport"
      >{{ $t('statistics.exportData') }}</el-button
    >
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import { exportInfor } from '@/api/index'
@Component({
  name: 'TitleIndex',
})
export default class extends Vue {
  @Prop() private flag!: any
  @Prop() private tateData!: any
  @Prop() private turnoverData!: any

  nowIndex = 2 - 1
  value = []
  tabsParam = [
    'statistics.yesterday',
    'statistics.last7Days',
    'statistics.last30Days',
    'statistics.thisWeek',
    'statistics.thisMonth',
  ]
  @Watch('flag')
  getNowIndex(val) {
    this.nowIndex = val
  }
  toggleTabs(index: number) {
    this.nowIndex = index
    this.value = []
    this.$emit('sendTitleInd', index + 1)
  }
  handleExport() {
    this.$confirm(String(this.$t('statistics.exportConfirm')), String(this.$t('statistics.notice')), {
      confirmButtonText: String(this.$t('common.confirm')),
      cancelButtonText: String(this.$t('common.cancel')),
      type: 'warning',
    })
      .then(async () => {
        const { data } = await exportInfor()
        let url = window.URL.createObjectURL(data)
        var a = document.createElement('a')
        document.body.appendChild(a)
        a.href = url
        a.download = String(this.$t('statistics.fileName'))
        a.click()
        window.URL.revokeObjectURL(url)
      })
      .then((response) => {})
  }
}
</script>

<style lang="scss" scoped>
.title-index {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;

  .month {
    display: block;
    flex: 0 0 auto;

    .tabs {
      width: auto;

      .li-tab {
        flex: 0 0 auto;
        min-width: 88px;
        padding: 0 16px;
        white-space: nowrap;
      }
    }
  }

  .get-time {
    padding-left: 0;
    white-space: nowrap;
  }

  .right-el-button {
    float: none;
    margin-left: auto;
  }
}

@media (max-width: 1100px) {
  .title-index {
    .get-time {
      order: 3;
      width: 100%;
    }
  }
}
</style>
