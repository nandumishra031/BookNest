package com.booknest.app.repository

import com.booknest.app.database.BookNestDatabase
import com.booknest.app.database.entities.*
import com.booknest.app.data.*
import com.booknest.app.datastore.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.UUID

class BookNestRepository(
    private val database: BookNestDatabase,
    val userPreferences: UserPreferences
) {

    // User operations
    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val userEntity = database.userDao().getUserByEmail(email)
            if (userEntity != null) {
                // Update login state in database
                database.userDao().logoutAllUsers()
                database.userDao().loginUser(userEntity.id)

                // Save login state in preferences
                userPreferences.saveLoginState(
                    isLoggedIn = true,
                    userId = userEntity.id,
                    userName = userEntity.name,
                    userEmail = userEntity.email,
                    profileImage = userEntity.profileImageUrl
                )

                Result.success(userEntity.toUser())
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(name: String, email: String, password: String): Result<User> {
        return try {
            val existingUser = database.userDao().getUserByEmail(email)
            if (existingUser != null) {
                Result.failure(Exception("User already exists"))
            } else {
                val userId = UUID.randomUUID().toString()
                val userEntity = UserEntity(
                    id = userId,
                    name = name,
                    email = email,
                    profileImageUrl = "",
                    rating = 5.0f,
                    location = "Mumbai, India",
                    isLoggedIn = true
                )

                database.userDao().insertUser(userEntity)

                // Save login state in preferences
                userPreferences.saveLoginState(
                    isLoggedIn = true,
                    userId = userId,
                    userName = name,
                    userEmail = email
                )

                Result.success(userEntity.toUser())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(user: User): Result<User> {
        return try {
            val userEntity = user.toUserEntity()
            database.userDao().updateUser(userEntity)

            // Update preferences
            userPreferences.updateUserProfile(
                userName = user.name,
                userEmail = user.email,
                profileImage = user.profileImageUrl
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): User? {
        return database.userDao().getLoggedInUser()?.toUser()
    }

    suspend fun logout() {
        database.userDao().logoutAllUsers()
        userPreferences.logout()
    }

    // Book operations
    fun getAllBooks(): Flow<List<Book>> {
        return database.bookDao().getAllBooks().map { entities ->
            entities.map { it.toBook() }
        }
    }

    suspend fun getBookById(bookId: String): Book? {
        return database.bookDao().getBookById(bookId)?.toBook()
    }

    fun getTrendingBooks(): Flow<List<Book>> {
        return database.bookDao().getTrendingBooks(10).map { entities ->
            entities.map { it.toBook() }
        }
    }

    fun getBooksByCategory(category: String): Flow<List<Book>> {
        return database.bookDao().getBooksByCategory(category).map { entities ->
            entities.map { it.toBook() }
        }
    }

    suspend fun addBook(book: Book): Result<Unit> {
        return try {
            database.bookDao().insertBook(book.toBookEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cart operations
    fun getCartItems(userId: String): Flow<List<CartItem>> {
        return database.cartDao().getCartItems(userId).map { cartEntities ->
            cartEntities.mapNotNull { cartEntity ->
                val book = database.bookDao().getBookById(cartEntity.bookId)?.toBook()
                book?.let {
                    CartItem(
                        book = it,
                        quantity = cartEntity.quantity,
                        isRental = cartEntity.isRental,
                        rentalDays = cartEntity.rentalDays
                    )
                }
            }
        }
    }

    suspend fun addToCart(userId: String, book: Book, quantity: Int, isRental: Boolean = false, rentalDays: Int = 0): Result<Unit> {
        return try {
            val existingItem = database.cartDao().getCartItem(userId, book.id)
            if (existingItem != null) {
                val updatedItem = existingItem.copy(quantity = existingItem.quantity + quantity)
                database.cartDao().updateCartItem(updatedItem)
            } else {
                val cartItem = CartItemEntity(
                    bookId = book.id,
                    userId = userId,
                    quantity = quantity,
                    isRental = isRental,
                    rentalDays = rentalDays
                )
                database.cartDao().insertCartItem(cartItem)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCartItemQuantity(userId: String, bookId: String, newQuantity: Int): Result<Unit> {
        return try {
            val cartItem = database.cartDao().getCartItem(userId, bookId)
            if (cartItem != null) {
                if (newQuantity <= 0) {
                    database.cartDao().deleteCartItem(cartItem)
                } else {
                    database.cartDao().updateCartItem(cartItem.copy(quantity = newQuantity))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(userId: String, bookId: String): Result<Unit> {
        return try {
            database.cartDao().removeFromCart(userId, bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCart(userId: String): Result<Unit> {
        return try {
            database.cartDao().clearCart(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Wishlist operations
    fun getWishlistItems(userId: String): Flow<List<Book>> {
        return database.wishlistDao().getWishlistItems(userId).map { wishlistEntities ->
            wishlistEntities.mapNotNull { wishlistEntity ->
                database.bookDao().getBookById(wishlistEntity.bookId)?.toBook()
            }
        }
    }

    suspend fun addToWishlist(userId: String, book: Book): Result<Unit> {
        return try {
            val existingItem = database.wishlistDao().getWishlistItem(userId, book.id)
            if (existingItem == null) {
                val wishlistItem = WishlistItemEntity(
                    bookId = book.id,
                    userId = userId
                )
                database.wishlistDao().insertWishlistItem(wishlistItem)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWishlist(userId: String, bookId: String): Result<Unit> {
        return try {
            database.wishlistDao().removeFromWishlist(userId, bookId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isInWishlist(userId: String, bookId: String): Boolean {
        return database.wishlistDao().getWishlistItem(userId, bookId) != null
    }

    // Initialize sample data
    suspend fun initializeSampleData() {
        val books = getSampleBooksForDatabase()
        database.bookDao().insertBooks(books)
    }

    // Check if first time launch
    suspend fun isFirstTimeLaunch(): Boolean {
        return userPreferences.isFirstTimeLaunch.first()
    }

    suspend fun setFirstTimeLaunchCompleted() {
        userPreferences.setFirstTimeLaunch(false)
    }
}

// Extension functions to convert between entities and data classes
private fun UserEntity.toUser(): User {
    return User(
        id = id,
        name = name,
        email = email,
        profileImageUrl = profileImageUrl,
        rating = rating,
        location = location
    )
}

private fun User.toUserEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        profileImageUrl = profileImageUrl,
        rating = rating,
        location = location,
        isLoggedIn = true
    )
}

private fun BookEntity.toBook(): Book {
    val seller = User(
        id = sellerId,
        name = sellerName,
        email = "",
        profileImageUrl = "",
        rating = sellerRating,
        location = sellerLocation
    )

    return Book(
        id = id,
        title = title,
        author = author,
        coverImageUrl = coverImageUrl,
        price = price,
        rentalPrice = rentalPrice,
        condition = BookCondition.valueOf(condition),
        category = category,
        description = description,
        seller = seller,
        rating = rating,
        isAvailableForRent = isAvailableForRent,
        isAvailableForPurchase = isAvailableForPurchase,
        isNewBook = isNewBook
    )
}

private fun Book.toBookEntity(): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        author = author,
        coverImageUrl = coverImageUrl,
        price = price,
        rentalPrice = rentalPrice,
        condition = condition.name,
        category = category,
        description = description,
        sellerId = seller.id,
        sellerName = seller.name,
        sellerRating = seller.rating,
        sellerLocation = seller.location,
        rating = rating,
        isAvailableForRent = isAvailableForRent,
        isAvailableForPurchase = isAvailableForPurchase,
        isNewBook = isNewBook
    )
}

private fun getSampleBooksForDatabase(): List<BookEntity> {
    return listOf(
        BookEntity(
            id = "1",
            title = "The Psychology of Money",
            author = "Morgan Housel",
            coverImageUrl = "",
            price = 399.0,
            rentalPrice = 99.0,
            condition = BookCondition.NEW.name,
            category = "Finance",
            description = "Timeless lessons on wealth, greed, and happiness",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.6f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 10
        ),
        BookEntity(
            id = "2",
            title = "Data Structures and Algorithms",
            author = "Narasimha Karumanchi",
            coverImageUrl = "",
            price = 650.0,
            rentalPrice = 150.0,
            condition = BookCondition.GOOD.name,
            category = "Academic",
            description = "Complete guide to DSA",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 3
        ),
        BookEntity(
            id = "3",
            title = "Atomic Habits",
            author = "James Clear",
            coverImageUrl = "",
            price = 450.0,
            rentalPrice = 120.0,
            condition = BookCondition.LIKE_NEW.name,
            category = "Self Help",
            description = "An easy & proven way to build good habits",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.7f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 5
        ),
        BookEntity(
            id = "4",
            title = "Operating System Concepts",
            author = "Abraham Silberschatz",
            coverImageUrl = "",
            price = 800.0,
            rentalPrice = 200.0,
            condition = BookCondition.GOOD.name,
            category = "Academic",
            description = "Essential concepts in OS",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.5f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 2
        ),
        BookEntity(
            id = "5",
            title = "The Alchemist",
            author = "Paulo Coelho",
            coverImageUrl = "",
            price = 299.0,
            rentalPrice = 89.0,
            condition = BookCondition.GOOD.name,
            category = "Fiction",
            description = "A magical story about following your dreams",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.4f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 4
        )
    )
}
