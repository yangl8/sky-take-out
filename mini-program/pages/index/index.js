import navBar from "../common/Navbar/navbar.vue"
import Phone from "@/components/uni-phone/index.vue"
import popMask from "./components/popMask.vue"
import popCart from "./components/popCart.vue"
import dishDetail from "./components/dishDetail.vue"
import {
	userLogin,
	getCategoryList,
	dishListByCategoryId,
	querySetmeaList,
	getShoppingCartList,
	newAddShoppingCartAdd,
	newShoppingCartSub,
	delShoppingCart,
	querySetmealDishById,
	getShopStatus,
	getMerchantInfo,
} from "../api/api.js"
import { mapState, mapMutations } from "vuex"
import { baseUrl } from "../../utils/env"
import prepareMultiFlavorPopup from "../../utils/multiFlavorPopup.js"
import aggregateMenuCartQuantities from "../../utils/menuCartQuantities.js"
import getShopClosedNoticeState from "../../utils/shopClosedNotice.js"
export default {
	data() {
		return {
			title: "Hello",
			openOrderCartList: false,
			typeListData: [],
			dishListData: [],
			dishListItems: [],
			dishDetailes: {},
			openDetailPop: false,
			openMoreNormPop: false,
			moreNormDataes: null,
			tableInfo: null,
			moreNormDishdata: null,
			moreNormdata: [],
			dishMealData: null,
			openTablePeoPleNumber: 1,
			orderData: 0,
			typeIndex: 0,
			openTablePop: false,
			flavorDataes: [],
			orderDishNumber: 0,
			orderDishPrice: 0,
			params: {
				shopId: "f3deb",
				storeId: "1282344676983062530",
				tableId: "1282346960773238786",
			},
			rightIdAndType: {},
			phoneData: "",
			tablewareNumber: 0,
			shopStatusTimer: null,
			shopStatusRequestSequence: 0,
			shopStatusRequestInFlight: false,
			shopStatusPageVisible: false,
			shopStatusWasVisibleBeforeBackground: false,
			lastObservedShopStatus: null,
			shopClosedNoticeVisible: false,
			shopClosedNoticeDismissed: false,
			scrollTop: 0,
			menuHeight: 0,
			menuItemHeight: 0,
			itemId: "",
			arr: [],
		}
	},
	components: {
		navBar,
		Phone,
		popMask,
		popCart,
		dishDetail,
	},
	created() {
		this._handleAppBackground = () => {
			this.shopStatusWasVisibleBeforeBackground = this.shopStatusPageVisible
			if (this.shopStatusPageVisible) this.stopShopStatusSync()
		}
		this._handleAppForeground = () => {
			const pages = getCurrentPages()
			const currentPage = pages[pages.length - 1]
			const menuIsCurrentPage = currentPage && currentPage.route === 'pages/index/index'
			if (!this.shopStatusWasVisibleBeforeBackground && !menuIsCurrentPage) return
			this.shopStatusWasVisibleBeforeBackground = false
			this.startShopStatusSync()
		}
		uni.$on('app:background', this._handleAppBackground)
		uni.$on('app:foreground', this._handleAppForeground)
	},
	beforeDestroy() {
		uni.$off('app:background', this._handleAppBackground)
		uni.$off('app:foreground', this._handleAppForeground)
		this.stopShopStatusSync()
	},
	computed: {
		shopStatus() {
			return this.$store.state.shopStatus
		},
		orderListDataes: function () {
			return this.orderListData()
		},
		loaddingSt: function () {
			return this.lodding()
		},
		orderAndUserInfo: function () {
			let orderData = []
			Array.isArray(this.orderListDataes) &&
				this.orderListDataes.forEach((n, i) => {
					let userData = {}
					userData.nickName = n.name ?? ""
					userData.avatarUrl = n.image ?? ""
					userData.dishList = [n]
					const num = orderData.findIndex(
						(o) => o.nickName == userData.nickName
					)
					if (num != -1) {
						orderData[num].dishList.push(n)
					} else {
						orderData.push(userData)
					}
				})
			return orderData
		},
		ht: function () {
			return (
				uni.getMenuButtonBoundingClientRect().top +
				uni.getMenuButtonBoundingClientRect().height +
				7
			)
		},
	},
	onReady() {
		this.getMenuItemTop()
	},
	onLoad(options) {
		uni.onNetworkStatusChange(function (res) {
			if (res.isConnected == false) {
				uni.navigateTo({
					url: "/pages/nonet/index",
				})
			}
		})
		if (options) {
			if (!options.status && !options.formOrder) {
				this.getData()
			}
		}
	},
	onShow() {
		this.startShopStatusSync()
		if (this.token()) {
			this.init()
		}
	},
	onHide() {
		this.stopShopStatusSync()
	},
	onUnload() {
		this.stopShopStatusSync()
	},
	methods: {
		...mapMutations([
			"setShopInfo",
			"setShopPhone",
			"setShopStatus",
			"initdishListMut",
			"setStoreInfo",
			"setBaseUserInfo",
			"setLodding",
			"setToken",
			"setDeliveryFee",
		]),
		...mapState([
			"shopInfo",
			"shopPhone",
			"orderListData",
			"baseUserInfo",
			"lodding",
			"token", //token
			"deliveryFee",
		]),
		startShopStatusSync() {
			this.shopStatusPageVisible = true
			this.refreshShopStatus()
			if (this.shopStatusTimer === null) {
				this.shopStatusTimer = setInterval(() => {
					this.refreshShopStatus()
				}, 25000)
			}
		},
		loginSync() {
			return new Promise((resolve, reject) => {
				uni.login({
					success: (loginRes) => {
						if (loginRes.errMsg === "login:ok") {
							resolve(loginRes.code)
						}
					},
				})
			})
		},
		getData() {
			let res = wx.getMenuButtonBoundingClientRect()
			let _this = this
			this.getMerchantInfo()
			this.selectHeight = res.height
			if (this.token() === "") {
				uni.showModal({
					title: this.$t("home.loginTitle"),
					content: this.$t("home.loginRequired"),
					confirmText: this.$t("common.modalLogin"),
					showCancel: false,
					success(res) {
						if (res.confirm) {
							uni.getUserProfile({
								desc: _this.$t("home.login"),
								success: async function (userInfo) {
									_this.setBaseUserInfo(userInfo.userInfo)
									try {
										const jsCode = await _this.loginSync()
										const success = await userLogin({ code: jsCode })
										if (success.code === 1) {
											_this.setToken(success.data.token)
											_this.init()
										}
									} catch (err) {
										uni.showToast({ title: _this.$t("home.loginFailed"), icon: "none" })
									}
								},
								fail: function () {},
							})
						}
					},
				})
			}
		},

		async init() {
			if (this.typeIndex !== 0) {
				this.typeIndex = 0
			}

			getCategoryList().then((res) => {
				if (res && res.code === 1) {
					this.typeListData = [...res.data]
					if (res.data.length > 0) {
						this.getDishListDataes(res.data[this.typeIndex || 0])
					}
				}
			})
			this.getTableOrderDishListes()
		},
		async swichMenu(params, index) {
			if (this.arr.length == 0) {
				await this.getMenuItemTop()
			}
			if (index == this.typeIndex) return
			this.$nextTick(function () {
				this.typeIndex = index
				this.leftMenuStatus(index)
			})
			this.getDishListDataes(params, index)
		},
		getElRect(elClass, dataVal) {
			new Promise((resolve, reject) => {
				const query = uni.createSelectorQuery().in(this)
				query
					.select("." + elClass)
					.fields(
						{
							size: true,
						},
						(res) => {
							if (!res) {
								setTimeout(() => {
									this.getElRect(elClass)
								}, 10)
								return
							}
							this[dataVal] = res.height
							resolve()
						}
					)
					.exec()
			})
		},
		async leftMenuStatus(index) {
			this.typeIndex = index
			if (this.menuHeight == 0 || this.menuItemHeight == 0) {
				await this.getElRect("menu-scroll-view", "menuHeight")
				await this.getElRect("type_item", "menuItemHeight")
			}
			this.scrollTop =
				index * this.menuItemHeight +
				this.menuItemHeight / 2 -
				this.menuHeight / 2
		},
		getMenuItemTop() {
			new Promise((resolve) => {
				let selectorQuery = uni.createSelectorQuery()
				selectorQuery
					.selectAll(".class-item")
					.boundingClientRect((rects) => {
						if (!rects.length) {
							setTimeout(() => {
								this.getMenuItemTop()
							}, 10)
							return
						}
					})
					.exec()
			})
		},
		async getDishListDataes(params, index) {
			this.rightIdAndType = {}
			this.rightIdAndType = {
				id: params.id,
				type: params.type,
			}
			const param = {
				categoryId: params.id,
			}
			if (params.type === 1) {
				await dishListByCategoryId(param)
					.then((res) => {
						if (res && res.code === 1) {
							this.dishListData =
								res.data &&
								res.data.map((obj) => ({
									...obj,
									type: 1,
									newCardNumber: 0,
								}))
						}
					})
					.catch((err) => { })
			} else {
				await querySetmeaList(param)
					.then((success) => {
						if (success && success.code === 1) {
							this.dishListData =
								success.data &&
								success.data.map((obj) => ({
									...obj,
									type: 2,
									newCardNumber: 0,
								}))
						}
					})
					.catch((err) => { })
			}
			if (typeof index === "number") {
				this.typeIndex = index
			}
			this.setOrderNum()
		},
		// Retain the last known shop status on request failure; unknown status must never be treated as open
		async refreshShopStatus() {
			if (!this.shopStatusPageVisible || this.shopStatusRequestInFlight) return this.shopStatus
			this.shopStatusRequestInFlight = true
			const requestSequence = ++this.shopStatusRequestSequence
			try {
				const res = await getShopStatus({ _refresh: Date.now() })
				if (!this.shopStatusPageVisible || requestSequence !== this.shopStatusRequestSequence) {
					return this.shopStatus
				}
				const status = res && res.code === 1 && (res.data === 0 || res.data === 1)
					? res.data
					: null
				if (status === null) return this.shopStatus
				const noticeState = getShopClosedNoticeState(
					this.lastObservedShopStatus,
					status,
					this.shopClosedNoticeVisible,
					this.shopClosedNoticeDismissed
				)
				this.shopClosedNoticeVisible = noticeState.visible
				this.shopClosedNoticeDismissed = noticeState.dismissed
				this.lastObservedShopStatus = status
				this.setShopStatus(status)
				return status
			} catch (error) {
				return this.shopStatus
			} finally {
				if (requestSequence === this.shopStatusRequestSequence) {
					this.shopStatusRequestInFlight = false
				}
			}
		},
		// Stop polling and invalidate in-flight requests when the page is hidden or unloaded
		stopShopStatusSync() {
			this.shopStatusPageVisible = false
			this.shopStatusRequestSequence += 1
			this.shopStatusRequestInFlight = false
			if (this.shopStatusTimer !== null) {
				clearInterval(this.shopStatusTimer)
				this.shopStatusTimer = null
			}
		},
		dismissShopClosedNotice() {
			this.shopClosedNoticeVisible = false
			this.shopClosedNoticeDismissed = true
		},
		async getMerchantInfo() {
			await getMerchantInfo()
				.then((res) => {
					if (res && res.code === 1) {
						this.phoneData = res.data.phone
						this.setShopPhone(res.data.phone)
						this.setShopInfo(res.data)
						this.setDeliveryFee(res.data.deliveryFee)
					}
				})
				.catch((err) => { })
		},
		getNewImage(image) {
			return `${baseUrl}/common/download?name=${image}`
		},
		async getTableOrderDishListes() {
			await getShoppingCartList({})
				.then((res) => {
					if (res.code === 1) {
						this.initdishListMut(res.data)
						this.computOrderInfo()
						this.setOrderNum()
					}
				})
				.catch((err) => { })
		},
		goOrder() {
			uni.navigateTo({
				url: "/pages/order/index",
			})
		},
		async addDishAction(item, form) {
			this.openMoreNormPop = false
			this.tablewareNumber++
			this.dishDetailes.dishNumber++
			if (
				this.orderListDataes &&
				!this.orderListDataes.some((n) => n.id == item.dishId) &&
				this.flavorDataes.length > 0
			) {
				item.flavorRemark = JSON.stringify(this.flavorDataes)
			}
			let dishFlavorDatas = ""
			let flavorRemark = []
			if (item.flavorRemark) {
				flavorRemark = JSON.parse(item.flavorRemark)
			}
			if (item.dishFlavor !== "" && item.dishFlavor) {
				dishFlavorDatas = item.dishFlavor
			} else if (flavorRemark.length > 0) {
				dishFlavorDatas = flavorRemark.join(',')
			} else {
				dishFlavorDatas = null
			}
			let params = {
				dishFlavor: dishFlavorDatas,
			}
			if (item.type === 1) {
				params = {
					...params,
					dishId: item.id,
				}
			} else if (item.type === 2) {
				params = {
					setmealId: item.id,
				}
			} else if (form === "购物车") {
				// Each cart row has a unique ID, so update the selected row directly
				params = { id: item.id }
			}
			newAddShoppingCartAdd(params)
				.then((res) => {
					if (res.code === 1) {
						this.getTableOrderDishListes()
						this.getDishListDataes(this.rightIdAndType)
						this.flavorDataes = []
					}
				})
				.catch((err) => { })
		},
		addShop(item) {
			console.log(item);
			this.dishDetailes = item
			this.addDishAction(item, "普通")
		},
		async redDishAction(item, form) {
			let dishFlavorDatas = ""
			let flavorRemark = []
			if (item.flavorRemark) {
				flavorRemark = JSON.parse(item.flavorRemark)
			}
			if (item.dishFlavor !== "" && item.dishFlavor) {
				dishFlavorDatas = item.dishFlavor
			} else if (flavorRemark.length > 0) {
				dishFlavorDatas = flavorRemark[0]
			} else {
				dishFlavorDatas = null
			}
			let params = {
				dishFlavor: dishFlavorDatas,
			}
			if (item.type === 1) {
				params = {
					...params,
					dishId: item.id,
				}
			} else if (item.type === 2) {
				params = {
					// ...params,
					setmealId: item.id,
				}
			} else if (form === "购物车") {
				// Each cart row has a unique ID, so update the selected row directly
				params = { id: item.id }
			}
			if (form !== "购物车") {
				const cartItem = this.orderListDataes.find((cart) => {
					if (item.type === 1) {
						return cart.dishId === item.id &&
							(cart.dishFlavor || null) === dishFlavorDatas
					}
					return item.type === 2 && cart.setmealId === item.id
				})
				if (cartItem) {
					params = { id: cartItem.id }
				}
			}
			await newShoppingCartSub(params)
				.then((res) => {
					if (res.code === 1) {
						// Update the displayed quantity only after the request succeeds to keep it consistent with persisted data
						if (form !== "购物车" && this.dishDetailes.dishNumber > 0) {
							this.dishDetailes.dishNumber--
						}
						this.getTableOrderDishListes()
						this.getDishListDataes(this.rightIdAndType)
					}
				})
				.catch((err) => { })
		},
		clearCardOrder() {
			delShoppingCart()
				.then((res) => {
					this.openOrderCartList = false
					this.getTableOrderDishListes()
					this.getDishListDataes(this.rightIdAndType)
				})
				.catch((err) => { })
		},
		openDetailHandle(item) {
			this.dishDetailes = item
			if (item.type === 2) {
				querySetmealDishById({
					id: item.id,
				})
					.then((res) => {
						if (res.code === 1) {
							this.openDetailPop = true
							this.dishMealData = res.data
						}
					})
					.catch((err) => { })
			} else {
				this.openDetailPop = true
			}
		},
		dishClose() {
			this.openDetailPop = false
		},
		moreNormDataesHandle(item) {
			const popupData = prepareMultiFlavorPopup(item, this.isMultiSelectFlavor)
			this.moreNormDishdata = popupData.dish
			this.moreNormdata = popupData.moreNormdata
			this.flavorDataes = popupData.flavorDataes
			this.openDetailPop = false
			this.openMoreNormPop = true
		},
		isMultiSelectFlavor(group) {
			const name = String(group.name || '')
			return /忌口|不要|去除|exclude/i.test(name) ||
				(group.value || []).some(value => /^(不要|去掉|不加)/.test(String(value)))
		},
		// Dietary preferences allow multiple selections; spice level, temperature, and similar groups are single-select
		checkMoreNormPop(val) {
			const group = this.moreNormdata.find(obj => obj.value === val.obj ||
				obj.value.every((value, index) => value === val.obj[index]))
			let item = val.item
			const num = this.flavorDataes.findIndex((it) => it == item)
			if (group && !this.isMultiSelectFlavor(group)) {
				this.flavorDataes = this.flavorDataes.filter(value => !group.value.includes(value))
				this.flavorDataes.push(item)
			} else if (num === -1) {
				this.flavorDataes.push(item)
			} else {
				this.flavorDataes.splice(num, 1)
			}
		},
		closeMoreNorm(moreNormDishdata) {
			this.flavorDataes.splice(0, this.flavorDataes.length)
			this.openMoreNormPop = false
		},
		computOrderInfo() {
			let oriData = this.orderListDataes
			this.orderDishNumber = this.orderDishPrice = 0
			oriData.map((n, i) => {
				this.orderDishNumber += n.number
				this.orderDishPrice += n.number * n.amount
			})
			this.orderDishPrice = this.orderDishPrice
		},
		setOrderNum() {
			const menuItems = aggregateMenuCartQuantities(this.dishListData || [], this.orderListDataes)
			this.dishListData = menuItems
			if (this.dishListItems.length == 0) {
				this.dishListItems = menuItems
			} else {
				this.dishListItems.splice(0, this.dishListItems.length, ...menuItems)
			}
		},
		handlePhone(type) {
			this.$refs.phone.$refs.popup.open(type)
		},
		closePopup(type) {
			this.$refs.phone.$refs.popup.close(type)
		},
	},
}
