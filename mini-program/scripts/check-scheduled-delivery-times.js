const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const root = path.resolve(__dirname, '..');
const helperSource = fs.readFileSync(path.join(root, 'utils/scheduledDeliveryTime.js'), 'utf8')
	.replace(/export function /g, 'function ');
const helperContext = {};
vm.runInNewContext(`${helperSource}\nthis.api = { getScheduledDeliverySlots, formatBeijingDateTime, getBeijingDateKey, getBeijingWeekday, formatDeliveryDateTime, normalizeOrderDateTime };`, helperContext);
const { getScheduledDeliverySlots, formatBeijingDateTime, getBeijingDateKey, getBeijingWeekday, formatDeliveryDateTime, normalizeOrderDateTime } = helperContext.api;

function beijingTime(hour, minute, day = 11) {
	return Date.UTC(2026, 9, day, hour - 8, minute);
}

const previousTimezone = process.env.TZ;
for (const timezone of ['Europe/Paris', 'America/Los_Angeles', 'Asia/Shanghai']) {
	process.env.TZ = timezone;
	const cases = [
		[9, 0, '10:30'],
		[10, 0, '10:30'],
		[10, 30, '11:00'],
		[17, 8, '18:00'],
		[21, 45, null],
		[22, 0, null],
		[23, 0, null],
	];
	for (const [hour, minute, expected] of cases) {
		const slots = getScheduledDeliverySlots(beijingTime(hour, minute));
		assert.strictEqual(slots[0] || null, expected, `${timezone}: Beijing ${hour}:${String(minute).padStart(2, '0')}`);
		assert(slots.every(slot => /^([1][0-9]|20|21|22):(00|30)$/.test(slot)));
		assert(!slots.includes('10:00') && !slots.includes('22:30'));
	}
	const afterClose = beijingTime(21, 45);
	assert.strictEqual(getScheduledDeliverySlots(afterClose).length, 0);
	assert.strictEqual(getScheduledDeliverySlots(afterClose, 1)[0], '10:30');
	assert.strictEqual(getScheduledDeliverySlots(beijingTime(23, 0), 1)[0], '10:30');
	assert.strictEqual(getScheduledDeliverySlots(beijingTime(21, 30))[0], '22:00');
	assert(getScheduledDeliverySlots(beijingTime(17, 30)).includes('18:00'));
	assert(!getScheduledDeliverySlots(beijingTime(17, 31)).includes('18:00'));
	assert.strictEqual(getBeijingDateKey(beijingTime(23, 0), 1), '2026-10-12');
	assert.strictEqual(getBeijingWeekday(beijingTime(23, 0), 1), 1);
	assert.strictEqual(formatBeijingDateTime(beijingTime(21, 45), '10:30', 1), '2026-10-12 10:30:00');
assert.strictEqual(formatDeliveryDateTime('2026-10-12T10:30:00'), '2026-10-12 10:30');
assert.strictEqual(normalizeOrderDateTime('2026-10-11 17:48', beijingTime(17, 8)), '2026-10-11 17:48:00');
assert.strictEqual(normalizeOrderDateTime('2026-10-11T17:48:12', beijingTime(17, 8)), '2026-10-11 17:48:12');
}
if (previousTimezone === undefined) delete process.env.TZ;
else process.env.TZ = previousTimezone;

for (const locale of ['zh', 'en', 'fr']) {
	const source = fs.readFileSync(path.join(root, `locales/${locale}.js`), 'utf8').replace('export default', 'module.exports =');
	const context = { module: { exports: {} } };
	vm.runInNewContext(source, context);
	assert(context.module.exports.order.scheduledTimeExpired, `${locale} missing expired-slot message`);
}

const checkout = fs.readFileSync(path.join(root, 'pages/order/index.js'), 'utf8');
assert(checkout.includes('getScheduledDeliverySlots'));
assert(checkout.includes('normalizeOrderDateTime'));
assert(checkout.includes('scheduleNeedsReselect'));
assert(checkout.includes('startScheduledTimeRefresh'));
assert(checkout.includes('clearInterval(this.scheduleTimer)'));
assert(checkout.includes("this.$t('order.scheduledTimeExpired')"));
assert.strictEqual((checkout.match(/validateScheduledTimeBeforeSubmit\(\)/g) || []).length, 3);
const successPage = fs.readFileSync(path.join(root, 'pages/success/index.vue'), 'utf8');
assert(successPage.includes('Number(result.data.payStatus) === 1'));
assert(successPage.includes('formatDeliveryDateTime(result.data.estimatedDeliveryTime)'));
const deliveryInfo = fs.readFileSync(path.join(root, 'pages/details/components/deliveryInfo.vue'), 'utf8');
assert(deliveryInfo.includes('formatDeliveryDateTime(order.estimatedDeliveryTime)'));
assert(!checkout.includes('popright'));

console.log('Scheduled delivery checks passed: Beijing boundaries, next-day rollover, non-Beijing device timezones, expiry guard, and zh/en/fr message keys.');
