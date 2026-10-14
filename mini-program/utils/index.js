export const call = (val, callbacks = {}) => {
	const { simulatedDialog, ...phoneCallbacks } = callbacks
	if (uni.getSystemInfoSync().platform === 'devtools' && simulatedDialog) {
		return uni.showModal(simulatedDialog)
	}
	return uni.makePhoneCall({
		phoneNumber: val,
		...phoneCallbacks,
	})
}

export const userFacingErrorMessage = (message, locale = 'zh', fallbackKey = 'order.operationFailed') => {
	if (typeof message !== 'string' || !message.trim()) {
		return translate(locale, fallbackKey)
	}
	if (message.includes('营业状态暂不可用')) return translate(locale, 'order.shopStatusUnavailable')
	if (message.includes('店铺当前未营业')) return translate(locale, 'order.shopClosed')
	if (message.includes('预约送达时间不足')) return translate(locale, 'order.deliveryTimeUnavailable')
	if (message.includes('请选择有效的预约送达时间')) return translate(locale, 'order.deliveryTimeUnavailable')
	if (message.includes('不在允许营业时段')) return translate(locale, 'order.deliveryTimeUnavailable')
	if (message.includes('配送方式无效')) return translate(locale, 'order.deliveryTimeUnavailable')
	if (message.includes('超出配送范围')) return translate(locale, 'order.outOfRange')
	if (message.includes('购物车') && (message.includes('空') || message.includes('不能下单'))) {
		return translate(locale, 'order.emptyCart')
	}
	if (message.includes('地址')) return translate(locale, 'order.addressUnavailable')
	if (message.includes('菜品') || message.includes('套餐') || message.includes('商品')) {
		return translate(locale, 'order.cartItemsUnavailable')
	}
	if (message.includes('订单')) return translate(locale, 'order.orderUnavailable')
	return translate(locale, fallbackKey)
}

export const splitMobile = (mobile) => {
	return String(mobile).replace(/(?=(\d{4})+$)/g, '-')
}

export const getLableVal = (item, locale = 'zh') => {
	switch (item) {
		case '1':
			return translate(locale, 'address.company')
		case '2':
			return translate(locale, 'address.home')
		case '3':
			return translate(locale, 'address.school')
		default:
			return translate(locale, 'address.other')
	}
}
// const orderStatus = {
// };
// const waybillStatus = {
// };
export const statusWord = (status, time, locale = 'zh', payStatus) => {
	if (payStatus === 2) {
		return translate(locale, 'status.refunded')
	}
	if (time) {
		if (status === 1 && time > 0) {
			return translate(locale, 'status.pendingPayment')
		} else if (status === 6 || (time < 0 && status === 1)) {
			return translate(locale, 'status.cancelled')
		}
	}
	switch (status) {
		case 1:
			return translate(locale, 'status.pendingPayment')
		case 2:
			return translate(locale, 'status.awaitingMerchant')
		case 3:
			return translate(locale, 'status.awaitingCourier')
		case 4:
			return translate(locale, 'status.delivering')
		case 5:
			return translate(locale, 'status.completed')
		case 6:
			return translate(locale, 'status.cancelled')
		case 7:
			return translate(locale, 'status.refunded')
	}
	return ''
}
export const runTimeBack = () => {
	//  var nowTime = new Date();
	//  var createdTime = new Date(val);
	//  var TIME = 60 * Number(timeInterval)*1000;
	//  var differ = nowTime - createdTime;
	//  if (differ < 0 || differ > TIME) {
	// return 0;
	//  };
	//  differ = TIME - differ;
	//  var minute = Math.floor(differ / (60 * 1000));
	//  console.log(minute,123)
	//  minute = minute < 10 ? '0' + minute : minute;
	//  var second = Math.floor((differ - minute * (60 * 1000)) / 1000);
	//  second = second < 10 ? '0' + second : second;

	//  return minute + ':' + second;



}
export const getOvertime = (time) => {
	const end = Date.parse(new Date(time.replace(/-/g, "/")))
	const now = Date.parse(new Date())
	const m15 = 15 * 60 * 1000
	const msec = m15 - (now - end)
	return msec
}
// export const runTime=()=>{
// 	const end = Date.parse(new Date(this.orderData().orderTime))
// 	const now = Date.parse(new Date())
// 	const m15 = 15*60*1000
// 	const msec = m15-(now - end)
// 	if(msec < 0) {
// 		this.timeout=true
// 	}else{
// 		let min = parseInt(msec/1000/60%60)
// 		let sec = parseInt(msec/1000%60)
// 		if(sec<10){
// 			sec= '0'+sec
// 		}else{
// 			sec=sec
// 		}
// 		this.rocallTime=min + ':' +sec
// 		let that = this
// 		if(min>=0 &&sec>=0){
// 			if(min===0&& sec===0){
// 				this.timeout=true
// 				return
// 			}
// 			setTimeout(function(){
// 				that.runTimeBack()
// 			},1000)
// 		}
// 	}
// }
export const getWeekDate = (date, locale = 'zh') => {
	let now = new Date(date);
	let day = now.getDay()
	let weeks = [
		translate(locale, 'common.sunday'),
		translate(locale, 'common.monday'),
		translate(locale, 'common.tuesday'),
		translate(locale, 'common.wednesday'),
		translate(locale, 'common.thursday'),
		translate(locale, 'common.friday'),
		translate(locale, 'common.saturday'),
	]
	let week = weeks[day]
	return week

}
export const presentFormat = () => {
	var date = new Date();
	date.setTime(date.getTime() + 3600000)
	var year = date.getFullYear();
	var month = date.getMonth() + 1;
	var day = date.getDate();
	var hour = date.getHours();
	var minute = date.getMinutes();
	var second = date.getSeconds()
	var time = + year + '-' + addZero(month) + '-' + addZero(day) + ' ' + addZero(hour) + ':' + addZero(minute) + ':' + addZero(second);
	debugger
	return time;
}
function addZero(s) {
	return s < 10 ? ('0' + s) : s;
}
export const dateFormat = (b, time) => {
	let t = new Date()
	let date = null
	if (b) {
		date = new Date(t.setDate(t.getDate() + 1))
	} else {
		date = t;
	}

	let ret;
	let y = date.getFullYear().toString()
	let m = (date.getMonth() + 1).toString()
	let d = date.getDate().toString()
	if (m < 10) {
		m = '0' + m
	}
	if (d < 10) {
		d = '0' + d
	}

	ret = y + '-' + m + '-' + d + ' ' + time + ':00'
	console.log(ret, 44)
	return ret;
}
import { translate } from "@/locales";
