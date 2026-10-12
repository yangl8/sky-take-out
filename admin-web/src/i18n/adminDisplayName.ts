/**
 * 本地化系统管理员显示标签，同时保留普通员工的真实姓名
 * @param name 登录接口返回的员工显示名
 * @param userName 登录接口返回的员工账号
 * @param translate 当前语言下的翻译函数
 * @return 应在管理端导航栏显示的名称
 */
export function adminDisplayName(
  name: string | undefined,
  userName: string | undefined,
  translate: (key: string) => string
): string {
  if (name === '管理员' && userName && userName.toLowerCase() === 'admin') {
    return translate('nav.administrator')
  }
  return name || ''
}
