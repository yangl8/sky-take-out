import { orderAddressLabel } from '@/i18n/orderAddress'
import { orderReasonLabel } from '@/i18n/orders'
import { localizeAdminMessage } from '@/i18n/adminMessages'
import fs from 'fs'
import path from 'path'
import i18n from '@/i18n'
import en from '@/i18n/locales/en'
import fr from '@/i18n/locales/fr'
import zh from '@/i18n/locales/zh'

describe('order address presentation localization', () => {
  it('keeps the stored Chinese address intact in Chinese', () => {
    const address = '北京市市辖区朝阳区新街大道1号'
    expect(orderAddressLabel(address, 'zh')).toBe(address)
  })

  it('translates only a uniquely matched administrative prefix', () => {
    const address = '北京市市辖区朝阳区新街大道1号'
    expect(orderAddressLabel(address, 'en')).toBe('Beijing Municipality, Chaoyang District, 新街大道1号')
    expect(orderAddressLabel(address, 'fr')).toBe('municipalité de Beijing, district de Chaoyang, 新街大道1号')
  })

  it('localizes only the exact demo delivery address and supported separators', () => {
    const address = '北京市市辖区朝阳区三里屯路19号'
    expect(orderAddressLabel(address, 'zh')).toBe(address)
    expect(orderAddressLabel(address, 'en')).toBe('19 Sanlitun Road, Chaoyang District, Beijing')
    expect(orderAddressLabel(address, 'fr')).toBe('19 rue Sanlitun, district de Chaoyang, Pékin')
    expect(orderAddressLabel('北京市 / 市辖区 / 朝阳区 / 三里屯路19号', 'en'))
      .toBe('19 Sanlitun Road, Chaoyang District, Beijing')
    expect(orderAddressLabel('北京市市辖区朝阳区三里屯路20号', 'en'))
      .toBe('Beijing Municipality, Chaoyang District, 三里屯路20号')
    expect(orderAddressLabel('北京市市辖区朝阳区新街大道1号', 'fr'))
      .toBe('municipalité de Beijing, district de Chaoyang, 新街大道1号')
  })

  it('uses the shared formatter in both admin order detail entry points', () => {
    const dashboardOrder = fs.readFileSync(path.resolve(__dirname, '../../../src/views/dashboard/components/orderList.vue'), 'utf8')
    const orderDetails = fs.readFileSync(path.resolve(__dirname, '../../../src/views/orderDetails/index.vue'), 'utf8')
    expect(dashboardOrder).toContain('orderAddressLabel(diaForm.address, $i18n.locale)')
    expect(orderDetails).toContain('orderAddressLabel(diaForm.address, $i18n.locale)')
  })

  it('returns unmatched address text unchanged', () => {
    const address = '未知省未知市未知区自填地址'
    expect(orderAddressLabel(address, 'en')).toBe(address)
    expect(orderAddressLabel(address, 'fr')).toBe(address)
  })
})

describe('system message localization', () => {
  const originalLocale = i18n.locale

  afterEach(() => {
    i18n.locale = originalLocale
  })

  it('translates only known system cancellation reasons and keeps free text', () => {
    i18n.locale = 'fr'
    expect(orderReasonLabel('用户取消')).toBe('Annulée par le client')
    expect(orderReasonLabel('订单超时，自动取消')).toBe('Annulée automatiquement après expiration')
    expect(orderReasonLabel('客户自填的理由')).toBe('客户自填的理由')
  })

  it('localizes known API errors and preserves unknown server text', () => {
    i18n.locale = 'en'
    expect(localizeAdminMessage('订单状态错误')).toBe('The order status is invalid.')
    expect(localizeAdminMessage('未识别的错误')).toBe('未识别的错误')
    expect(localizeAdminMessage('example-user已存在')).toBe('example-user already exists.')
  })

  it('provides matching API error translations in all three locales', () => {
    const keys = Object.keys(en.apiErrors).sort()
    expect(Object.keys(fr.apiErrors).sort()).toEqual(keys)
    expect(Object.keys(zh.apiErrors).sort()).toEqual(keys)
    expect(Object.keys(fr.orders).sort()).toEqual(Object.keys(en.orders).sort())
    expect(Object.keys(zh.orders).sort()).toEqual(Object.keys(en.orders).sort())
  })
})

describe('password dialog localization', () => {
  it('provides the confirmation label in Chinese, English, and French', () => {
    expect(zh.password.confirm).toBe('确认密码')
    expect(en.password.confirm).toBe('Confirm password')
    expect(fr.password.confirm).toBe('Confirmer le mot de passe')
  })
})
