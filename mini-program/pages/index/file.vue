<template>
	<view style="display: flex;">
		<food-menu :tabBars="tabBars" :tabIndexShow="tabIndexShow" :swiperWidth="menuWidth" :swiperHeight="swiperHeight" @tabtap="tabtap"></food-menu>
		<scroll-view scroll-y="true" @scroll="scrollGet($event)" scroll-with-animation :scroll-into-view="'tab_'+tabIndex"
		 :style="{height:swiperHeight+'px',width:foodWidth+'px'}" style="padding-top: 20px;">
			<block v-for="(items,index) in foodsList" :key="index">
				<view :id="'tab_'+tabBars[index].id">
					<view style="font-size: 30upx;height: 40upx;margin-left: 20upx;">
						{{tabBars[index].name}}
					</view>
					<template v-if="items.list.length>0">
						<block v-for="(item,index1) in items.list" :key="index1">
							<food-component @showModal="showModal" :item="item"></food-component>
						</block>
					</template>
					<template v-else>
						该菜单尚未添加商品
					</template>
				</view>
			</block>
		</scroll-view>
	</view>
</template>

<script>
	import foodComponent from "./food-component.vue"
	import foodMenu from "./food-menu.vue"

	export default {
		components: {
			foodComponent,
			foodMenu
		},
		data() {
			return {
				swiperHeight: 0,
				menuWidth: 0,
				foodWidth: 0,
				tabIndexShow: 0,
				tabIndex: 0,
				scroNum: [],
				tabBars: [{
						name: "粑粑",
						id: "0"
					},
					{
						name: "粥粥",
						id: "1"
					},
					{
						name: "包包",
						id: "2"
					},
					{
						name: "面面",
						id: "3"
					},
					{
						name: "粉粉",
						id: "4"
					},
					{
						name: "肉肉",
						id: "5"
					},
					{
						name: "套餐",
						id: "6"
					},

					{
						name: "苍穹",
						id: "7"
					}
					,
					{
						name: "肉肉2",
						id: "8"
					},{
						name: "肉肉3",
						id: "9"
					},{
						name: "肉肉4",
						id: "10"
					},
				],
				foodsList: [{
						list: [{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						},{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						},{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						},{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						},{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						},{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						},{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
							},{
							food_name: "小米南瓜粥",
							food_price: 13.5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						}]
					},
					{
						list: [{
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, {
							food_name: "黑米粥",
							food_price: 199,
							food_image: "../../static/test.jpg",
							title: "我是标题",
							num: 100
						}, ]
					},
					{
						list: [{
							food_name: "白米粥",
							food_price: 299,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100,
							priority: 11,
							display: 1,
						}, ]
					},
					{
						list: [{
							food_name: "包子",
							food_price: 5,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						}, ]
					},
					{
						list: [{
							food_name: "饺子",
							food_price: 100,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						}, ]
					},
					{
						list: [{
							food_name: "肉肉",
							food_price: 999,
							food_image: "../../static/logo.jpg",
							title: "我是标题",
							num: 100
						}, ]
					}
				]
			}
		},
		onLoad() {
			uni.getSystemInfo({
				success: (res) => {
					let height = res.windowHeight
					let windowWidth = res.windowWidth
					this.swiperHeight = height - uni.upx2px(153)
					this.menuWidth = windowWidth / 5
					this.foodWidth = windowWidth * 4 / 5
				}
			});
		},
		mounted() {
			this.loadScroNum();
		},
		methods: {
			loadScroNum() {
				for (var i = 0; i < this.tabBars.length; i++) {
					wx.createSelectorQuery().select('#tab_' + i).boundingClientRect(e => {
						this.scroNum.push({
							top: e.top,
							bot: e.top + e.height
						})
					}).exec()
				}
			},
			tabtap(index) {
				this.tabIndexShow = index;
				this.tabIndex = index;
			},
			scrollGet(e) {
				var now = parseInt(e.detail.scrollTop) + 40
				for (var i = 0; i < this.tabBars.length; i++) {
					if (now > this.scroNum[i].top && now < this.scroNum[i].bot) {
						this.tabIndexShow = i
						return
					}
				}
			}
		}
	}
</script>

<style>
	.swiper-tab-list {
		width: 100%;
		text-align: center;
		color: #808080;
		font-size: 36upx;
		height: 160upx;
		line-height: 160upx;
	}

	.active {
		color: #FEDE33;
	}
</style>