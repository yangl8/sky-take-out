import i18n from './index'

const reasonKeys: Record<string, string> = {
  '订单量较多，暂时无法接单': 'orders.reasonTooManyOrders',
  '菜品已销售完，暂时无法接单': 'orders.reasonSoldOut',
  '餐厅已打烊，暂时无法接单': 'orders.reasonClosed',
  '骑手不足无法配送': 'orders.reasonNoCourier',
  '客户电话取消': 'orders.reasonCustomerCancelled',
  '用户取消': 'orders.reasonUserCancelled',
  '订单超时，自动取消': 'orders.reasonTimeoutCancelled',
  '自定义原因': 'orders.customReason',
}

export function orderReasonLabel(value: string): string {
  const key = reasonKeys[value]
  return key ? String(i18n.t(key)) : value
}
