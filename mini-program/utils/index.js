// 拨打电话
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

// 将后端业务错误转换为当前语言的用户提示
export const userFacingErrorMessage = (message, locale = 'zh', fallbackKey = 'order.operationFailed') => {
	if (typeof message !== 'string' || !message.trim()) {
		return translate(locale, fallbackKey)
	}
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

// 分割电话号码
export const splitMobile = (mobile) => {
	return String(mobile).replace(/(?=(\d{4})+$)/g, '-')
}

// 判断地址
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
// status // 1待付款 2待接单 3 已接单 4 派送中 5 已完成 6 已取消 7 退款
// waybillStatus '运单状态：1调度成功，骑手未接单  2 骑手接单，待取货状态 3. 待送达 骑手到商家取完货，运单属于待送达状态  4 已送达 运单完成 5 运单取消：顾客申请取消订单
// const orderStatus = {
//   // 1:"待付款",// 等待支付
//   // 2:"待接单",// 等待商户接单
//   // 3:"已接单",// 商家已接单
//   // 4:"派送中",// 订单派送中
//   // 5:"已完成",// 订单已完成
//   // 6:"已取消",// 订单已取消
//   // 7:"退款"//
//   1: "待付款",
//   2: "等待商家接单",
//   3: "商家已接单",
//   4: "派送中",
//   5: "已完成",
//   6: "已取消",
//   7: "退款",
// };
// const waybillStatus = {
//   3: "派送中",
// };
// 状态
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
// 倒计时
export const runTimeBack = () => {
	//  var nowTime = new Date();
	//  var createdTime = new Date(val);
	//  var TIME = 60 * Number(timeInterval)*1000;
	//  // 目标时间和当前时间的毫秒数
	//  var differ = nowTime - createdTime;
	//  // 目标时间超过当前时间，或目标时间与当前时间的差超过30分钟则返回0
	//  if (differ < 0 || differ > TIME) {
	// return 0;
	//  };
	//  // 剩余时间的秒数
	//  differ = TIME - differ;
	//  // 计算分钟数
	//  var minute = Math.floor(differ / (60 * 1000));
	//  console.log(minute,123)
	//  minute = minute < 10 ? '0' + minute : minute;
	//  // 计算秒数
	//  var second = Math.floor((differ - minute * (60 * 1000)) / 1000);
	//  second = second < 10 ? '0' + second : second;
	//  // 返回结果格式 29:59

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
// 获取周几
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
	//年 getFullYear()：四位数字返回年份
	var year = date.getFullYear(); //getFullYear()代替getYear()
	//月 getMonth()：0 ~ 11
	var month = date.getMonth() + 1;
	//日 getDate()：(1 ~ 31)
	var day = date.getDate();
	//时 getHours()：(0 ~ 23)
	var hour = date.getHours();
	//分 getMinutes()： (0 ~ 59)
	var minute = date.getMinutes();
	//秒 getSeconds()：(0 ~ 59)
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
	// 年
	let y = date.getFullYear().toString()
	// 月
	let m = (date.getMonth() + 1).toString()
	// 日
	let d = date.getDate().toString()
	if (m < 10) {
		m = '0' + m
	}
	if (d < 10) {
		d = '0' + d
	}
	// 有其他格式化字符需求可以继续添加，必须转化成字符串

	ret = y + '-' + m + '-' + d + ' ' + time + ':00'
	console.log(ret, 44)
	return ret;
}
import { translate } from "@/locales";
