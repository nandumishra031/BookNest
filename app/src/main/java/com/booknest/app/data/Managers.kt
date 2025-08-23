package com.booknest.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

// Wishlist Manager
object WishlistManager {
    private val _wishlistItems = mutableStateListOf<Book>()
    val wishlistItems: List<Book> = _wishlistItems

    fun addToWishlist(book: Book): Boolean {
        return if (!_wishlistItems.contains(book)) {
            _wishlistItems.add(book)
            true
        } else {
            false
        }
    }

    fun removeFromWishlist(book: Book): Boolean {
        return _wishlistItems.remove(book)
    }

    fun isInWishlist(book: Book): Boolean {
        return _wishlistItems.contains(book)
    }

    fun clearWishlist() {
        _wishlistItems.clear()
    }
}

// Cart Manager with quantity validation
object CartManager {
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> = _cartItems

    fun addToCart(book: Book, quantity: Int = 1, isRental: Boolean = false, rentalDays: Int = 0): Boolean {
        // Check if book has enough stock (for demo, assume max 5 copies available)
        val maxStock = if (book.isNewBook) 10 else 3
        val currentQuantityInCart = _cartItems.find { it.book.id == book.id }?.quantity ?: 0

        if (currentQuantityInCart + quantity > maxStock) {
            return false // Not enough stock
        }

        val existingItem = _cartItems.find { it.book.id == book.id && it.isRental == isRental }
        if (existingItem != null) {
            val newQuantity = existingItem.quantity + quantity
            if (newQuantity <= maxStock) {
                val index = _cartItems.indexOf(existingItem)
                _cartItems[index] = existingItem.copy(quantity = newQuantity)
                return true
            } else {
                return false
            }
        } else {
            _cartItems.add(CartItem(book, quantity, isRental, rentalDays))
            return true
        }
    }

    fun updateQuantity(cartItem: CartItem, newQuantity: Int): Boolean {
        val maxStock = if (cartItem.book.isNewBook) 10 else 3
        if (newQuantity <= 0) {
            _cartItems.remove(cartItem)
            return true
        }
        if (newQuantity > maxStock) {
            return false
        }

        val index = _cartItems.indexOf(cartItem)
        if (index >= 0) {
            _cartItems[index] = cartItem.copy(quantity = newQuantity)
            return true
        }
        return false
    }

    fun removeFromCart(cartItem: CartItem): Boolean {
        return _cartItems.remove(cartItem)
    }

    fun clearCart() {
        _cartItems.clear()
    }

    fun getAvailableStock(book: Book): Int {
        val maxStock = if (book.isNewBook) 10 else 3
        val currentQuantityInCart = _cartItems.find { it.book.id == book.id }?.quantity ?: 0
        return maxStock - currentQuantityInCart
    }
}
