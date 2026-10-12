const localizedAddresses = {
	'北京市市辖区朝阳区三里屯路19号': [
		'19 Sanlitun Road, Chaoyang District, Beijing',
		'19 rue Sanlitun, district de Chaoyang, Pékin'
	],
	'北京市朝阳区三里屯路19号': [
		'19 Sanlitun Road, Chaoyang District, Beijing',
		'19 rue Sanlitun, district de Chaoyang, Pékin'
	]
}

function normalizeAddress(value) {
	return value.replace(/[\/\s,，、]/g, '')
}

function getDemoDeliveryAddress(value, locale) {
	if (typeof value !== 'string' || (locale !== 'en' && locale !== 'fr')) return null
	const translation = localizedAddresses[normalizeAddress(value)]
	if (!translation) return null
	return translation[locale === 'fr' ? 1 : 0]
}

module.exports = { getDemoDeliveryAddress }
