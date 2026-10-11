function aggregateMenuCartQuantities(menuItems, cartItems) {
	const cart = Array.isArray(cartItems) ? cartItems : []

	return menuItems.map((menuItem) => {
		const cartIdField = menuItem.type === 1
			? 'dishId'
			: menuItem.type === 2
				? 'setmealId'
				: null
		const dishNumber = cartIdField
			? cart.reduce((total, cartItem) => {
				if (!cartItem || cartItem[cartIdField] == null || String(cartItem[cartIdField]) !== String(menuItem.id)) return total
				const quantity = Number(cartItem.number)
				return total + (Number.isFinite(quantity) ? quantity : 0)
			}, 0)
			: 0
		const flavors = Array.isArray(menuItem.flavors)
			? menuItem.flavors.filter((flavor) => flavor.name !== '')
			: menuItem.flavors

		return { ...menuItem, flavors, dishNumber }
	})
}

module.exports = aggregateMenuCartQuantities
