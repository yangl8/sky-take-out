const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const aggregateMenuCartQuantities = require('../utils/menuCartQuantities.js')

const root = path.join(__dirname, '..')
const template = fs.readFileSync(path.join(root, 'pages/index/index.vue'), 'utf8')
const dishDetail = fs.readFileSync(path.join(root, 'pages/index/components/dishDetail.vue'), 'utf8')
const legacyDishCard = fs.readFileSync(path.join(root, 'pages/index/food-component.vue'), 'utf8')
const page = fs.readFileSync(path.join(root, 'pages/index/index.js'), 'utf8')
const menuStyles = fs.readFileSync(path.join(root, 'pages/index/style.scss'), 'utf8')

const menuItems = [
	{ id: 10, type: 1, name: 'Frog', flavors: [{ name: '' }, { name: 'Mild' }] },
	{ id: 20, type: 2, name: 'Combo', flavors: [] },
	{ id: 30, type: 1, name: 'Other dish', flavors: [] }
]
const initialCart = [
	{ id: 101, dishId: '10', number: 2, dishFlavor: 'Mild' },
	{ id: 102, dishId: 10, number: 3, dishFlavor: 'No onion' },
	{ id: 103, setmealId: '20', number: 4 },
	{ id: 104, dishId: 30, number: 1 },
	{ id: 105, dishId: 999, setmealId: 10, number: 99 },
	null
]

const initialResult = aggregateMenuCartQuantities(menuItems, initialCart)
assert.deepEqual(initialResult.map((item) => item.dishNumber), [5, 4, 1], 'Counts must sum every matching cart row by menu type and ID')
assert.deepEqual(initialResult[0].flavors.map((flavor) => flavor.name), ['Mild'], 'Empty flavor choices should be excluded without mutating cart variants')
assert.equal(menuItems[0].flavors.length, 2, 'Aggregation must not mutate the original menu item')
assert.equal(initialCart.filter(Boolean).length, 5, 'Distinct cart rows for flavor variants must remain separate')

const afterAdd = aggregateMenuCartQuantities(menuItems, [...initialCart, { id: 106, dishId: 10, number: 1, dishFlavor: 'Very spicy' }])
assert.equal(afterAdd[0].dishNumber, 6, 'A new flavor variant must increment the menu total')
const afterUpdate = aggregateMenuCartQuantities(menuItems, initialCart.map((item) => item && item.id === 101 ? { ...item, number: 5 } : item))
assert.equal(afterUpdate[0].dishNumber, 8, 'Updating a cart row quantity must update the menu total')
const afterDelete = aggregateMenuCartQuantities(menuItems, initialCart.filter((item) => item && item.id !== 102))
assert.equal(afterDelete[0].dishNumber, 2, 'Deleting one flavor variant must retain the other variant count')
assert.deepEqual(aggregateMenuCartQuantities(menuItems, [])[0].dishNumber, 0, 'Clearing the cart must reset menu totals')

for (const [locale, expected] of Object.entries({
	zh: { short: '选择规格', quantity: '购物车内 {count} 份', existing: '选择规格' },
	en: { short: 'Select', quantity: 'In cart: {count}', existing: 'Choose options' },
	fr: { short: 'Choisir', quantity: 'Panier : {count}', existing: 'Choisir les options' }
})) {
	const source = fs.readFileSync(path.join(root, `locales/${locale}.js`), 'utf8')
	const match = source.match(/home:\s*\{([^\n]+)\}/)
	assert.ok(match, `${locale} home translations must exist`)
	assert.ok(match[1].includes(`selectOptionsShort: "${expected.short}"`), `${locale} short multi-option button text must match`)
	assert.ok(match[1].includes(`cartQuantity: "${expected.quantity}"`), `${locale} cart quantity text must match`)
	assert.ok(match[1].includes(`selectOptions: "${expected.existing}"`), `${locale} existing full prompt translation must remain unchanged`)
}

const multiSpecBlock = template.match(/<view class="dish_active_btn" v-else>([\s\S]*?)<\/view>/)
assert.ok(multiSpecBlock, 'Multi-spec menu actions must be present')
assert.match(multiSpecBlock[1], /class="dish_cart_quantity"[\s\S]*?\$t\("home\.cartQuantity"/)
assert.doesNotMatch(multiSpecBlock[1], /dish_cart_quantity"\s+v-if=/, 'The cart count must remain visible at zero')
assert.match(multiSpecBlock[1], /class="check_but" @click="moreNormDataesHandle\(item\)"[^>]*>\{\{ \$t\("home\.selectOptionsShort"\) \}\}/)
assert.doesNotMatch(multiSpecBlock[1], /dish_(?:add|red)|[−-]\s*数量|数量\s*\+/i, 'Menu multi-spec action must not expose per-variant quantity controls')
assert.match(dishDetail, /\$t\("home\.selectOptionsShort"\)/, 'The dish detail multi-spec button must use the short translation')
assert.doesNotMatch(dishDetail, /\$t\("home\.selectOptions"\)/, 'The dish detail selection button must not use the longer prompt')
assert.match(legacyDishCard, /\$t\('home\.selectOptionsShort'\)/, 'The remaining dish card selection label must use the short translation')
assert.doesNotMatch(legacyDishCard, /\$t\('home\.selectOptions'\)/, 'Dish card selection labels must not use the longer prompt')
assert.match(menuStyles, /\.dish_active_btn\s*\{[\s\S]*?align-items: center;[\s\S]*?\.dish_cart_quantity\s*\{[\s\S]*?text-align: center;/, 'Cart quantity must be centered over its button')
assert.match(page, /import aggregateMenuCartQuantities from "\.\.\/\.\.\/utils\/menuCartQuantities\.js"/)
assert.match(page, /this\.initdishListMut\(res\.data\)[\s\S]*?this\.setOrderNum\(\)/, 'Refreshing cart data must refresh aggregate menu quantities')
assert.match(page, /setOrderNum\(\)\s*\{\s*const menuItems = aggregateMenuCartQuantities\(this\.dishListData \|\| \[\], this\.orderListDataes\)/)
assert.match(page, /if \(typeof index === "number"\)\s*\{\s*this\.typeIndex = index\s*\}/, 'Refreshing the current category without an index must preserve the selected tab')
assert.match(page, /onShow\(\)\s*\{[\s\S]*?this\.init\(\)/, 'Returning to the menu must reload cart data')
assert.match(page, /async init\(\)\s*\{[\s\S]*?this\.getTableOrderDishListes\(\)[\s\S]*?async swichMenu/)
assert.match(page, /async addDishAction\(item, form\)\s*\{[\s\S]*?this\.getTableOrderDishListes\(\)[\s\S]*?addShop\(item\)/)
assert.match(page, /async redDishAction\(item, form\)\s*\{[\s\S]*?this\.getTableOrderDishListes\(\)[\s\S]*?clearCardOrder\(\)/)
assert.match(page, /clearCardOrder\(\)\s*\{[\s\S]*?this\.getTableOrderDishListes\(\)[\s\S]*?openDetailHandle\(item\)/)
assert.doesNotMatch(template, /\$t\("home\.selectOptions"\)/, 'Menu cards must use the short key without changing other prompt screens')

console.log('Multi-spec menu cart aggregation, synchronization, and short translations passed.')
