export type TablewareDisplay =
  | { type: 'unknown' }
  | { type: 'none' }
  | { type: 'sets'; count: number }
  | { type: 'byMeal'; count: number }

export function isScheduledDelivery(deliveryStatus: unknown): boolean {
  return deliveryStatus != null && Number(deliveryStatus) === 0
}

export function resolveTablewareDisplay(status: unknown, number: unknown): TablewareDisplay {
  if (status == null || number == null || number === '') return { type: 'unknown' }

  const count = Number(number)
  if (!Number.isFinite(count) || count < 0) return { type: 'unknown' }
  if (Number(status) === 1) return { type: 'byMeal', count }
  if (Number(status) === 0 && count === 0) return { type: 'none' }
  if (Number(status) === 0 && count > 0) return { type: 'sets', count }
  return { type: 'unknown' }
}
