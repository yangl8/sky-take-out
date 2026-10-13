package com.sky.service;

import java.util.List;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

public interface ShoppingCartService {
    
    /**
     * Adds an item to the current user's cart or increments its existing quantity
     *
     * @param shoppingCartDTO Dish or set-meal details to add
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * Decrements an item in the current user's cart and removes it when its quantity reaches zero
     *
     * @param shoppingCartDTO Cart item to decrement
     */
    void subtractShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * Lists cart items belonging to the current user
     *
     * @return Current user's cart items
     */
    List<ShoppingCart> showShoppingCart();

    /**
     * Removes all cart items belonging to the current user
     */
    void cleanShoppingCart();
}
