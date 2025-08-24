package com.booknest.app.database.dao

import androidx.room.*
import com.booknest.app.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    @Query("UPDATE users SET isLoggedIn = 1 WHERE id = :userId")
    suspend fun loginUser(userId: String)

    @Query("UPDATE users SET profileImageUrl = :imagePath WHERE id = :userId")
    suspend fun updateUserProfileImage(userId: String, imagePath: String)
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE category = :category ORDER BY rating DESC")
    fun getBooksByCategory(category: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE :query OR author LIKE :query")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY rating DESC LIMIT :limit")
    fun getTrendingBooks(limit: Int): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("UPDATE books SET stockQuantity = stockQuantity - :quantity WHERE id = :bookId")
    suspend fun updateStock(bookId: String, quantity: Int)

    @Query("UPDATE books SET coverImageUrl = :imagePath WHERE id = :bookId")
    suspend fun updateBookCoverImage(bookId: String, imagePath: String)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartItems(userId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND bookId = :bookId AND isRental = :isRental")
    suspend fun getCartItem(userId: String, bookId: String, isRental: Boolean): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun getAllCartItemsForBook(userId: String, bookId: String): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cartItem: CartItemEntity)

    @Update
    suspend fun updateCartItem(cartItem: CartItemEntity)

    @Delete
    suspend fun deleteCartItem(cartItem: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeFromCart(userId: String, bookId: String)
}

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items WHERE userId = :userId ORDER BY addedAt DESC")
    fun getWishlistItems(userId: String): Flow<List<WishlistItemEntity>>

    @Query("SELECT * FROM wishlist_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun getWishlistItem(userId: String, bookId: String): WishlistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(wishlistItem: WishlistItemEntity)

    @Delete
    suspend fun deleteWishlistItem(wishlistItem: WishlistItemEntity)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeFromWishlist(userId: String, bookId: String)
}

@Dao
interface RentalDao {
    @Query("SELECT * FROM rentals WHERE userId = :userId AND isActive = 1")
    fun getActiveRentals(userId: String): Flow<List<RentalEntity>>

    @Query("SELECT * FROM rentals WHERE userId = :userId AND isActive = 0")
    fun getPastRentals(userId: String): Flow<List<RentalEntity>>

    @Query("SELECT * FROM rentals WHERE id = :rentalId")
    suspend fun getRentalById(rentalId: String): RentalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRental(rental: RentalEntity)

    @Update
    suspend fun updateRental(rental: RentalEntity)

    @Query("UPDATE rentals SET isActive = 0 WHERE id = :rentalId")
    suspend fun expireRental(rentalId: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    fun getUserOrders(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(orderItems: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>
}

@Dao
interface UserBooksDao {
    @Query("SELECT * FROM user_books WHERE userId = :userId AND isRental = 0 ORDER BY purchaseDate DESC")
    fun getUserPurchasedBooks(userId: String): Flow<List<UserBookEntity>>

    @Query("SELECT * FROM user_books WHERE userId = :userId AND isRental = 1 AND (accessExpiryDate IS NULL OR accessExpiryDate > :currentTime) ORDER BY purchaseDate DESC")
    fun getUserActiveRentedBooks(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<UserBookEntity>>

    @Query("SELECT * FROM user_books WHERE userId = :userId ORDER BY purchaseDate DESC")
    fun getAllUserBooks(userId: String): Flow<List<UserBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserBook(userBook: UserBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserBooks(userBooks: List<UserBookEntity>)

    @Query("SELECT * FROM user_books WHERE userId = :userId AND bookId = :bookId AND isRental = :isRental")
    suspend fun getUserBook(userId: String, bookId: String, isRental: Boolean): UserBookEntity?

    @Update
    suspend fun updateUserBook(userBook: UserBookEntity)

    @Delete
    suspend fun deleteUserBook(userBook: UserBookEntity)
}
