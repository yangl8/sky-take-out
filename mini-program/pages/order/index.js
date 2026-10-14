import {
	getAddressBookDefault,
	queryAddressBookList,
	getEstimatedDeliveryTime,
	getShopStatus,
	submitOrderSubmit
} from '../api/api.js'
import {
	mapState,
	mapMutations,
} from 'vuex'
import {
	baseUrl
} from '../../utils/env'
import {
	getLableVal,
	userFacingErrorMessage

} from '../../utils/index.js'
import Pikers from '@/components/uni-piker/index.vue'
import AddressPop from "./components/address.vue"
import DishDetail from "./components/dishDetail.vue"
import DishInfo from "./components/dishInfo.vue"
import getCheckoutAddressState from '@/utils/checkoutAddress.js'
import getImmediateDeliveryStatusNotice from '@/utils/shopStatusDecision.js'
import {
	formatBeijingDateTime,
	formatBeijingTime,
	getBeijingDateKey,
	getBeijingWeekday,
	getScheduledDeliverySlots,
	normalizeOrderDateTime
} from '@/utils/scheduledDeliveryTime.js'
export default {
	data() {
		return {
			platform: 'ios',
			orderDishPrice: 0,
			openPayType: false,
			psersonUrl: '../../static/btn_waiter_sel.png',
			nickName: '',
			gender: 0,
			phoneNumber: '',
			address: '',
			remark: '',
			arrivalTime: '',
			orderTime: '',
			addressBookId: '',
			addressLabel: '',
			tagLabel: '',
			orderDishNumber: 0,
			showDisplay: false,
			type: 'center',
			expirationTime: '',
			// rocallTime:'',
			tablewareData: '无需餐具',
			tableware: '',
			packAmount: 0,
			value: [0, 0],
			timeValue: [0, 0],
			indicatorStyle: `height: 44px;color:#333`,
			tabIndex: 0,
			scrollinto: 'tab0',
			scrollH: 0,
			popleft: ['今天', '明天'],
			scheduleNeedsReselect: false,
			scheduleTimer: null,
			scheduleInitialized: false,
			beijingDateKey: '',
			visible: true,
			baseData: [
				'无需餐具', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10'
			],
			activeRadio: '无需餐具',
			radioGroup: ['依据餐量提供', '无需餐具'],
			newDateData: [],
			// styleType: 'button',
			textTip: '',
			showConfirm: false,
			phoneData: '15200000001',
			selectValue: 0,
			selectDateValue: 0,
			timeout: false,
			isTomorrow: false,
			isImmediateDelivery: true,
			status: 0,
			num: 0,
			weeks: [],
			scrollTop: 0,
			addressList: [],
			isHandlePy: false,
			shopStatusRequestSequence: 0,
			shopStatusPageVisible: false,
			deliveryStatusNoticeKey: ''
		}
	},
	computed: {
		orderListDataes: function () {
			return this.orderListData()
		},
		orderDataes: function () {
			let testList = []
			if (this.showDisplay === false) {
				if (this.orderListDataes.length > 3) {
					for (var i = 0; i < 3; i++) {
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
	created() {
		this.popleft = [this.$t('order.today'), this.$t('order.tomorrow')]
		this.baseData = [this.$t('order.noTableware'), '1', '2', '3', '4', '5', '6', '7', '8', '9', '10']
		this.tablewareData = this.$t('order.noTableware')
		this.activeRadio = this.$t('order.noTableware')
		this.radioGroup = [this.$t('order.provideByMeal'), this.$t('order.noTableware')]
		this.refreshBeijingDateLabels()

		this.getAddressList()
	},
	mounted() {
		this.countdown()
	},
	components: {
		Pikers,
		// Popup,
		AddressPop,
		DishDetail,
		DishInfo
	},
	async onLoad(options) {
		this.initPlatform()
		this.psersonUrl = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.avatarUrl
		this.nickName = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.nickName
		this.gender = this.$store.state.baseUserInfo && this.$store.state.baseUserInfo.gender
		this.remark = this.remarkData()
		this.init()
		if (this.addressData() && this.addressData().detail) {
			const newAddress = this.addressData()
			Object.assign(this, getCheckoutAddressState(newAddress, this.$store.state.locale, getLableVal))
		} else {
			await this.getAddressBookDefault()
		}

		await this.getEstimatedDeliveryTime();
		this.refreshScheduledTimeOptions()
		this.setArrivalTime(this.arrivalTime)
		this.setGender(this.gender)
	},
	onShow() {
		this.shopStatusPageVisible = true
		this.refreshShopStatus()
		this.refreshScheduledTimeOptions()
		this.startScheduledTimeRefresh()
	},
	onHide() {
		this.shopStatusPageVisible = false
		this.shopStatusRequestSequence += 1
		this.stopScheduledTimeRefresh()
	},
	onUnload() {
		this.shopStatusPageVisible = false
		this.shopStatusRequestSequence += 1
		this.stopScheduledTimeRefresh()
	},
	onReady() {
		uni.getSystemInfo({
			success: (res) => {
				this.scrollH = res.windowHeight - uni.upx2px(100)
			}
		})
	},
	methods: {
		...mapState(['orderListData', 'remarkData', 'addressData', 'storeInfo', 'shopInfo', 'deliveryFee', 'shopStatus']),
		...mapMutations(['setAddressBackUrl', 'setOrderData', 'setArrivalTime', 'setRemark', 'setGender', 'setShopStatus']),
		async refreshShopStatus() {
			const requestSequence = ++this.shopStatusRequestSequence
			try {
				const result = await getShopStatus({ _refresh: Date.now() })
				if (!this.shopStatusPageVisible || requestSequence !== this.shopStatusRequestSequence) {
					return { status: this.shopStatus(), refreshed: false }
				}
				const status = result && result.code === 1 && (result.data === 0 || result.data === 1)
					? result.data
					: null
				if (status === null) return { status: this.shopStatus(), refreshed: false }
				this.setShopStatus(status)
				return { status, refreshed: status !== null }
			} catch (error) {
				return { status: this.shopStatus(), refreshed: false }
			}
		},
		init() {
			this.computOrderInfo()

		},
		initPlatform() {
			const res = uni.getSystemInfoSync()
			this.platform = res.platform
		},
		async getEstimatedDeliveryTime() {
			try {
				const result = await getEstimatedDeliveryTime({ shopId: this.shopInfo().shopId, customerAddress: this.address })
				this.orderTime = result && result.code === 1 && result.data
					? result.data
					: formatBeijingDateTime(Date.now() + 30 * 60 * 1000, formatBeijingTime(Date.now() + 30 * 60 * 1000))
			} catch (error) {
				// If the estimate endpoint is unavailable, keep the Beijing-time fallback for immediate delivery
				const estimated = Date.now() + 30 * 60 * 1000
				this.orderTime = formatBeijingDateTime(estimated, formatBeijingTime(estimated))
			}
			this.arrivalTime = this.getTimePart(this.orderTime)
		},
		getTimePart(value) {
			const match = typeof value === 'string' && /(?:T|\s)(\d{2}:\d{2})/.exec(value)
			return match ? match[1] : formatBeijingTime(Date.now() + 30 * 60 * 1000)
		},
		refreshBeijingDateLabels() {
			const weekdays = ['common.sunday', 'common.monday', 'common.tuesday', 'common.wednesday', 'common.thursday', 'common.friday', 'common.saturday']
			this.popleft = [this.$t('order.today'), this.$t('order.tomorrow')]
			this.weeks = [0, 1].map(offset => this.$t(weekdays[getBeijingWeekday(Date.now(), offset)]))
		},
		refreshScheduledTimeOptions() {
			const now = Date.now()
			const dateKey = getBeijingDateKey(now)
			if (this.beijingDateKey && this.beijingDateKey !== dateKey) {
				if (!this.isImmediateDelivery) this.scheduleNeedsReselect = true
				this.isTomorrow = false
				this.tabIndex = 0
				this.arrivalTime = this.isImmediateDelivery ? this.getTimePart(this.orderTime) : ''
				if (!this.isImmediateDelivery) this.setArrivalTime('')
			}
			this.beijingDateKey = dateKey
			this.refreshBeijingDateLabels()
			const dayOffset = this.isTomorrow ? 1 : 0
			const slots = getScheduledDeliverySlots(now, dayOffset)
			this.newDateData = dayOffset === 0 ? [this.$t('order.immediateDelivery'), ...slots] : slots
			if (!this.isImmediateDelivery && this.arrivalTime && !slots.includes(this.arrivalTime)) {
				this.arrivalTime = ''
				this.selectValue = -1
				this.scheduleNeedsReselect = true
				this.setArrivalTime('')
			}
			if (!this.isImmediateDelivery && this.arrivalTime && slots.includes(this.arrivalTime)) {
				this.selectValue = slots.indexOf(this.arrivalTime) + (dayOffset === 0 ? 1 : 0)
			}
			if (!this.scheduleInitialized) {
				this.scheduleInitialized = true
				this.selectValue = dayOffset === 0 ? 0 : -1
			}
		},
		startScheduledTimeRefresh() {
			this.stopScheduledTimeRefresh()
			this.scheduleTimer = setInterval(() => this.refreshScheduledTimeOptions(), 30000)
		},
		stopScheduledTimeRefresh() {
			if (this.scheduleTimer) clearInterval(this.scheduleTimer)
			this.scheduleTimer = null
		},
		getAddressList() {
			this.testValue = false
			queryAddressBookList().then(res => {
				if (res.code === 1) {
					this.testValue = true
					this.addressList = res.data
				}
			})
		},
		getAddressBookDefault() {
			return getAddressBookDefault().then(res => {
				if (res.code === 1) {
					Object.assign(this, getCheckoutAddressState(res.data, this.$store.state.locale, getLableVal))
				}
			})
		},
		goAddress() {
			this.setAddressBackUrl('/pages/order/index')
			if (this.addressList.length === 0) {
				uni.redirectTo({
					url: '/pages/addOrEditAddress/addOrEditAddress'
				})
			} else {
				uni.redirectTo({
					url: '/pages/address/address'
				})
			}

		},
		getNewImage(image) {
			return `${baseUrl}/common/download?name=${image}`
		},
		computOrderInfo() {
			let oriData = this.orderListDataes
			this.orderDishNumber = this.orderDishPrice = 0
			this.orderDishPrice = 0
			oriData.map((n, i) => {
				// this.orderDishPrice += n.number * n.price
				this.orderDishPrice += n.number * n.amount
				this.orderDishNumber += n.number
				console.log(n);
			})
			this.orderDishPrice = this.orderDishPrice + this.deliveryFee() + this.orderDishNumber
		},
		goBack() {
			uni.navigateBack({ delta: 1 })
		},
		closeMask() {
			this.openPayType = false
		},
		toggleDisplay() {
			this.showDisplay = !this.showDisplay
		},
		async payOrderHandle() {
			if (this.isHandlePy) return false
			this.isHandlePy = true
			this.deliveryStatusNoticeKey = ''

			if (!this.address) {
				this.isHandlePy = false
				uni.showToast({
					title: this.$t('order.selectAddress'),
					icon: 'none',
				})
				return false
			}
			if (!this.validateScheduledTimeBeforeSubmit()) {
				this.isHandlePy = false
				return false
			}
			const currentShopStatus = await this.refreshShopStatus()
			if (this.isImmediateDelivery) {
				const statusNoticeKey = getImmediateDeliveryStatusNotice(currentShopStatus)
				if (statusNoticeKey) {
					this.isHandlePy = false
					this.deliveryStatusNoticeKey = statusNoticeKey
					return false
				}
			}
			if (!this.validateScheduledTimeBeforeSubmit()) {
				this.isHandlePy = false
				return false
			}
			const params = {
				payMethod: 1,
				addressBookId: this.addressBookId,
				remark: this.remark,
				estimatedDeliveryTime: this.isImmediateDelivery
					? this.normalizeServerDateTime(this.orderTime)
					: formatBeijingDateTime(Date.now(), this.arrivalTime, this.isTomorrow ? 1 : 0),
				deliveryStatus: this.isImmediateDelivery ? 1 : 0,
				tablewareStatus: this.status,
				tablewareNumber: this.num,
				packAmount: this.orderDishNumber,
				amount: this.orderDishPrice,
				shopId: this.shopInfo().shopId,
				deliveryFee: this.deliveryFee()
			}

			try {
				const res = await submitOrderSubmit(params)
				if (res.code === 1) {
					this.isHandlePy = false
					this.setOrderData(res.data)
					uni.navigateTo({
						url: '/pages/pay/index?orderId=' + res.data.id
					})
				} else {
					await this.handleOrderSubmissionError(res.msg)
				}
			} catch (error) {
				await this.handleOrderSubmissionError(error && (error.msg || (error.data && error.data.msg)))
			}
		},
		validateScheduledTimeBeforeSubmit() {
			this.refreshScheduledTimeOptions()
			if (this.isImmediateDelivery || (!this.scheduleNeedsReselect && this.arrivalTime)) return true
			uni.showToast({ title: this.$t('order.scheduledTimeExpired'), icon: 'none' })
			return false
		},
		normalizeServerDateTime(value) {
			return normalizeOrderDateTime(value, Date.now() + 30 * 60 * 1000)
		},
		async handleOrderSubmissionError(message) {
			if (typeof message === 'string' && message.includes('超出配送范围')) {
				this.isHandlePy = false
				this.deliveryStatusNoticeKey = 'order.outOfRange'
				return
			}
			if (typeof message === 'string' && message.includes('地址')) {
				this.isHandlePy = false
				this.deliveryStatusNoticeKey = 'order.addressUnavailable'
				return
			}
			const currentShopStatus = await this.refreshShopStatus()
			this.isHandlePy = false
			if (typeof message === 'string' && message.includes('店铺当前未营业')) {
				const noticeKey = getImmediateDeliveryStatusNotice(currentShopStatus, true)
				if (this.isImmediateDelivery && noticeKey) {
					this.deliveryStatusNoticeKey = noticeKey
				} else {
					uni.showToast({ title: this.orderErrorMessage(message), icon: 'none' })
				}
				return
			}
			const localizedMessage = this.orderErrorMessage(message)
			if (localizedMessage === this.$t('order.shopStatusUnavailable')) {
				this.deliveryStatusNoticeKey = 'order.shopStatusUnavailable'
				return
			}
			uni.showToast({ title: localizedMessage, icon: 'none' })
		},
		dismissDeliveryStatusNotice() {
			this.deliveryStatusNoticeKey = ''
		},
		orderErrorMessage(message) {
			return userFacingErrorMessage(message, this.$store.state.locale)
		},
		call() {
			uni.makePhoneCall({
				phoneNumber: '114'
			})
		},
		handleContact(type) {
			this.showConfirm = false
			this.openPopuos(type)
			this.textTip = this.$t('popup.contactMerchant')
		},
		handleRefund(type) {
			this.showConfirm = false
			this.openPopuos(type)
			this.textTip = this.$t('order.contactRefund')
		},
		goRemark() {
			this.setAddressBackUrl('/pages/order/index')
			uni.redirectTo({
				url: '/pages/remark/index'
			})
		},
		openPopuos(type) {
			this.$refs.popup.open(type)
		},
		closePopup(type) {
			this.$refs.popup.close(type)
		},
		change(e) {
		},
		handlePiker() {
			if (this.tableware !== '') {
				this.num = Number(this.tableware)
				this.status = 0
				if (this.tableware === this.$t('order.noTableware')) {
					this.num = 0
					this.status = 0
				}
				if (this.tableware === this.$t('order.provideByMeal')) {
					this.num = this.orderDishNumber
					this.status = 1
				}

				if (this.tableware !== this.$t('order.provideByMeal') && this.tableware !== this.$t('order.noTableware')) {
					this.tablewareData = this.$t('order.tablewareCount', { count: this.tableware })
				} else {
					this.tablewareData = this.tableware
				}
			} else {
				let cont = this.baseData[this.$refs.dishinfo.$refs.piker.defaultValue[0]]
				this.tablewareData = cont
				if (this.activeRadio === this.$t('order.provideByMeal')) {
					this.num = this.orderDishNumber
					this.status = 1
				} else {
					this.num = 0
					this.status = 0
				}
			}
		},
		changeCont(val) {
			this.tableware = val
		},
		handleRadio(e) {
			this.activeRadio = e.detail.value
		},
		countdown() {
			const end = Date.parse(new Date())
		},
		dateChange(index) {
			this.isTomorrow = index === 1
			this.tabIndex = index
			if (this.isTomorrow && this.isImmediateDelivery) {
				this.isImmediateDelivery = false
				this.arrivalTime = ''
				this.selectValue = -1
				this.scheduleNeedsReselect = true
				this.setArrivalTime('')
			}
			this.refreshScheduledTimeOptions()
		},
		timeClick: function (val) {
			this.selectValue = val.i
			this.setTime(val.val)
		},
		setTime(val) {
			this.deliveryStatusNoticeKey = ''
			if (val === this.$t('order.immediateDelivery')) {
				this.arrivalTime = this.getTimePart(this.orderTime)
				this.isImmediateDelivery = true
				this.scheduleNeedsReselect = false
			} else {
				this.arrivalTime = val
				this.isImmediateDelivery = false
				this.scheduleNeedsReselect = false
			}

			this.setArrivalTime(this.arrivalTime)

		},
		touchstart(e) {
			if (e.changedTouches[0].clientY > 400) {
			}
		}
	}
}
