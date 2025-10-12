package com.booknest.app.data

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverImageUrl: String,
    val price: Double,
    val rentalPrice: Double,
    val condition: BookCondition,
    val category: String,
    val description: String,
    val seller: User,
    val rating: Float,
    val isAvailableForRent: Boolean,
    val isAvailableForPurchase: Boolean,
    val isNewBook: Boolean
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUrl: String,
    val rating: Float,
    val location: String
)

enum class BookCondition {
    NEW, LIKE_NEW, GOOD, FAIR, POOR
}

data class CartItem(
    val book: Book,
    val quantity: Int,
    val isRental: Boolean,
    val rentalDays: Int = 0
)

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: String
)

enum class BottomNavItem {
    HOME, MY_BOOKS, SELL, RENTALS, PROFILE
}

data class Order(
    val id: String,
    val userId: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val orderDate: Long,
    val status: OrderStatus = OrderStatus.CONFIRMED,
    val paymentMethod: String = "To be integrated", // Placeholder for future payment gateway
    val deliveryAddress: String = ""
)

data class OrderItem(
    val book: Book,
    val quantity: Int,
    val isRental: Boolean,
    val rentalDays: Int = 0,
    val itemPrice: Double,
    val rentalStartDate: Long? = null,
    val rentalEndDate: Long? = null
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED
}
