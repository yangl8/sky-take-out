import i18n from './index'

const flavorKeys: { [name: string]: string } = {
  "甜味": "catalog.flavorSweet",
  "温度": "catalog.flavorTemperature",
  "忌口": "catalog.flavorExclusions",
  "辣度": "catalog.flavorSpice",
  "无糖": "catalog.noSugar",
  "少糖": "catalog.lessSugar",
  "半糖": "catalog.halfSugar",
  "多糖": "catalog.moreSugar",
  "全糖": "catalog.fullSugar",
  "热饮": "catalog.hot",
  "常温": "catalog.room",
  "去冰": "catalog.noIce",
  "少冰": "catalog.lessIce",
  "多冰": "catalog.moreIce",
  "不要葱": "catalog.noOnion",
  "不要蒜": "catalog.noGarlic",
  "不要香菜": "catalog.noCoriander",
  "不要辣": "catalog.noChili",
  "不辣": "catalog.notSpicy",
  "微辣": "catalog.mild",
  "中辣": "catalog.medium",
  "重辣": "catalog.verySpicy"
}

export function flavorLabel(value: string): string {
  return flavorKeys[value] ? String(i18n.t(flavorKeys[value])) : value
}
