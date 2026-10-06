
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
import { call } from '@/utils/index.js'
import Status from './components/status.vue'//订单状态
import OrderDetail from "./components/orderDetail.vue" //菜品详情
import DeliveryInfo from "./components/deliveryInfo.vue" //配送信息
import OrderInfo from "./components/orderInfo.vue" //订单信息

export default {
	data() {
		return {
			showDisplay: false,
			rocallTime: '',
			textTip: '',
			showConfirm: false,
			orderDetailsData: {},
			timeout: false,
			isOvertime: false,
			orderId: null,
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

		// // 处理订单详情列表
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
	//   组件
	components: { Status, OrderDetail, DeliveryInfo, OrderInfo },
	onLoad(options) {
		this.orderId = options.orderId
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
		// 获取订单详情
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
				shopName: order.shopName || merchant.shopName,
				shopTelephone: order.shopTelephone || merchant.phone || this.shopPhone(),
				deliveryFee: order.deliveryFee ?? merchant.deliveryFee ?? 0,
			}
			this.initdishListMut(this.orderDetailsData.orderDetailList || [])
			if (this.orderDetailsData.status === 1 && this.orderDetailsData.orderTime) {
				this.runTimeBack(this.orderDetailsData.orderTime)
			}
		},

		// 催单
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
		// 取消订单接口
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
				uni.showToast({ title: error.msg || this.$t('order.operationFailed'), icon: 'none' })
			})
		},
		// 取消订单
		handleCancel(val) {
			if (val.obj.status === 1 || val.obj.status === 2) {
				uni.showModal({
					title: this.$t('popup.cancelOrderTitle'),
					content: this.$t('popup.cancelOrderConfirm'),
					confirmText: this.$t('common.confirm'),
					cancelText: this.$t('common.cancel'),
					success: ({ confirm }) => {
						if (confirm) this.cancel(val.type, val.obj)
					}
				})
			} else {
				this.phone = val.obj.shopTelephone || this.shopPhone() || (this.shopInfo() || {}).phone
				this.showConfirm = false
				this.$refs.commonPopup.open(val.type)
				this.textTip = this.$t('popup.contactMerchantCancel', { phone: this.phone || '-' })
			}
		},
		// 再来一单
		async oneMoreOrder(id) {
			// 先清空购物车
			await delShoppingCart()
			repetitionOrder(id).then(res => {
				if (res.code === 1) {
					uni.redirectTo({
						url: '/pages/index/index'
					})
				}
			})
		},
		// 处理状态
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
		// 订单倒计时
		runTimeBack(time) {
			const end = Date.parse(time.replace(/-/g, "/"))

			const now = Date.parse(new Date())
			const m15 = 15 * 60 * 1000
			const msec = m15 - (now - end)
			if (msec < 0) {
				this.timeout = true
				clearTimeout(this.times)
				this.cancel('center', this.orderDetailsData) //超时的时候取消订单
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
						this.cancel('center', this.orderDetailsData) //超时的时候取消订单
						return
					}
					this.times = setTimeout(function () {
						that.runTimeBack(time)
					}, 1000)
				}
			}
		},

		// 重新拼装image
		getNewImage(image) {
			return `${baseUrl}/common/download?name=${image}`
		},
		// 展开或收起订单中的全部商品
		toggleDisplay() {
			this.showDisplay = !this.showDisplay
		},
		// 返回上一级
		goBack() {
			uni.navigateBack({ delta: 1 })
		},
		openPopuos(type) {
			this.$refs.commonPopup.open(type)
		},
		// 联系商家进行退款弹层
		handleRefund(type) {
			this.showConfirm = false
			this.openPopuos(type)
			this.textTip = this.$t('order.contactRefund')
		},
		// 拨打电话弹层
		handlePhone(type, phone) {
			if (phone) this.phone = phone
			if (!this.phone) {
				uni.showToast({ title: this.$t('order.operationFailed'), icon: 'none' })
				return
			}
			this.$refs.phone.open(type)
		},
		// 关闭弹层
		closePopup(type) {
			this.$refs.phone.close(type)
		},
		// closePopupInfo
		closePopupInfo(type) {
			this.$refs.commonPopup.close(type)
			this.getBaseData(this.orderId)
		},
		// 立即支付
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
			call(this.phone)
		},
	}
}
