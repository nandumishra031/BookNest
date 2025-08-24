package com.booknest.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String, // Store hashed password for security
    val profileImageUrl: String,
    val rating: Float,
    val location: String,
    val phone: String = "",
    val bio: String = "",
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val coverImageUrl: String,
    val price: Double,
    val rentalPrice: Double,
    val condition: String, // BookCondition enum as string
    val category: String,
    val description: String,
    val sellerId: String,
    val sellerName: String,
    val sellerRating: Float,
    val sellerLocation: String,
    val rating: Float,
    val isAvailableForRent: Boolean,
    val isAvailableForPurchase: Boolean,
    val isNewBook: Boolean,
    val stockQuantity: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val userId: String,
    val quantity: Int,
    val isRental: Boolean,
    val rentalDays: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishlist_items")
data class WishlistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val userId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rentals")
data class RentalEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val userId: String,
    val startDate: Long,
    val endDate: Long,
    val totalDays: Int,
    val isActive: Boolean,
    val rentalPrice: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val totalAmount: Double,
    val orderStatus: String, // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    val paymentMethod: String,
    val deliveryAddress: String,
    val orderDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String,
    val bookId: String,
    val quantity: Int,
    val price: Double,
    val isRental: Boolean = false,
    val rentalDays: Int = 0,
    val rentalStartDate: Long? = null,
    val rentalEndDate: Long? = null
)

@Entity(tableName = "user_books")
data class UserBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val bookId: String,
    val purchaseDate: Long,
    val orderId: String,
    val quantity: Int = 1,
    val purchasePrice: Double,
    val accessExpiryDate: Long? = null, // For rentals, null for purchases
    val isRental: Boolean = false
)
