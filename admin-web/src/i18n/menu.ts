import i18n from './index'

// Display-only translations of existing demo data. Never modify API payloads.
const nameKeys: { [value: string]: string } = {
  "王老吉": "menuNames.wanglaoji",
  "北冰洋": "menuNames.beibingyang",
  "雪花啤酒": "menuNames.snowBeer",
  "米饭": "menuNames.rice",
  "馒头": "menuNames.bun",
  "老坛酸菜鱼": "menuNames.pickledFish",
  "经典酸菜鮰鱼": "menuNames.classicCatfish",
  "蜀味水煮草鱼": "menuNames.sichuanCarp",
  "清炒小油菜": "menuNames.greens",
  "蒜蓉娃娃菜": "menuNames.garlicCabbage",
  "清炒西兰花": "menuNames.broccoli",
  "炝炒圆白菜": "menuNames.cabbage",
  "清蒸鲈鱼": "menuNames.bass",
  "东坡肘子": "menuNames.porkKnuckle",
  "梅菜扣肉": "menuNames.porkGreens",
  "剁椒鱼头": "menuNames.fishHead",
  "金汤酸菜牛蛙": "menuNames.frogSoup",
  "香锅牛蛙": "menuNames.frogHotpot",
  "馋嘴牛蛙": "menuNames.frogSpecial",
  "草鱼2斤": "menuNames.grassCarp",
  "江团鱼2斤": "menuNames.jiangtuan",
  "鮰鱼2斤": "menuNames.catfish",
  "鸡蛋汤": "menuNames.eggSoup",
  "平菇豆腐汤": "menuNames.tofuSoup",
  "套餐A": "menuNames.comboA"
}
const descriptionKeys: { [value: string]: string } = {
  "还是小时候的味道": "menuDescriptions.childhood",
  "精选五常大米": "menuDescriptions.rice",
  "优质面粉": "menuDescriptions.flour",
  "原料：汤，草鱼，酸菜": "menuDescriptions.pickledFish",
  "原料：酸菜，江团，鮰鱼": "menuDescriptions.catfishGreens",
  "原料：草鱼，汤": "menuDescriptions.carpBroth",
  "原料：小油菜": "menuDescriptions.greens",
  "原料：蒜，娃娃菜": "menuDescriptions.garlicCabbage",
  "原料：西兰花": "menuDescriptions.broccoli",
  "原料：圆白菜": "menuDescriptions.cabbage",
  "原料：鲈鱼": "menuDescriptions.bass",
  "原料：猪肘棒": "menuDescriptions.porkKnuckle",
  "原料：猪肉，梅菜": "menuDescriptions.porkGreens",
  "原料：鲢鱼，剁椒": "menuDescriptions.fishHead",
  "原料：鲜活牛蛙，酸菜": "menuDescriptions.frogSoup",
  "配料：鲜活牛蛙，莲藕，青笋": "menuDescriptions.frogHotpot",
  "配料：鲜活牛蛙，丝瓜，黄豆芽": "menuDescriptions.frogSpecial",
  "原料：草鱼，黄豆芽，莲藕": "menuDescriptions.grassCarp",
  "配料：江团鱼，黄豆芽，莲藕": "menuDescriptions.jiangtuan",
  "原料：鮰鱼，黄豆芽，莲藕": "menuDescriptions.catfish",
  "配料：鸡蛋，紫菜": "menuDescriptions.eggSoup",
  "配料：豆腐，平菇": "menuDescriptions.tofuSoup"
}

function translate(value: string, keys: { [value: string]: string }): string {
  return Object.prototype.hasOwnProperty.call(keys, value) ? String(i18n.t(keys[value])) : value
}

export function menuName(value: string): string {
  return translate(value, nameKeys)
}

export function menuDescription(value: string): string {
  return translate(value, descriptionKeys)
}

// The backend formats an order summary as "name*quantity;".
export function orderMenuSummary(value: string): string {
  if (!value) return value
  return value.replace(/(^|;)([^;]+)(\*\d+)(?=;|$)/g,
    (_, separator, name, quantity) => separator + menuName(name) + quantity)
}
