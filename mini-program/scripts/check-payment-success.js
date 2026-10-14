const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

const root = path.join(__dirname, '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const helperSource = read('utils/paymentSuccessPresentation.js')
  .replace('export function getPaymentSuccessPresentation', 'function getPaymentSuccessPresentation')
const helperContext = { result: null }
vm.runInNewContext(`${helperSource}\nthis.result = getPaymentSuccessPresentation;`, helperContext)
const getPresentation = helperContext.result

assert.deepEqual({ ...getPresentation(1) }, {
  arrivalKey: 'success.immediateArrival',
  messageKey: 'success.immediatePreparing',
})
assert.deepEqual({ ...getPresentation('1') }, {
  arrivalKey: 'success.immediateArrival',
  messageKey: 'success.immediatePreparing',
})
assert.deepEqual({ ...getPresentation(0) }, {
  arrivalKey: 'success.scheduledArrival',
  messageKey: 'success.scheduledPreparing',
})
assert.deepEqual({ ...getPresentation('0') }, {
  arrivalKey: 'success.scheduledArrival',
  messageKey: 'success.scheduledPreparing',
})
for (const missingValue of [null, undefined, '', 2]) {
  assert.deepEqual({ ...getPresentation(missingValue) }, {
    arrivalKey: 'success.unknownArrival',
    messageKey: 'success.unknownPreparing',
  })
}

const expectedCopy = {
  zh: {
    immediateArrival: '预计送达：{time}',
    scheduledArrival: '预约送达：{time}',
    unknownArrival: '配送时间：{time}',
    immediatePreparing: '订单支付成功，商家将尽快安排备餐，请耐心等待。',
    scheduledPreparing: '预约成功！商家将根据预约时间安排备餐。',
    unknownPreparing: '支付已成功，配送信息正在确认。',
  },
  en: {
    immediateArrival: 'Estimated delivery: {time}',
    scheduledArrival: 'Scheduled delivery: {time}',
    unknownArrival: 'Delivery time: {time}',
    immediatePreparing: 'Payment successful. The restaurant will prepare your order shortly.',
    scheduledPreparing: 'Your order is scheduled. The restaurant will prepare it for your selected delivery time.',
    unknownPreparing: 'Payment successful. Delivery details are being confirmed.',
  },
  fr: {
    immediateArrival: 'Livraison estimée : {time}',
    scheduledArrival: 'Livraison programmée : {time}',
    unknownArrival: 'Horaire de livraison : {time}',
    immediatePreparing: 'Paiement effectué. Le restaurant préparera votre commande prochainement.',
    scheduledPreparing: 'Votre commande est programmée. Le restaurant la préparera pour l’heure de livraison prévue.',
    unknownPreparing: 'Paiement effectué. Les informations de livraison sont en cours de confirmation.',
  },
}

for (const [locale, copy] of Object.entries(expectedCopy)) {
  const source = read(`locales/${locale}.js`)
  for (const key of [
    'immediateArrival', 'scheduledArrival', 'unknownArrival',
    'immediatePreparing', 'scheduledPreparing', 'unknownPreparing',
  ]) {
    assert.ok(source.includes(`${key}: "${copy[key]}"`), `${locale}: wrong ${key}`)
  }
}

const page = read('pages/success/index.vue')
const paymentPage = read('pages/pay/index.vue')
const successStyles = page.slice(page.indexOf('<style lang="scss" scoped>'))
const descriptionStyle = successStyles.match(/\.success_desc\s*\{([^}]*)\}/)[1]
const arrivalStyle = successStyles.match(/\.word-box\s*\{([^}]*)\}/)[1]
assert.match(page, /getOrderDetail\(this\.orderId\)/)
assert.match(page, /this\.deliveryStatus = result\.data\.deliveryStatus/)
assert.match(page, /Number\(result\.data\.payStatus\) === 1/)
assert.match(page, /formatDeliveryDateTime\(result\.data\.estimatedDeliveryTime\)/)
assert.match(page, /presentation\.arrivalKey/)
assert.match(page, /presentation\.messageKey/)
assert.match(page, /url: "\/pages\/index\/index"/)
assert.match(page, /url: "\/pages\/details\/index\?orderId=" \+ this\.orderId/)
assert.equal((paymentPage.match(/\/pages\/success\/index\?orderId=/g) || []).length, 2)
assert.doesNotMatch(page, /success\.preparing"\)/)
assert.match(descriptionStyle, /line-height:\s*1\.4;/)
assert.match(descriptionStyle, /padding:\s*0 36rpx;/)
assert.doesNotMatch(descriptionStyle, /\n\s*height:/)
assert.match(arrivalStyle, /min-height:\s*44rpx;/)
assert.match(arrivalStyle, /line-height:\s*1\.4;/)
assert.match(arrivalStyle, /padding:\s*0 36rpx;/)
assert.match(successStyles, /\.btns\s*\{\s*margin-top:\s*32rpx;/)
assert.match(successStyles, /\.go_dish\s*\{[^}]*margin-top:\s*0;/)

console.log('Payment success delivery-type checks passed.')
