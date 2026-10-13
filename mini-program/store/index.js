import Vue from 'vue'
import Vuex from 'vuex'
import { getInitialLocale, saveLocale } from '@/locales'

Vue.use(Vuex)

const store = new Vuex.Store({
	state: {
		locale: getInitialLocale(),
		storeInfo: {},
		shopInfo: '',
		orderListData: [],
		baseUserInfo: '',
		lodding: false,
		sessionId: '',
		addressBackUrl: '',
		dishTypeIndex: 0,
		shopPhone: '',
		shopStatus: null,
		orderData: {},
		selectedOrder: {},
		token: '',
		arrivals: '',
		remarkData: '',
		addressData: {},
		deliveryFee: 0,
		gender: 0
	},
	mutations: {
		setLocale(state, locale) {
			state.locale = locale
			saveLocale(locale)
		},
		setStoreInfo(state, provider) {
			state.storeInfo = provider
		},
		setShopInfo(state, provider) {
			state.shopInfo = provider
		},
		initdishListMut(state, provider) {
			state.orderListData = provider
		},
		setBaseUserInfo(state, provider) {
			state.baseUserInfo = provider
		},
		setLodding(state, provider) {
			state.lodding = provider
		},
		setSessionId(state, provider) {
			state.sessionId = provider
		},
		setAddressBackUrl(state, provider) {
			state.addressBackUrl = provider
		},
		setDishTypeIndex(state, provider) {
			state.dishTypeIndex = provider
		},
		setShopPhone(state, provider) {
			state.shopPhone = provider
		},
		setShopStatus(state, provider) {
			state.shopStatus = provider
		},
		setOrderData(state, provider) {
			state.orderData = provider

		},
		setSelectedOrder(state, provider) {
			state.selectedOrder = provider
		},
		setToken(state, provider) {
			state.token = provider
		},
		setArrivalTime(state, provider) {
			state.arrivals = provider
		},
		setRemark(state, provider) {
			state.remarkData = provider
		},
		setAddress(state, provider) {
			state.addressData = provider
		},
		setDeliveryFee(state, deliveryFee) {
			state.deliveryFee = deliveryFee
		},
		setGender(state, gender) {
			state.gender = gender
		}
	},
	actions: {

	}
})

export default store
