const BEIJING_OFFSET_MS = 8 * 60 * 60 * 1000;
const PREPARATION_TIME_MS = 30 * 60 * 1000;

function beijingParts(timestamp) {
	const date = new Date(timestamp + BEIJING_OFFSET_MS);
	return {
		year: date.getUTCFullYear(),
		month: date.getUTCMonth(),
		day: date.getUTCDate(),
		weekday: date.getUTCDay(),
	};
}

function beijingDateTimeEpoch(parts, hour, minute) {
	return Date.UTC(parts.year, parts.month, parts.day, hour, minute) - BEIJING_OFFSET_MS;
}

function pad(value) {
	return String(value).padStart(2, '0');
}

export function getBeijingWeekday(timestamp, dayOffset = 0) {
	const parts = beijingParts(timestamp);
	const date = new Date(Date.UTC(parts.year, parts.month, parts.day + dayOffset));
	return date.getUTCDay();
}

export function getBeijingDateKey(timestamp, dayOffset = 0) {
	const parts = beijingParts(timestamp);
	const date = new Date(Date.UTC(parts.year, parts.month, parts.day + dayOffset));
	return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}`;
}

export function getScheduledDeliverySlots(timestamp, dayOffset = 0) {
	const parts = beijingParts(timestamp);
	const target = new Date(Date.UTC(parts.year, parts.month, parts.day + dayOffset));
	const targetParts = {
		year: target.getUTCFullYear(),
		month: target.getUTCMonth(),
		day: target.getUTCDate(),
	};
	const earliestAllowed = timestamp + PREPARATION_TIME_MS;
	const slots = [];

	for (let minutes = 10 * 60 + 30; minutes <= 22 * 60; minutes += 30) {
		const hour = Math.floor(minutes / 60);
		const minute = minutes % 60;
		if (beijingDateTimeEpoch(targetParts, hour, minute) >= earliestAllowed) {
			slots.push(`${pad(hour)}:${pad(minute)}`);
		}
	}
	return slots;
}

export function formatBeijingDateTime(timestamp, time, dayOffset = 0) {
	const match = /^(\d{2}):(\d{2})$/.exec(time || '');
	if (!match) return '';
	const parts = beijingParts(timestamp);
	const date = new Date(Date.UTC(parts.year, parts.month, parts.day + dayOffset));
	return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ${match[1]}:${match[2]}:00`;
}

export function formatBeijingTime(timestamp) {
	const shifted = new Date(timestamp + BEIJING_OFFSET_MS);
	return `${pad(shifted.getUTCHours())}:${pad(shifted.getUTCMinutes())}`;
}

export function normalizeOrderDateTime(value, fallbackTimestamp) {
	if (typeof value === 'string') {
		const normalized = value.replace('T', ' ').trim();
		if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(normalized)) return `${normalized}:00`;
		const withSeconds = /^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})/.exec(normalized);
		if (withSeconds) return withSeconds[1];
	}
	return formatBeijingDateTime(fallbackTimestamp, formatBeijingTime(fallbackTimestamp));
}

export function formatDeliveryDateTime(value) {
	if (typeof value !== 'string') return '';
	const normalized = value.replace('T', ' ');
	const match = /^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/.exec(normalized);
	return match ? `${match[1]} ${match[2]}` : '';
}
