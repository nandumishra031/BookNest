package com.booknest.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknest.app.data.Book
import com.booknest.app.data.CartItem
import com.booknest.app.data.Order
import com.booknest.app.data.User
import com.booknest.app.repository.BookNestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookNestViewModel(private val repository: BookNestRepository) : ViewModel() {

    // UI States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Dark mode state
    val isDarkMode = repository.userPreferences.isDarkMode

    // Books
    val allBooks = repository.getAllBooks()
    val trendingBooks = repository.getTrendingBooks()

    // Cart and Wishlist
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _wishlistItems = MutableStateFlow<List<Book>>(emptyList())
    val wishlistItems: StateFlow<List<Book>> = _wishlistItems.asStateFlow()

    // Orders
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _orderSuccess = MutableStateFlow<Order?>(null)
    val orderSuccess: StateFlow<Order?> = _orderSuccess.asStateFlow()

    // Rentals
    private val _activeRentals = MutableStateFlow<List<com.booknest.app.ui.rentals.RentalItem>>(emptyList())
    val activeRentals: StateFlow<List<com.booknest.app.ui.rentals.RentalItem>> = _activeRentals.asStateFlow()

    private val _pastRentals = MutableStateFlow<List<com.booknest.app.ui.rentals.RentalItem>>(emptyList())
    val pastRentals: StateFlow<List<com.booknest.app.ui.rentals.RentalItem>> = _pastRentals.asStateFlow()

    // User Books - NEW PROPERTIES FOR PURCHASED BOOKS
    private val _userPurchasedBooks = MutableStateFlow<List<Book>>(emptyList())
    val userPurchasedBooks: StateFlow<List<Book>> = _userPurchasedBooks.asStateFlow()

    private val _userRentedBooks = MutableStateFlow<List<Book>>(emptyList())
    val userRentedBooks: StateFlow<List<Book>> = _userRentedBooks.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Always ensure sample data is initialized
                repository.initializeSampleData()

                // Check login state after initialization
                _currentUser.value = repository.getCurrentUser()
                _currentUser.value?.let { user ->
                    loadUserData(user.id)
                }

                _isInitialized.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            // Load cart items
            repository.getCartItems(userId).collect { items ->
                _cartItems.value = items
            }
        }

        viewModelScope.launch {
            // Load wishlist items
            repository.getWishlistItems(userId).collect { items ->
                _wishlistItems.value = items
            }
        }

        viewModelScope.launch {
            // Load orders
            repository.getUserOrders(userId).collect { orders ->
                _orders.value = orders
            }
        }

        viewModelScope.launch {
            // Load active rentals
            repository.getActiveRentals(userId).collect { rentals ->
                _activeRentals.value = rentals
            }
        }

        viewModelScope.launch {
            // Load past rentals
            repository.getPastRentals(userId).collect { rentals ->
                _pastRentals.value = rentals
            }
        }

        viewModelScope.launch {
            // Load purchased books
            repository.getUserPurchasedBooks(userId).collect { books ->
                _userPurchasedBooks.value = books
            }
        }

        viewModelScope.launch {
            // Load rented books
            repository.getUserRentedBooks(userId).collect { books ->
                _userRentedBooks.value = books
            }
        }
    }

    // Authentication
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.loginUser(email, password)
            _isLoading.value = false

            result.onSuccess { user ->
                _currentUser.value = user
                loadUserData(user.id)
                onResult(true, null)
            }.onFailure { exception ->
                onResult(false, exception.message)
            }
        }
    }

    fun register(name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.registerUser(name, email, password)
            _isLoading.value = false

            result.onSuccess { user ->
                _currentUser.value = user
                loadUserData(user.id)
                onResult(true, null)
            }.onFailure { exception ->
                onResult(false, exception.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentUser.value = null
            _cartItems.value = emptyList()
            _wishlistItems.value = emptyList()
            _orders.value = emptyList()
            _activeRentals.value = emptyList()
            _pastRentals.value = emptyList()
            _userPurchasedBooks.value = emptyList()
            _userRentedBooks.value = emptyList()
        }
    }

    // Profile Management
    fun updateProfile(updatedUser: User, imageUri: Uri? = null, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (imageUri != null) {
                repository.updateUserProfileWithImage(updatedUser, imageUri)
            } else {
                repository.updateUserProfile(updatedUser)
            }
            _isLoading.value = false

            result.onSuccess { user ->
                _currentUser.value = user
                onResult(true, null)
            }.onFailure { exception ->
                onResult(false, exception.message)
            }
        }
    }

    // Password Management
    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val userId = _currentUser.value?.id
        if (userId == null) {
            onResult(false, "User not logged in")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.changePassword(userId, currentPassword, newPassword)
            _isLoading.value = false

            result.onSuccess {
                onResult(true, "Password changed successfully")
            }.onFailure { exception ->
                onResult(false, exception.message)
            }
        }
    }

    // Cart Management
    fun addToCart(book: Book, quantity: Int = 1, isRental: Boolean = false, rentalDays: Int = 0) {
        val userId = _currentUser.value?.id
        if (userId == null) {
            // User not logged in - this is likely the issue
            return
        }
        viewModelScope.launch {
            repository.addToCart(userId, book, quantity, isRental, rentalDays)
        }
    }

    fun updateCartItemQuantity(bookId: String, newQuantity: Int) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.updateCartItemQuantity(userId, bookId, newQuantity)
        }
    }

    fun removeFromCart(bookId: String) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.removeFromCart(userId, bookId)
        }
    }

    fun clearCart() {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.clearCart(userId)
        }
    }

    // Wishlist Management
    fun addToWishlist(book: Book) {
        val userId = _currentUser.value?.id
        if (userId == null) {
            // User not logged in - this is likely the issue
            return
        }
        viewModelScope.launch {
            repository.addToWishlist(userId, book)
        }
    }

    fun removeFromWishlist(bookId: String) {
        val userId = _currentUser.value?.id
        if (userId == null) {
            // User not logged in - this is likely the issue
            return
        }
        viewModelScope.launch {
            repository.removeFromWishlist(userId, bookId)
        }
    }

    suspend fun isInWishlist(bookId: String): Boolean {
        val userId = _currentUser.value?.id ?: return false
        return repository.isInWishlist(userId, bookId)
    }

    // Book Management with Image Support
    fun addBookWithImage(book: Book, imageUri: Uri?, onResult: (Boolean, String?) -> Unit) {
        val currentUser = _currentUser.value
        if (currentUser == null) {
            onResult(false, "User not logged in")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // Set the current user as the seller
            val bookWithSeller = book.copy(seller = currentUser)
            val result = repository.addBookWithImage(bookWithSeller, imageUri)
            _isLoading.value = false

            result.onSuccess {
                onResult(true, "Book listed successfully!")
            }.onFailure { exception ->
                onResult(false, exception.message)
            }
        }
    }

    // Image utility methods
    fun getImageUri(imagePath: String): Uri? {
        return repository.getImageUri(imagePath)
    }

    // Order Management
    fun placeOrder(deliveryAddress: String = "", onResult: (Boolean, String?, Order?) -> Unit) {
        val userId = _currentUser.value?.id
        if (userId == null) {
            onResult(false, "User not logged in", null)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.createOrderFromCart(userId, deliveryAddress)
            _isLoading.value = false

            result.onSuccess { order ->
                _orderSuccess.value = order
                onResult(true, "Order placed successfully!", order)
            }.onFailure { exception ->
                onResult(false, exception.message, null)
            }
        }
    }

    fun buyBookDirectly(book: Book, quantity: Int = 1, deliveryAddress: String = "", onResult: (Boolean, String?, Order?) -> Unit) {
        val userId = _currentUser.value?.id
        if (userId == null) {
            onResult(false, "User not logged in", null)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // Add to cart temporarily and then create order
            val addToCartResult = repository.addToCart(userId, book, quantity, isRental = false)

            if (addToCartResult.isSuccess) {
                val orderResult = repository.createOrderFromCart(userId, deliveryAddress)

                orderResult.onSuccess { order ->
                    _orderSuccess.value = order
                    _isLoading.value = false
                    onResult(true, "Purchase successful!", order)
                }.onFailure { exception ->
                    _isLoading.value = false
                    onResult(false, exception.message, null)
                }
            } else {
                _isLoading.value = false
                onResult(false, "Failed to process purchase", null)
            }
        }
    }

    fun rentBookDirectly(book: Book, rentalDays: Int, quantity: Int = 1, deliveryAddress: String = "", onResult: (Boolean, String?, Order?) -> Unit) {
        val userId = _currentUser.value?.id
        if (userId == null) {
            onResult(false, "User not logged in", null)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // Add to cart temporarily as rental and then create order
            val addToCartResult = repository.addToCart(userId, book, quantity, isRental = true, rentalDays = rentalDays)

            if (addToCartResult.isSuccess) {
                val orderResult = repository.createOrderFromCart(userId, deliveryAddress)

                orderResult.onSuccess { order ->
                    _orderSuccess.value = order
                    _isLoading.value = false
                    onResult(true, "Rental booking successful!", order)
                }.onFailure { exception ->
                    _isLoading.value = false
                    onResult(false, exception.message, null)
                }
            } else {
                _isLoading.value = false
                onResult(false, "Failed to process rental", null)
            }
        }
    }

    fun getOrderById(orderId: String, onResult: (Order?) -> Unit) {
        viewModelScope.launch {
            val order = repository.getOrderById(orderId)
            onResult(order)
        }
    }

    fun clearOrderSuccess() {
        _orderSuccess.value = null
    }

    // Calculate total for cart items
    fun calculateCartTotal(): Double {
        return _cartItems.value.sumOf { cartItem ->
            if (cartItem.isRental) {
                cartItem.book.rentalPrice * cartItem.quantity * cartItem.rentalDays
            } else {
                cartItem.book.price * cartItem.quantity
            }
        }
    }

    // Theme Management
    fun toggleDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            repository.userPreferences.setDarkMode(isDarkMode)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class BookNestViewModelFactory(private val repository: BookNestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookNestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookNestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
