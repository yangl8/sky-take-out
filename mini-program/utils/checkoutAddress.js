function getCheckoutAddressState(address, locale, getLabel) {
	if (!address) return {
		address: '',
		phoneNumber: '',
		nickName: '',
		gender: 0,
		addressBookId: '',
		tagLabel: '',
		addressLabel: ''
	}

	return {
		address: `${address.provinceName || ''}${address.cityName || ''}${address.districtName || ''}${address.detail || ''}`,
		phoneNumber: address.phone || '',
		nickName: address.consignee || '',
		gender: address.sex,
		addressBookId: address.id || '',
		tagLabel: address.label || '',
		addressLabel: address.label ? getLabel(address.label, locale) : ''
	}
}

getCheckoutAddressState.refreshSelectedAddress = function (selectedAddress, addressList) {
	if (!selectedAddress || !selectedAddress.id || !Array.isArray(addressList)) return selectedAddress
	return addressList.find((address) => String(address.id) === String(selectedAddress.id)) || selectedAddress
}

module.exports = getCheckoutAddressState
