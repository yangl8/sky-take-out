import i18n from './index'

const messageKeys: Record<string, string> = {
  '密码错误': 'apiErrors.passwordIncorrect',
  '账号不存在': 'apiErrors.accountNotFound',
  '账号被锁定': 'apiErrors.accountLocked',
  '未知错误': 'apiErrors.unknown',
  '文件上传失败': 'apiErrors.uploadFailed',
  '密码修改失败': 'apiErrors.passwordChangeFailed',
  '当前分类关联了套餐,不能删除': 'apiErrors.categoryHasSetMeals',
  '当前分类关联了菜品,不能删除': 'apiErrors.categoryHasDishes',
  '起售中的菜品不能删除': 'apiErrors.dishOnSale',
  '当前菜品关联了套餐,不能删除': 'apiErrors.dishHasSetMeals',
  '起售中的套餐不能删除': 'apiErrors.setmealOnSale',
  '套餐内包含未启售菜品，无法启售': 'apiErrors.setmealHasUnavailableDishes',
  '订单状态错误': 'apiErrors.orderStatusError',
  '订单不存在': 'apiErrors.orderNotFound',
  '退款失败，请检查微信支付配置后重试': 'apiErrors.refundFailed',
  '订单商品数量超出可处理范围': 'apiErrors.orderQuantityTooLarge',
  '该订单已支付': 'apiErrors.orderAlreadyPaid',
  '配送方式无效，无法下单': 'apiErrors.invalidDeliveryType',
  '请选择有效的预约送达时间': 'apiErrors.deliveryTimeRequired',
  '预约送达时间不足最短准备时间': 'apiErrors.deliveryPreparationTimeNotMet',
  '预约送达时间不在允许营业时段内': 'apiErrors.deliveryTimeOutsideBusinessHours',
  '店铺当前未营业，无法提交立即配送订单': 'apiErrors.shopClosedForDelivery',
  '店铺营业状态暂不可用，请稍后重试': 'apiErrors.shopStatusUnavailable',
  '订单超过15分钟付款期限，无法继续支付': 'apiErrors.orderPaymentExpired',
  '订单当前状态不允许支付': 'apiErrors.orderPaymentNotAllowed',
  '预约送达时间不足30分钟准备时间，无法支付': 'apiErrors.scheduledOrderPreparationNotMet',
  '店铺当前未营业，无法发起立即配送支付': 'apiErrors.shopClosedForPayment',
  '购物车数据为空，不能下单': 'apiErrors.shoppingCartEmpty',
  '用户地址为空，不能下单': 'apiErrors.addressRequired',
  '配送费配置无效，无法下单': 'apiErrors.invalidDeliveryFee',
  '购物车商品数量必须大于零': 'apiErrors.cartQuantityInvalid',
  '购物车商品信息无效，请刷新后重试': 'apiErrors.cartInvalid',
  '菜品不存在或已停售，请刷新后重试': 'apiErrors.dishUnavailable',
  '套餐不存在或已停售，请刷新后重试': 'apiErrors.setmealUnavailable',
  '商品价格无效，无法下单': 'apiErrors.invalidProductPrice',
  '店铺地址解析失败': 'apiErrors.shopAddressUnavailable',
  '收货地址解析失败': 'apiErrors.deliveryAddressUnavailable',
  '配送路线规划失败': 'apiErrors.deliveryRouteUnavailable',
  '超出配送范围': 'apiErrors.outOfDeliveryRange',
  '没有查询到默认地址': 'apiErrors.defaultAddressNotFound',
  '地址不存在': 'apiErrors.addressNotFound',
  '设置默认地址失败': 'apiErrors.defaultAddressUpdateFailed'
}

/**
 * Maps known backend error messages to the selected admin locale; unknown messages remain unchanged
 * @param message Error message returned by the backend
 * @return Localized message for a known error
 */
export function localizeAdminMessage(message: unknown): string {
  if (typeof message !== 'string' || !message.trim()) return String(i18n.t('apiErrors.requestFailed'))

  const value = message.trim()
  const key = messageKeys[value]
  if (key) return String(i18n.t(key))

  const duplicateName = value.match(/^(.+?)已存在$/)
  if (duplicateName) {
    return String(i18n.t('apiErrors.alreadyExists', { value: duplicateName[1] }))
  }

  return value
}
