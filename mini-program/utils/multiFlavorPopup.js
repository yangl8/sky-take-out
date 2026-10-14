function prepareMultiFlavorPopup(item, isMultiSelectFlavor) {
	const moreNormdata = item.flavors.map((group) => ({
		...group,
		value: JSON.parse(group.value)
	}))
	const flavorDataes = []

	moreNormdata.forEach((group) => {
		if (!isMultiSelectFlavor(group) && group.value.length > 0) {
			flavorDataes.push(group.value[0])
		}
	})

	return { dish: item, moreNormdata, flavorDataes }
}

module.exports = prepareMultiFlavorPopup
