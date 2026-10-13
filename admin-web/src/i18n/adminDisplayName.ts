/**
 * Localizes the system administrator label while preserving employee names
 * @param name Employee display name returned by the login endpoint
 * @param userName Employee account returned by the login endpoint
 * @param translate Translation function for the active locale
 * @return Name to display in the admin navigation bar
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
