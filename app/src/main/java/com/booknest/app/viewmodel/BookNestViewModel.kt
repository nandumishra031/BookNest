package com.booknest.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknest.app.data.Book
import com.booknest.app.data.CartItem
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

    // Books
    val allBooks = repository.getAllBooks()
    val trendingBooks = repository.getTrendingBooks()

    // Cart and Wishlist
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _wishlistItems = MutableStateFlow<List<Book>>(emptyList())
    val wishlistItems: StateFlow<List<Book>> = _wishlistItems.asStateFlow()

    init {
        checkLoginState()
        initializeApp()
    }

    private fun checkLoginState() {
        viewModelScope.launch {
            _currentUser.value = repository.getCurrentUser()
            _currentUser.value?.let { user ->
                loadUserData(user.id)
            }
        }
    }

    private fun initializeApp() {
        viewModelScope.launch {
            if (repository.isFirstTimeLaunch()) {
                repository.initializeSampleData()
                repository.setFirstTimeLaunchCompleted()
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
        }
    }

    // Profile Management
    fun updateProfile(updatedUser: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateUserProfile(updatedUser)
            _isLoading.value = false

            result.onSuccess { user ->
                _currentUser.value = user
                onResult(true, null)
            }.onFailure { exception ->
                onResult(false, exception.message)
            }
        }
    }

    // Cart Management
    fun addToCart(book: Book, quantity: Int = 1, isRental: Boolean = false, rentalDays: Int = 0) {
        val userId = _currentUser.value?.id ?: return
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
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.addToWishlist(userId, book)
        }
    }

    fun removeFromWishlist(bookId: String) {
        val userId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.removeFromWishlist(userId, bookId)
        }
    }

    suspend fun isInWishlist(bookId: String): Boolean {
        val userId = _currentUser.value?.id ?: return false
        return repository.isInWishlist(userId, bookId)
    }

    // Book Management
    fun addBook(book: Book, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.addBook(book)
            result.onSuccess {
                onResult(true, null)
            }.onFailure { exception ->
                onResult(false, exception.message)
            }
        }
    }

    suspend fun getBookById(bookId: String): Book? {
        return repository.getBookById(bookId)
    }

    fun getBooksByCategory(category: String) = repository.getBooksByCategory(category)

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
