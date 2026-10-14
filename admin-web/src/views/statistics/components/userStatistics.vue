<template>
  <div class="container">
    <h2 class="homeTitle">{{ $t('statistics.userStatistics') }}</h2>
    <div class="charBox">
      <div id="usermain" style="width: 100%; height: 320px"></div>
      <ul class="orderListLine user">
        <li class="one"><span></span>{{ $t('statistics.totalUsersCount') }}</li>
        <li class="three"><span></span>{{ $t('statistics.newUsersCount') }}</li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import * as echarts from 'echarts'
@Component({
  name: 'UserStatistics',
})
export default class extends Vue {
  @Prop() private userdata!: any
  @Watch('userdata')
  getData() {
    this.$nextTick(() => {
      this.initChart()
    })
  }
  @Watch('$i18n.locale')
  relocalizeChart() {
    this.$nextTick(() => this.initChart())
  }
  initChart() {
    type EChartsOption = echarts.EChartsOption
    const chartDom = document.getElementById('usermain') as any
    const myChart = echarts.getInstanceByDom(chartDom) || echarts.init(chartDom)
    var option: any
    option = {
      // legend: {
      //   show: true,
      //   top: 'bottom',
      // },
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#fff',
        borderRadius: 2,
        textStyle: {
          color: '#333',
          fontSize: 12,
          fontWeight: 300,
        },
      },
      grid: {
        top: '5%',
        left: '20',
        right: '50',
        bottom: '12%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        axisLabel: {
          textStyle: {
            color: '#666',
            fontSize: '12px',
          },
        },
        axisLine: {
          lineStyle: {
            color: '#E5E4E4',
            width: 1,
          },
        },
        data: this.userdata.dateList,
      },
      yAxis: [
        {
          type: 'value',
          min: 0,
          //max: 500,
          //interval: 100,
          axisLabel: {
            textStyle: {
              color: '#666',
              fontSize: '12px',
            },
          },
        },
      ],
      series: [
        {
          name: String(this.$t('statistics.totalUsers')),
          type: 'line',
          // stack: 'Total',
          smooth: false,
          showSymbol: false,
          symbolSize: 10,
          itemStyle: {
            normal: {
              color: '#FFD000',
              lineStyle: {
                color: '#FFD000',
              },
            },
            emphasis: {
              color: '#fff',
              borderWidth: 5,
              borderColor: '#FFC100',
            },
          },

          data: this.userdata.totalUserList,
        },
        {
          name: String(this.$t('dashboard.newUsers')),
          type: 'line',
          // stack: 'Total',
          smooth: false,
          showSymbol: false,
          symbolSize: 10,
          itemStyle: {
            normal: {
              color: '#FD7F7F',
              fontWeigth: 300,
              lineStyle: {
                color: '#FD7F7F',
              },
            },
            emphasis: {
              color: '#fff',
              borderWidth: 5,
              borderColor: '#FD7F7F',
            },
          },

          data: this.userdata.newUserList,
        },
      ],
    }
    option && myChart.setOption(option)
  }
}
</script>
<style scoped>
</style>
