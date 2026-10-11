const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const getCheckoutAddressState = require('../utils/checkoutAddress.js')

const addressTemplate = fs.readFileSync(path.join(__dirname, '../pages/order/components/address.vue'), 'utf8')
const addressPage = fs.readFileSync(path.join(__dirname, '../pages/address/address.vue'), 'utf8')
const orderStyles = fs.readFileSync(path.join(__dirname, '../pages/order/style.scss'), 'utf8')
const orderScript = fs.readFileSync(path.join(__dirname, '../pages/order/index.js'), 'utf8')
const addressPageStyleStart = addressPage.indexOf('<style lang="scss" scoped>')
const addressViewStyles = addressPage.slice(addressPageStyleStart)
const checkoutAddressStyleStart = orderStyles.indexOf('        .address {')
const checkoutAddressStyleEnd = orderStyles.indexOf('        .name {', checkoutAddressStyleStart)
const checkoutAddressStyles = orderStyles.slice(checkoutAddressStyleStart, checkoutAddressStyleEnd)
const labelFor = (label, locale) => `${locale}:${label}`
const makeAddress = (label) => ({
	id: 'address-1',
	provinceName: '北京市',
	cityName: '北京市',
	districtName: '朝阳区',
	detail: 'Xinjie Avenue 123 Building LongAddressWithoutSpaces'.repeat(3),
	phone: '12345678901',
	consignee: 'Pierre',
	sex: 0,
	label
})

const defaultAddress = getCheckoutAddressState(makeAddress('1'), 'en', labelFor)
assert.equal(defaultAddress.tagLabel, '1')
assert.equal(defaultAddress.addressLabel, 'en:1')

const manuallySelectedAddress = getCheckoutAddressState(makeAddress('2'), 'fr', labelFor)
assert.equal(manuallySelectedAddress.tagLabel, '2')
assert.equal(manuallySelectedAddress.addressLabel, 'fr:2')

let checkout = getCheckoutAddressState(makeAddress('1'), 'en', labelFor)
checkout = getCheckoutAddressState(makeAddress(''), 'en', labelFor)
assert.equal(checkout.tagLabel, '')
assert.equal(checkout.addressLabel, '')
checkout = getCheckoutAddressState(makeAddress('3'), 'fr', labelFor)
assert.equal(checkout.tagLabel, '3')
assert.equal(checkout.addressLabel, 'fr:3')

const selectedBeforeEdit = makeAddress('1')
const editedAddress = { ...makeAddress(''), id: selectedBeforeEdit.id }
assert.equal(
	getCheckoutAddressState.refreshSelectedAddress(selectedBeforeEdit, [editedAddress]).label,
	''
)
assert.equal(
	getCheckoutAddressState.refreshSelectedAddress(selectedBeforeEdit, []).label,
	'1'
)

assert.match(addressTemplate, /<text v-if="tagLabel" class="tag"/)
assert.match(addressPage, /refreshSelectedAddress\(this\.addressData, this\.addressList\)/)
assert.match(addressPage, /await this\.getAddressList\(\)\.catch/)
assert.match(checkoutAddressStyles, /\.address\s*\{[\s\S]*?display:\s*flex;/)
assert.match(checkoutAddressStyles, /\.tag\s*\{[\s\S]*?position:\s*static;/)
assert.match(checkoutAddressStyles, /\.word\s*\{[\s\S]*?min-width:\s*0;/)
assert.match(checkoutAddressStyles, /\.word\s*\{[\s\S]*?word-break:\s*normal;/)
assert.match(checkoutAddressStyles, /\.word\s*\{[\s\S]*?overflow-wrap:\s*anywhere;/)
assert.match(addressViewStyles, /\.address_liests\s*\{[\s\S]*?min-height:\s*256rpx;/)
assert.match(addressViewStyles, /\.details\s*\{[\s\S]*?gap:\s*8rpx;/)
assert.match(addressViewStyles, /\.tag\s*\{[\s\S]*?max-width:\s*38%;/)
assert.match(addressViewStyles, /\.address_word\s*\{[\s\S]*?min-width:\s*0;/)
assert.match(addressViewStyles, /\.address_word\s*\{[\s\S]*?word-break:\s*normal;/)
assert.match(addressViewStyles, /\.address_word\s*\{[\s\S]*?overflow-wrap:\s*anywhere;/)
assert.doesNotMatch(addressViewStyles, /\.address_word\s*\{[^}]*white-space:\s*nowrap/)
assert.match(orderScript, /超出配送范围[\s\S]*?deliveryStatusNoticeKey = 'order\.outOfRange'/)
assert.match(orderScript, /message\.includes\('地址'\)[\s\S]*?deliveryStatusNoticeKey = 'order\.addressUnavailable'/)
assert.match(orderStyles, /\.delivery_status_notice[\s\S]*?word-break:\s*normal;/)
assert.match(orderStyles, /\.delivery_status_notice[\s\S]*?overflow-wrap:\s*anywhere;/)
for (const locale of ['zh', 'en', 'fr']) {
  const messages = fs.readFileSync(path.join(__dirname, `../locales/${locale}.js`), 'utf8')
  assert.match(messages, /outOfRange:/)
  assert.match(messages, /addressUnavailable:/)
}

console.log('Address label layout, localized address notices, and checkout address transitions passed.')
