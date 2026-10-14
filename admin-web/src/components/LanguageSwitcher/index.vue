<template>
  <label class="language-switcher">
    <span>{{ $t('common.language') }}</span>
    <select :value="$i18n.locale" :aria-label="String($t('common.language'))" @change="changeLanguage">
      <option value="zh">中文</option>
      <option value="en">English</option>
      <option value="fr">Français</option>
    </select>
  </label>
</template>
<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
import { setLanguage, Language } from '@/i18n'
@Component
export default class LanguageSwitcher extends Vue {
  changeLanguage(event: Event) {
    setLanguage((event.target as HTMLSelectElement).value as Language)
  }
}
</script>
<style scoped>
.language-switcher { display: inline-flex; align-items: center; gap: 8px; font-size: 14px; color: #333; }
select { padding: 6px 8px; border: 1px solid #ccc; border-radius: 4px; background: #fff; color: #333; cursor: pointer; }
</style>
