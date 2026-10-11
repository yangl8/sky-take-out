function getImmediateDeliveryStatusNotice(statusResult, serverRejected = false) {
	if (!statusResult || !statusResult.refreshed) return 'order.shopStatusUnavailable'
	if (statusResult.status !== 0 && statusResult.status !== 1) return 'order.shopStatusUnavailable'
	if (statusResult.status === 0) return 'order.immediateDeliveryClosed'
	if (statusResult.status === 1 && serverRejected) return 'order.immediateDeliveryUnavailable'
	return ''
}

module.exports = getImmediateDeliveryStatusNotice
