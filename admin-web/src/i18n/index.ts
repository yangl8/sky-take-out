import Vue from 'vue'
import VueI18n from 'vue-i18n'
import elementEn from 'element-ui/lib/locale/lang/en'
import elementFr from 'element-ui/lib/locale/lang/fr'
import elementZh from 'element-ui/lib/locale/lang/zh-CN'
import en from './locales/en'
import fr from './locales/fr'
import zh from './locales/zh'

Vue.use(VueI18n)

export type Language = 'en' | 'fr' | 'zh'
const storageKey = 'admin-web.language'

export function resolveLanguage(saved: string | null, languages: readonly string[]): Language {
  if (saved === 'en' || saved === 'fr' || saved === 'zh') return saved
  for (const language of languages) {
    const code = language.toLowerCase().split(/[-_]/)[0]
    if (code === 'en' || code === 'fr' || code === 'zh') return code
  }
  return 'en'
}

function initialLanguage(): Language {
  let saved: string | null = null
  try {
    saved = window.localStorage.getItem(storageKey)
  } catch (_) {
    // Browser preferences remain available when storage is restricted.
  }
  return resolveLanguage(saved, navigator.languages || [navigator.language])
}

const i18n = new VueI18n({
  locale: initialLanguage(),
  fallbackLocale: 'en',
  messages: {
    en: { ...elementEn, ...en },
    fr: { ...elementFr, ...fr },
    zh: { ...elementZh, ...zh }
  }
})

document.documentElement.lang = i18n.locale

export function setLanguage(language: Language): void {
  if (language !== 'en' && language !== 'fr' && language !== 'zh') return
  i18n.locale = language
  document.documentElement.lang = language
  try {
    window.localStorage.setItem(storageKey, language)
  } catch (_) {
    // Switching still works for this session when storage is restricted.
  }
}

export default i18n
