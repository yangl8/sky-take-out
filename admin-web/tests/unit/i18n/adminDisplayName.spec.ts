import { adminDisplayName } from '@/i18n/adminDisplayName'
import en from '@/i18n/locales/en'
import fr from '@/i18n/locales/fr'
import zh from '@/i18n/locales/zh'

describe('administrator display name', () => {
  it('localizes the built-in admin account label in each language', () => {
    expect(adminDisplayName('管理员', 'admin', (key) => zh.nav.administrator)).toBe('管理员')
    expect(adminDisplayName('管理员', 'admin', (key) => en.nav.administrator)).toBe('Administrator')
    expect(adminDisplayName('管理员', 'admin', (key) => fr.nav.administrator)).toBe('Administrateur')
  })

  it('preserves employee names, including an employee named 管理员', () => {
    expect(adminDisplayName('管理员', 'employee01', () => 'Administrateur')).toBe('管理员')
    expect(adminDisplayName('Zhangsan', 'admin', () => 'Administrateur')).toBe('Zhangsan')
  })
})
