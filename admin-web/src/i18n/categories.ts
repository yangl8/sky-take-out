import i18n from './index'

// Display translations for known demo categories; custom names remain unchanged.
const categoryKeys: { [name: string]: string } = {
  "蜀味牛蛙": "categoryNames.sichuanFrog",
  "蜀味烤鱼": "categoryNames.sichuanFish",
  "特色蒸菜": "categoryNames.steamed",
  "新鲜时蔬": "categoryNames.vegetables",
  "水煮鱼": "categoryNames.poachedFish",
  "传统主食": "categoryNames.staples",
  "酒水饮料": "categoryNames.drinks",
  "汤类": "categoryNames.soups",
  "人气套餐": "categoryNames.popular",
  "商务套餐": "categoryNames.business"
}

export function categoryLabel(name: string): string {
  return categoryKeys[name] ? String(i18n.t(categoryKeys[name])) : name
}
