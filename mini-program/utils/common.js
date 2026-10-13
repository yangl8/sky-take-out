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
		handleReminder(type,id){
			reminderOrder(id).then(res => {
				if(res.code===1){
					this.showConfirm=true
					this.textTip=this.$t('popup.reminderSent')
					this.$refs.commonPopup.open(type)
				}
			})
		},
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
		async oneMoreOrder(id){
			await delShoppingCart()
			repetitionOrder(id).then(res => {
				if (res.code === 1) {
					uni.redirectTo({
					  url: '/pages/index/index'
					})
				}
			})
		},
		handlePhone(type) {
			// this.$refs.phone.open(type)
		},
		closePopup(type){
			this.$refs.commonPopup.close(type)
		},
	}
}
