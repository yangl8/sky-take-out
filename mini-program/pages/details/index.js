
import {
	getOrderDetail,
	getOrderPage,
	repetitionOrder,
	delShoppingCart,
	reminderOrder,
	cancelOrder,
} from '../api/api.js'
import { mapState, mapMutations } from 'vuex'
import { baseUrl } from '../../utils/env'
import { call, userFacingErrorMessage } from '@/utils/index.js'
import Status from './components/status.vue'
import OrderDetail from "./components/orderDetail.vue"
import DeliveryInfo from "./components/deliveryInfo.vue"
import OrderInfo from "./components/orderInfo.vue"

export default {
	data() {
		return {
			showDisplay: false,
			rocallTime: '',
			textTip: '',
			showConfirm: false,
			showContactPrompt: false,
			orderDetailsData: {},
			timeout: false,
			isOvertime: false,
			orderId: null,
			returnHomeAfterDetail: false,
			fallbackSex: null,
			activeRadio: 0,
			time: null,
			isPayment: false,
			times: null,
			phone: ""
		}
	},

	computed: {
		orderListDataes: function () {
			return this.orderListData()
		},

		orderDataes: function () {
			let testList = []
			if (this.showDisplay === false) {
				if (this.orderListDataes.length > 2) {
					for (var i = 0; i < 2; i++) {
						testList.push(this.orderListDataes[i])
					}
				} else {
					testList = this.orderListDataes
				}
				return testList
			} else {
				return this.orderListDataes
			}
		}
	},
	components: { Status, OrderDetail, DeliveryInfo, OrderInfo },
	onLoad(options) {
		this.orderId = options.orderId
		this.returnHomeAfterDetail = options.fromSuccess === '1'
		this.fallbackSex = options.sex === undefined ? null : Number(options.sex)
		const selectedOrder = this.selectedOrder()
		if (selectedOrder && String(selectedOrder.id) === String(options.orderId)) {
			const merchant = this.shopInfo() || {}
			this.orderDetailsData = {
				...selectedOrder,
				shopName: selectedOrder.shopName || merchant.shopName,
				shopTelephone: selectedOrder.shopTelephone || merchant.phone,
				deliveryFee: selectedOrder.deliveryFee ?? merchant.deliveryFee ?? 0,
			}
			this.initdishListMut(selectedOrder.orderDetailList || [])
		}
		if (this.orderId) {
			this.getBaseData(this.orderId)
		} else {
			uni.showToast({ title: this.$t('order.operationFailed'), icon: 'none' })
		}
	},

	methods: {
		...mapMutations(['setOrderData', 'initdishListMut']),
		...mapState(['shopInfo', 'orderListData', 'shopPhone', 'orderData', 'selectedOrder']),
		async getBaseData(id) {
			uni.showLoading({ title: this.$t('common.loading'), mask: true })
			let failed = false
			try {
				const res = await getOrderDetail(id)
				if (res.code === 1 && res.data) this.applyOrderDetail(res.data)
			} catch (error) {
				try {
					const page = await getOrderPage({ page: 1, pageSize: 50 })
					const order = page.code === 1 && page.data && page.data.records
						? page.data.records.find(item => String(item.id) === String(id))
						: null
					if (order) {
						this.applyOrderDetail(order)
					} else {
						failed = true
					}
				} catch (fallbackError) {
					failed = true
				}
			} finally {
				uni.hideLoading()
			}
			if (failed && !this.orderDetailsData.id) {
				uni.showToast({ title: this.$t('order.operationFailed'), icon: 'none' })
			}
		},
		applyOrderDetail(order) {
			const merchant = this.shopInfo() || {}
			this.orderDetailsData = {
				...order,
				sex: order.sex === undefined || order.sex === null ? this.fallbackSex : order.sex,
				shopName: order.shopName || merchant.shopName,
				shopTelephone: order.shopTelephone || merchant.phone || this.shopPhone(),
				deliveryFee: order.deliveryFee ?? merchant.deliveryFee ?? 0,
			}
			this.initdishListMut(this.orderDetailsData.orderDetailList || [])
			if (this.orderDetailsData.status === 1 && this.orderDetailsData.orderTime) {
				this.runTimeBack(this.orderDetailsData.orderTime)
			}
		},

		handleReminder(val) {
			reminderOrder(val.id).then(res => {
				if (res.code === 1) {
					this.showConfirm = true
					this.textTip = this.$t('popup.reminderSent')
					this.$refs.commonPopup.open(val.type)
					this.orderId = val.id
				}
			})
		},
		cancel(type, obj) {
			cancelOrder(obj.id).then(res => {
				if (res.code === 1) {
					this.isPayment = true
					this.orderDetailsData = {
						...this.orderDetailsData,
						status: 6,
						payStatus: Number(this.orderDetailsData.payStatus) === 1 ? 2 : this.orderDetailsData.payStatus,
					}
					uni.showToast({ title: this.$t('popup.orderCancelled'), icon: 'none' })
					this.orderId = obj.id
				}
			}).catch(error => {
				uni.showToast({ title: userFacingErrorMessage(error && error.msg, this.$store.state.locale), icon: 'none' })
			})
		},
		handleCancel(val) {
			if (val.obj.status === 1 || val.obj.status === 2) {
				uni.showModal({
					title: this.$t('popup.cancelOrderTitle'),
					content: this.$t('popup.cancelOrderConfirm'),
					confirmText: this.$t('common.modalConfirm'),
					cancelText: this.$t('common.modalCancel'),
					success: ({ confirm }) => {
						if (confirm) this.cancel(val.type, val.obj)
					}
				})
			} else {
				this.phone = val.obj.shopTelephone || this.shopPhone() || (this.shopInfo() || {}).phone
				this.showConfirm = false
				this.showContactPrompt = true
				this.$refs.commonPopup.open(val.type)
				this.textTip = this.$t('popup.contactMerchantCancel', { phone: this.phone || '-' })
			}
		},
		async oneMoreOrder(id) {
			await delShoppingCart()
			repetitionOrder(id).then(res => {
				if (res.code === 1) {
					uni.redirectTo({
						url: '/pages/index/index'
					})
				}
			})
		},
		statusWord(status) {
			if (this.timeout && status === 1 || this.orderDetailsData.status === 6) {
				return this.$t('status.cancelled')
			}
			switch (status) {
				case 2:
					return this.$t('status.awaitingMerchant')
				case 3:
					return this.$t('status.merchantAccepted')
				case 4:
					return this.$t('status.delivering')
				case 5:
					return this.$t('status.completed')
			}
		},
		runTimeBack(time) {
			const end = Date.parse(time.replace(/-/g, "/"))

			const now = Date.parse(new Date())
			const m15 = 15 * 60 * 1000
			const msec = m15 - (now - end)
			if (msec < 0) {
				this.timeout = true
				clearTimeout(this.times)
				this.cancel('center', this.orderDetailsData)
			} else {
				let min = parseInt(msec / 1000 / 60 % 60)
				let sec = parseInt(msec / 1000 % 60)
				if (min < 10) {
					min = '0' + min
				} else {
					min = min
				}
				if (sec < 10) {
					sec = '0' + sec
				} else {
					sec = sec
				}
				this.rocallTime = min + ':' + sec
				let that = this
				if (min >= 0 && sec >= 0) {
					if (min === 0 && sec === 0) {
						this.timeout = true
						clearTimeout(this.times)
						this.cancel('center', this.orderDetailsData)
						return
					}
					this.times = setTimeout(function () {
						that.runTimeBack(time)
					}, 1000)
				}
			}
		},

		getNewImage(image) {
			return `${baseUrl}/common/download?name=${image}`
		},
		toggleDisplay() {
			this.showDisplay = !this.showDisplay
		},
		goBack() {
			if (this.returnHomeAfterDetail) {
				uni.reLaunch({ url: '/pages/index/index' })
				return
			}
			uni.navigateBack({ delta: 1 })
		},
		openPopuos(type) {
			this.$refs.commonPopup.open(type)
		},
		handleRefund(type) {
			this.showConfirm = false
			this.showContactPrompt = true
			this.openPopuos(type)
			this.textTip = this.$t('order.contactRefund')
		},
		handlePhone(type, phone) {
			if (phone) this.phone = phone
			if (!this.phone) {
				uni.showToast({ title: this.$t('order.operationFailed'), icon: 'none' })
				return
			}
			if (this.showContactPrompt) {
				this.showContactPrompt = false
				this.$refs.commonPopup.close()
				setTimeout(() => this.$refs.phone.open(type), 300)
			} else {
				this.$refs.phone.open(type)
			}
		},
		closePopup(type) {
			this.$refs.phone.close(type)
		},
		// closePopupInfo
		closePopupInfo(type) {
			this.showContactPrompt = false
			this.$refs.commonPopup.close(type)
			this.getBaseData(this.orderId)
		},
		handlePay(id) {
			const obj = {
				orderNumber: this.orderDetailsData.number,
				orderAmount: this.orderDetailsData.amount,
				orderTime: this.orderDetailsData.orderTime
			}
			this.setOrderData(obj)
			uni.redirectTo({
				url: '/pages/pay/index?orderId=' + id
			})
		},
		call() {
			this.closePopup()
			call(this.phone, {
				fail: (error) => {
					const message = String((error && error.errMsg) || '').toLowerCase()
					if (message.includes('cancel')) return
					uni.showToast({ title: this.$t('popup.callFailed'), icon: 'none' })
				},
				simulatedDialog: {
					content: this.$t('popup.simulatedCall', { phone: this.phone }),
					confirmText: this.$t('common.modalCall'),
					cancelText: this.$t('common.modalCancel'),
				}
			})
		},
	}
}
