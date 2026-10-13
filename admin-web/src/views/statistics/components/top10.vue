<template>
  <div class="container top10">
    <h2 class="homeTitle">{{ $t('statistics.salesTop10') }}</h2>
    <div class="charBox">
      <div id="top" style="width: 100%; height: 380px"></div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator'
import * as echarts from 'echarts'
import { menuName } from '@/i18n/menu'
@Component({
  name: 'Top',
})
export default class extends Vue {
  menuName = menuName
  @Prop() private top10data!: any
  @Watch('top10data')
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
    const chartDom = document.getElementById('top') as any
    const myChart = echarts.getInstanceByDom(chartDom) || echarts.init(chartDom)
    var option: any
    option = {
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
        top: '-10px',
        left: '0',
        right: '0',
        bottom: '0',
        containLabel: true,
      },
      xAxis: {
        show: false,
      },
      yAxis: {
        axisLine: {
          show: false,
        },
        axisTick: {
          show: false,
          alignWithLabel: true,
        },
        type: 'category',
        // interval: 100,
        axisLabel: {
          textStyle: {
            color: '#666',
            fontSize: '12px',
          },
        },
        data: this.top10data.nameList.map((name) => this.menuName(name)),
      },
      series: [
        {
          data: this.top10data.numberList,
          type: 'bar',
          showBackground: true,
          backgroundStyle: {
            color: '#F3F4F7',
          },
          barWidth: 20,
          barGap: '80%' ,
          barCategoryGap: '80%' ,

          itemStyle: {
            emphasis: {
              barBorderRadius: 30,
            },
            normal: {
              barBorderRadius: [0, 10, 10, 0],
              color: new echarts.graphic.LinearGradient(
                1,
                0,
                0,
                0,
                [
                  { offset: 0, color: '#FFBD00' },
                  { offset: 1, color: '#FFD000' },
                ]
              ),
              label: {
                show: true,
                formatter: '{@score}',
                color: '#333',
                position: ['8', '5'],
              },
            },
          },
          // label: {
          //   show: true,
          //   position: "left",
          //   valueAnimation: true,
          // },
        },
      ],
    }
    option && myChart.setOption(option)
  }
}
</script>
