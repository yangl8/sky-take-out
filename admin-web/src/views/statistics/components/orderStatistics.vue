<template>
  <div class="container">
    <h2 class="homeTitle">{{ $t('statistics.orderStatistics') }}</h2>
    <div class="charBox">
      <div class="orderProportion">
        <div>
          <p>{{ $t('dashboard.orderCompletionRate') }}</p>
          <p>{{ (orderdata.orderCompletionRate * 100).toFixed(1) }}%</p>
        </div>
        <div class="symbol">=</div>
        <div>
          <p>{{ $t('dashboard.validOrders') }}</p>
          <p>{{ orderdata.validOrderCount }}</p>
        </div>
        <div class="symbol">/</div>
        <div>
          <p>{{ $t('statistics.totalOrders') }}</p>
          <p>{{ orderdata.totalOrderCount }}</p>
        </div>
      </div>
      <div id="ordermain" style="width: 100%; height: 300px"></div>
      <ul class="orderListLine">
        <li class="one"><span></span>{{ $t('statistics.totalOrdersCount') }}</li>
        <li class="three"><span></span>{{ $t('statistics.validOrdersCount') }}</li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import * as echarts from 'echarts'
@Component({
  name: 'OrderStatistics',
})
export default class extends Vue {
  @Prop() private orderdata!: any
  @Prop() private overviewData!: any

  @Watch('orderdata')
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
    const chartDom = document.getElementById('ordermain') as any
    const myChart = echarts.getInstanceByDom(chartDom) || echarts.init(chartDom)
    // const baseDate = this.orderdata.list.map((item) => {
    //   return (item as any).date
    // })
    // const baseAmount = this.orderdata.list.map((item) => {
    //   return (item as any).amount
    // })
    // const baseValidNum = this.orderdata.list.map((item) => {
    //   return (item as any).accomplishNum
    // })
    // const baseAccomplishNum = this.orderdata.list.map((item) => {
    //   return (item as any).accomplishNum
    // })
    console.log(this.orderdata)
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
        data: this.orderdata.data.dateList,
      },
      yAxis: [
        {
          type: 'value',
          min: 0,
          //max: 500,
          interval: 50,
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
          name: String(this.$t('statistics.totalOrders')),
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

          data: this.orderdata.data.orderCountList,
        },
        {
          name: String(this.$t('dashboard.validOrders')),
          type: 'line',
          // stack: 'Total',
          smooth: false,
          showSymbol: false,
          symbolSize: 10,
          itemStyle: {
            normal: {
              color: '#FD7F7F',
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

          data: this.orderdata.data.validOrderCountList,
        }
      ],
    }
    option && myChart.setOption(option)
  }
}
</script>
