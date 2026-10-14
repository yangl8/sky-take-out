function getShopClosedNoticeState(previousStatus, nextStatus, visible, dismissed) {
	if (nextStatus === 1) return { visible: false, dismissed: false }
	if (nextStatus === 0 && previousStatus !== 0 && !dismissed) {
		return { visible: true, dismissed: false }
	}
	return { visible, dismissed }
}

module.exports = getShopClosedNoticeState
