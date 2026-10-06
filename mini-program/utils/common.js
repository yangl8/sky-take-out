	import {  
		reminderOrder,
		cancelOrder,
		delShoppingCart,
		repetitionOrder
	} from '../pages/api/api.js'
export default {
	data(){
		return {
			textTip:'',
			showConfirm:false,
		}
	},
	// created(){
	// },
	methods: {
		// 催单
		handleReminder(type,id){
			reminderOrder(id).then(res => {
				if(res.code===1){
					this.showConfirm=true
					this.textTip=this.$t('popup.reminderSent')
					this.$refs.commonPopup.open(type)
				}
			})
		},
		// 取消订单
		handleCancel(type,obj){
			if(obj===1){
				cancelOrder(obj.id).then(res => {
					if(res.code===1){
						this.showConfirm=true
						this.textTip=this.$t('popup.orderCancelled')
						this.$refs.commonPopup.open(type)
					}
				})
			}else{
				this.showConfirm=false
				this.$refs.commonPopup.open(type)
				this.textTip=this.$t('popup.contactMerchant')
			}	
		},
		// 再来一单
		async oneMoreOrder(id){
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
		// 拨打电话弹层
		handlePhone(type) {
			// 暂时关闭打电话
			// this.$refs.phone.open(type)
		},
		// 关闭弹层
		closePopup(type){
			this.$refs.commonPopup.close(type)
		},
	}
}
