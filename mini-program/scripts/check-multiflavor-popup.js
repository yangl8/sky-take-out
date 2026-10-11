const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const prepareMultiFlavorPopup = require('../utils/multiFlavorPopup.js')

const indexTemplate = fs.readFileSync(path.join(__dirname, '../pages/index/index.vue'), 'utf8')
const popupTemplate = fs.readFileSync(path.join(__dirname, '../pages/index/components/popMask.vue'), 'utf8')
const indexScript = fs.readFileSync(path.join(__dirname, '../pages/index/index.js'), 'utf8')
const emptySelection = { name: '不要葱', value: '[]' }
const singleSelection = { name: '辣度', value: '["微辣","中辣"]' }
const isMultiSelectFlavor = (group) => group.name === '不要葱'
const firstDish = {
	id: 1,
	name: 'Dish one',
	price: 18,
	flavors: [emptySelection, singleSelection]
}
const secondDish = {
	id: 2,
	name: 'Dish two',
	price: 26,
	flavors: [{ name: '温度', value: '["热","冷"]' }]
}

let open = false
let dish = null
let groups = []
let selected = []
assert.equal(open && dish, false)

let popupData = prepareMultiFlavorPopup(firstDish, isMultiSelectFlavor)
dish = popupData.dish
groups = popupData.moreNormdata
selected = popupData.flavorDataes
open = true
assert.equal(dish.name, 'Dish one')
assert.equal(dish.price, 18)
assert.deepEqual(groups[0].value, [])
assert.deepEqual(selected, ['微辣'])

open = false
assert.equal(open && dish, false)
popupData = prepareMultiFlavorPopup(firstDish, isMultiSelectFlavor)
dish = popupData.dish
groups = popupData.moreNormdata
selected = popupData.flavorDataes
open = true
assert.equal(dish.name, 'Dish one')
assert.deepEqual(selected, ['微辣'])

popupData = prepareMultiFlavorPopup(secondDish, isMultiSelectFlavor)
dish = popupData.dish
groups = popupData.moreNormdata
selected = popupData.flavorDataes
assert.equal(dish.name, 'Dish two')
assert.equal(dish.price, 26)
assert.deepEqual(selected, ['热'])

const noFlavorDish = prepareMultiFlavorPopup({ id: 3, name: 'Plain dish', price: 9, flavors: [] }, isMultiSelectFlavor)
assert.equal(noFlavorDish.dish.name, 'Plain dish')
assert.deepEqual(noFlavorDish.moreNormdata, [])
assert.throws(() => prepareMultiFlavorPopup(null, isMultiSelectFlavor), TypeError)

assert.match(indexTemplate, /v-if="openMoreNormPop && moreNormDishdata"/)
assert.match(popupTemplate, /v-if="moreNormDishdata" class="more_norm_pop"/)
assert.match(popupTemplate, /moreNormDishdata\.name/)
assert.match(popupTemplate, /moreNormDishdata\.price/)
assert.match(popupTemplate, /addShop\(moreNormDishdata, '普通'\)/)
const popupHandler = indexScript.slice(indexScript.indexOf('moreNormDataesHandle(item) {'), indexScript.indexOf('isMultiSelectFlavor(group) {'))
assert.ok(popupHandler.indexOf('this.moreNormDishdata = popupData.dish') < popupHandler.indexOf('this.openMoreNormPop = true'))
assert.ok(popupHandler.indexOf('this.moreNormdata = popupData.moreNormdata') < popupHandler.indexOf('this.openMoreNormPop = true'))
assert.ok(popupHandler.indexOf('this.flavorDataes = popupData.flavorDataes') < popupHandler.indexOf('this.openMoreNormPop = true'))
assert.match(indexScript, /closeMoreNorm\(moreNormDishdata\)\s*\{[\s\S]*?this\.openMoreNormPop = false/)
assert.match(indexScript, /closeMoreNorm\(moreNormDishdata\)\s*\{\s*this\.flavorDataes\.splice\(0, this\.flavorDataes\.length\)/)

console.log('Multi-flavor popup empty, open, close, reopen, and product-switch checks passed.')
