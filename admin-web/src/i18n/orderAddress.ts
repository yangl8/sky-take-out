type RegionTranslation = [string, string, string]

interface AddressPrefix {
  source: string
  english: string
  french: string
}

const areaData = require('area-data/pcaa') as Record<string, Record<string, string>>
const regionTranslations = require('../../../mini-program/utils/regionTranslations.json') as Record<string, RegionTranslation>
const { getDemoDeliveryAddress } = require('../../../mini-program/utils/demoDeliveryAddress.js') as {
  getDemoDeliveryAddress: (address: string, locale: string) => string | null
}

function translationFor(code: string, sourceName: string, localeIndex: 1 | 2): string | null {
  const entry = regionTranslations[code]
  if (!entry || entry[0] !== sourceName || !entry[localeIndex]) return null
  return entry[localeIndex]
}

function buildAddressPrefixes(): AddressPrefix[] {
  const prefixes: AddressPrefix[] = []
  const provinces = areaData['86'] || {}

  Object.keys(provinces).forEach((provinceCode) => {
    const provinceName = provinces[provinceCode]
    const englishProvince = translationFor(provinceCode, provinceName, 1)
    const frenchProvince = translationFor(provinceCode, provinceName, 2)
    if (!englishProvince || !frenchProvince) return

    Object.keys(areaData[provinceCode] || {}).forEach((cityCode) => {
      const cityName = areaData[provinceCode][cityCode]
      const englishCity = cityName === '市辖区' ? '' : translationFor(cityCode, cityName, 1)
      const frenchCity = cityName === '市辖区' ? '' : translationFor(cityCode, cityName, 2)
      if (cityName !== '市辖区' && (!englishCity || !frenchCity)) return

      Object.keys(areaData[cityCode] || {}).forEach((districtCode) => {
        const districtName = areaData[cityCode][districtCode]
        const englishDistrict = translationFor(districtCode, districtName, 1)
        const frenchDistrict = translationFor(districtCode, districtName, 2)
        if (!englishDistrict || !frenchDistrict) return

        prefixes.push({
          source: provinceName + cityName + districtName,
          english: [englishProvince, englishCity, englishDistrict].filter(Boolean).join(', '),
          french: [frenchProvince, frenchCity, frenchDistrict].filter(Boolean).join(', ')
        })
      })
    })
  })

  return prefixes
}

const addressPrefixes = buildAddressPrefixes()

/**
 * 仅翻译能通过行政区划代码表唯一匹配的订单地址省市区，并原样保留详细地址
 * @param address 订单中保存的原始拼接地址
 * @param locale 当前管理端语言
 * @return 转换行政区划后的展示文本；不能可靠识别时返回原文
 */
export function orderAddressLabel(address: string, locale: string): string {
  if (typeof address !== 'string' || !address || locale === 'zh') return address
  const localeField = locale === 'fr' ? 'french' : locale === 'en' ? 'english' : null
  if (!localeField) return address

  const demoAddress = getDemoDeliveryAddress(address, locale)
  if (demoAddress) return demoAddress

  const matches = addressPrefixes.filter((prefix) => address.startsWith(prefix.source))
  if (!matches.length) return address

  const longestPrefix = Math.max(...matches.map((prefix) => prefix.source.length))
  const bestMatches = matches.filter((prefix) => prefix.source.length === longestPrefix)
  const translations = Array.from(new Set(bestMatches.map((prefix) => prefix[localeField])))
  if (translations.length !== 1) return address

  const detail = address.slice(longestPrefix)
  return detail ? `${translations[0]}, ${detail}` : translations[0]
}
