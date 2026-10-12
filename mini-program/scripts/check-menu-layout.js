const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const displayText = fs.readFileSync(path.join(__dirname, '../utils/displayText.js'), 'utf8')
const template = fs.readFileSync(path.join(__dirname, '../pages/index/index.vue'), 'utf8')
const styles = fs.readFileSync(path.join(__dirname, '../pages/index/style.scss'), 'utf8')
const categoryBlock = displayText.match(/const categories = \{([\s\S]*?)\n\};/)
assert.ok(categoryBlock, 'Category translations must remain in the existing dictionary')

const categories = [...categoryBlock[1].matchAll(/"([^"]+)": \["([^"]+)", "([^"]+)"\]/g)]
assert.equal(categories.length, 10, 'All existing category translations must remain available')
const frenchCategories = Object.fromEntries(categories.map(([, source, english, french]) => [source, { english, french }]))
assert.deepEqual(Object.fromEntries(Object.entries(frenchCategories).map(([source, value]) => [source, value.french])), {
	'蜀味牛蛙': 'Grenouilles sichuanaises',
	'蜀味烤鱼': 'Poisson grillé sichuanais',
	'特色蒸菜': 'Plats vapeur',
	'新鲜时蔬': 'Légumes',
	'水煮鱼': 'Poisson poché sichuanais',
	'传统主食': 'Féculents',
	'酒水饮料': 'Boissons',
	'汤类': 'Soupes',
	'人气套餐': 'Menus populaires',
	'商务套餐': 'Menus d’affaires'
})

const categoryStyles = styles.match(/\.type_list\s*\{([\s\S]*?)\.active\s*\{/)
assert.ok(categoryStyles, 'Category layout styles must be present')
assert.match(categoryStyles[1], /flex: 0 0 176rpx;/)
assert.match(categoryStyles[1], /height: 100rpx;/)
assert.match(categoryStyles[1], /max-height: 72rpx;/)
assert.match(categoryStyles[1], /word-break: normal;/)
assert.doesNotMatch(categoryStyles[1], /-webkit-line-clamp|text-overflow/)

const menuStyles = styles.match(/\.dish_info\s*\{([\s\S]*?)\n\s*\}\n\s*\.no_dish/)
assert.ok(menuStyles, 'Menu card layout styles must be present')
const dishRows = styles.match(/\.vegetable_order_list\s*\{([\s\S]*?)\.no_dish\s*\{/)
assert.ok(dishRows, 'Dish list row styles must be present')
const dishRowStyles = dishRows[1].match(/\.type_item\s*\{([\s\S]*?)\.dish_img\s*\{/)
assert.ok(dishRowStyles, 'Dish rows must define shared horizontal spacing')
assert.match(dishRowStyles[1], /padding-right:\s*24rpx;/, 'All card content must share one right safety inset')
assert.match(dishRowStyles[1], /box-sizing:\s*border-box;/)
assert.doesNotMatch(menuStyles[1], /\.dish_action_area\s*\{[^}]*padding-right:/, 'Controls must use the shared row inset')
const titleStyles = menuStyles[1].match(/\.dish_name\s*\{([\s\S]*?)\n\s*\}/)
assert.ok(titleStyles, 'Dish title styles must be present')
assert.match(titleStyles[1], /font-size:\s*30rpx;/)
assert.match(titleStyles[1], /line-height:\s*38rpx;/)
assert.match(titleStyles[1], /-webkit-line-clamp: 2;/)
assert.match(titleStyles[1], /word-break: normal;/)
assert.match(titleStyles[1], /overflow-wrap: anywhere;/)
assert.doesNotMatch(titleStyles[1], /(?:^|\n)\s*(?:min-)?height\s*:/, 'Titles must use adaptive height')

const descriptionStyles = menuStyles[1].match(/\.dish_label,\s*\.dish_num\s*\{([\s\S]*?)\n\s*\}/)
assert.ok(descriptionStyles, 'Dish description styles must be present')
assert.match(descriptionStyles[1], /-webkit-line-clamp: 2;/)
assert.match(descriptionStyles[1], /line-height:\s*34rpx;/)
assert.match(descriptionStyles[1], /max-height: 68rpx;/)
assert.match(descriptionStyles[1], /word-break: normal;/)
assert.match(descriptionStyles[1], /overflow-wrap: anywhere;/)
assert.doesNotMatch(descriptionStyles[1], /(?:^|\n)\s*(?:min-)?height\s*:/, 'Descriptions must not reserve fixed height')

assert.match(template, /class="dish_label dish_sales"/)
assert.match(menuStyles[1], /\.dish_sales\s*\{[\s\S]*?text-overflow: ellipsis;[\s\S]*?white-space: nowrap;/)
assert.match(menuStyles[1], /flex:\s*1;[\s\S]*?min-width:\s*0;/, 'Long text must be allowed to wrap within the available card width')
assert.match(menuStyles[1], /\.dish_price\s*\{\s*position: static;/)
assert.match(menuStyles[1], /\.dish_action_area\s*\{[\s\S]*?margin-left: auto;[\s\S]*?justify-content: flex-end;/)
assert.match(menuStyles[1], /\.dish_active_btn\s*\{[\s\S]*?align-items: center;[\s\S]*?\.dish_cart_quantity\s*\{[\s\S]*?text-align: center;/)
assert.match(menuStyles[1], /\.check_but\s*\{[\s\S]*?max-width: 180rpx;/)
assert.match(menuStyles[1], /\.dish_active\s*\{\s*position: static;/)
assert.match(menuStyles[1], /\.dish_active_btn\s*\{\s*position: static;/)
assert.doesNotMatch(menuStyles[1].match(/\.dish_active\s*\{([\s\S]*?)\.dish_add/)[1], /margin-left: auto;/)
assert.doesNotMatch(menuStyles[1].match(/\.dish_active_btn\s*\{([\s\S]*?)\.dish_cart_quantity/)[1], /margin-left: auto;/)
assert.match(template, /class="dish_action_area"\s*>\s*<view class="dish_active"[\s\S]*?<\/view>\s*<view class="dish_active_btn"/)
assert.match(menuStyles[1], /\.dish_active_btn\s*\{[\s\S]*?align-items: center;[\s\S]*?\.dish_cart_quantity\s*\{[\s\S]*?text-align: center;/)
assert.match(styles, /@media screen and \(max-width: 360px\)[\s\S]*?\.dish_price\s*\{\s*flex: 0 0 100%;/)

console.log('Menu category translations and adaptive multilingual layout checks passed.')
