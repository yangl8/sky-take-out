const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const getCheckoutAddressState = require('../utils/checkoutAddress.js')

const addressTemplate = fs.readFileSync(path.join(__dirname, '../pages/order/components/address.vue'), 'utf8')
const addressPage = fs.readFileSync(path.join(__dirname, '../pages/address/address.vue'), 'utf8')
const orderStyles = fs.readFileSync(path.join(__dirname, '../pages/order/style.scss'), 'utf8')
const addressStyleStart = orderStyles.indexOf('        .address {')
const addressStyleEnd = orderStyles.indexOf('        .name {', addressStyleStart)
const addressStyles = orderStyles.slice(addressStyleStart, addressStyleEnd)
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

assert.match(addressTemplate, /:class="\{ 'has-tag': !!tagLabel \}"/)
assert.match(addressPage, /refreshSelectedAddress\(this\.addressData, this\.addressList\)/)
assert.match(addressPage, /await this\.getAddressList\(\)\.catch/)
assert.match(orderStyles, /&\.has-tag\s*\{\s*padding-left:\s*76rpx;/)
assert.match(addressStyles, /word-break:\s*normal;/)
assert.match(addressStyles, /overflow-wrap:\s*anywhere;/)
assert.match(addressStyles, /word-wrap:\s*break-word;/)
assert.doesNotMatch(addressStyles, /-webkit-line-clamp|text-overflow:\s*ellipsis/)

console.log('Checkout address label transitions and wrapping rules passed.')
