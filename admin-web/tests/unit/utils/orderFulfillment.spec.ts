import { isScheduledDelivery, resolveTablewareDisplay } from '@/utils/orderFulfillment'

describe('order fulfillment display mapping', () => {
  it('marks only an explicit scheduled delivery status', () => {
    expect(isScheduledDelivery(0)).toBe(true)
    expect(isScheduledDelivery('0')).toBe(true)
    expect(isScheduledDelivery(1)).toBe(false)
    expect(isScheduledDelivery(null)).toBe(false)
    expect(isScheduledDelivery(undefined)).toBe(false)
  })

  it('distinguishes explicit no cutlery, selected sets, and meal count', () => {
    expect(resolveTablewareDisplay(0, 0)).toEqual({ type: 'none' })
    expect(resolveTablewareDisplay(0, 3)).toEqual({ type: 'sets', count: 3 })
    expect(resolveTablewareDisplay(1, 4)).toEqual({ type: 'byMeal', count: 4 })
  })

  it('keeps missing or invalid legacy fields unknown', () => {
    expect(resolveTablewareDisplay(null, 0)).toEqual({ type: 'unknown' })
    expect(resolveTablewareDisplay(0, null)).toEqual({ type: 'unknown' })
    expect(resolveTablewareDisplay(2, 0)).toEqual({ type: 'unknown' })
    expect(resolveTablewareDisplay(0, -1)).toEqual({ type: 'unknown' })
  })
})
