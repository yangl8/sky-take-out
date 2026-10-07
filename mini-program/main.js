import Vue from 'vue'
import App from './App'
import store from './store'
import { translate } from './locales'
import { displayText } from './utils/displayText'
import "@/styles/common.scss"

Vue.config.productionTip = false
Vue.prototype.$store = store
Vue.prototype.$t = (key, params) => translate(store.state.locale, key, params)
Vue.prototype.$displayText = (value, kind) => displayText(value, store.state.locale, kind)

App.mpType = 'app'

const app = new Vue({
	store,
    ...App
})
app.$mount()
