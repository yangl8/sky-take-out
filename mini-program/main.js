import Vue from 'vue'
import App from './App'
import store from './store'
import { translate } from './locales'
import "@/styles/common.scss"

Vue.config.productionTip = false
Vue.prototype.$store = store
Vue.prototype.$t = (key, params) => translate(store.state.locale, key, params)

App.mpType = 'app'

const app = new Vue({
	store,
    ...App
})
app.$mount()
