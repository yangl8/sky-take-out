<template>
  <section class="app-main">
    <transition
      name="fade-transform"
      mode="out-in"
    >
      <router-view />
    </transition>
  </section>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'

@Component({
  'name': 'AppMain'
})
export default class extends Vue {
  @Watch('$route')
  onRouteChange() {
    this.resetScrollPosition()
  }

  mounted() {
    this.resetScrollPosition()
  }

  private resetScrollPosition() {
    this.$nextTick(() => {
      const appMain = this.$el as HTMLElement
      appMain.scrollTop = 0
    })
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  height: calc(100% - 64px);
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
  min-width: 0;
}
</style>
