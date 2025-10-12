package com.booknest.app.repository

import android.content.Context
import android.net.Uri
import com.booknest.app.database.BookNestDatabase
import com.booknest.app.database.entities.*
import com.booknest.app.data.*
import com.booknest.app.datastore.UserPreferences
import com.booknest.app.utils.PasswordUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BookNestRepository(
    private val database: BookNestDatabase,
    val userPreferences: UserPreferences,
    private val context: Context // Add context for file operations
) {

    // User operations
    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val userEntity = database.userDao().getUserByEmail(email)
            if (userEntity != null) {
                // Verify password
                if (PasswordUtils.verifyPassword(password, userEntity.passwordHash)) {
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
                    Result.failure(Exception("Invalid email or password"))
                }
            } else {
                Result.failure(Exception("Invalid email or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(name: String, email: String, password: String): Result<User> {
        return try {
            val existingUser = database.userDao().getUserByEmail(email)
            if (existingUser != null) {
                Result.failure(Exception("An account with this email already exists"))
            } else {
                // Validate password strength
                val passwordValidation = PasswordUtils.validatePasswordStrength(password)
                if (!passwordValidation.isValid) {
                    Result.failure(Exception(passwordValidation.errors.first()))
                } else {
                    val userId = UUID.randomUUID().toString()
                    val passwordHash = PasswordUtils.hashPassword(password)

                    val userEntity = UserEntity(
                        id = userId,
                        name = name,
                        email = email,
                        passwordHash = passwordHash,
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
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val userEntity = database.userDao().getUserById(userId)
            if (userEntity != null) {
                // Verify current password
                if (PasswordUtils.verifyPassword(currentPassword, userEntity.passwordHash)) {
                    // Validate new password strength
                    val passwordValidation = PasswordUtils.validatePasswordStrength(newPassword)
                    if (!passwordValidation.isValid) {
                        Result.failure(Exception(passwordValidation.errors.first()))
                    } else {
                        // Hash new password and update
                        val newPasswordHash = PasswordUtils.hashPassword(newPassword)
                        val updatedUser = userEntity.copy(passwordHash = newPasswordHash)
                        database.userDao().updateUser(updatedUser)
                        Result.success(Unit)
                    }
                } else {
                    Result.failure(Exception("Current password is incorrect"))
                }
            } else {
                Result.failure(Exception("User not found"))
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
            val existingItem = database.cartDao().getCartItem(userId, book.id, isRental)
            if (existingItem != null) {
                // Update quantity if exact same type (rental status) exists
                val updatedItem = existingItem.copy(quantity = existingItem.quantity + quantity)
                database.cartDao().updateCartItem(updatedItem)
            } else {
                // Create new item if no existing item with same rental status
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
            // Get all cart items for this book (both rental and purchase)
            val cartItems = database.cartDao().getAllCartItemsForBook(userId, bookId)
            if (cartItems.isNotEmpty()) {
                // For now, update the first item - you might want to specify which one to update
                val cartItem = cartItems.first()
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
        // Check if data already exists to avoid duplicates
        val existingBooks = database.bookDao().getAllBooks().first()
        if (existingBooks.isNotEmpty()) {
            return // Data already initialized
        }

        // Initialize sample users first
        val users = getSampleUsers()
        users.forEach { user ->
            database.userDao().insertUser(user)
        }

        // Initialize sample books
        val books = getSampleBooksForDatabase()
        database.bookDao().insertBooks(books)

        // Initialize sample cart items for the first user
        val cartItems = getSampleCartItems()
        cartItems.forEach { cartItem ->
            database.cartDao().insertCartItem(cartItem)
        }

        // Initialize sample wishlist items
        val wishlistItems = getSampleWishlistItems()
        wishlistItems.forEach { wishlistItem ->
            database.wishlistDao().insertWishlistItem(wishlistItem)
        }

        // Initialize sample orders and rentals
        val orders = getSampleOrders()
        orders.forEach { order ->
            database.orderDao().insertOrder(order)
        }

        val rentals = getSampleRentals()
        rentals.forEach { rental ->
            database.rentalDao().insertRental(rental)
        }
    }

    // Check if first time launch
    suspend fun isFirstTimeLaunch(): Boolean {
        return userPreferences.isFirstTimeLaunch.first()
    }

    suspend fun setFirstTimeLaunchCompleted() {
        userPreferences.setFirstTimeLaunch(false)
    }

    // Image handling methods
    suspend fun saveUserProfileImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            val savedImagePath = saveImageToInternalStorage(imageUri, "profile_$userId")
            if (savedImagePath != null) {
                // Update user profile image path in database
                database.userDao().updateUserProfileImage(userId, savedImagePath)
                Result.success(savedImagePath)
            } else {
                Result.failure(Exception("Failed to save image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBookImage(bookId: String, imageUri: Uri): Result<String> {
        return try {
            val savedImagePath = saveImageToInternalStorage(imageUri, "book_$bookId")
            if (savedImagePath != null) {
                // Update book cover image path in database
                database.bookDao().updateBookCoverImage(bookId, savedImagePath)
                Result.success(savedImagePath)
            } else {
                Result.failure(Exception("Failed to save image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveImageToInternalStorage(sourceUri: Uri, fileName: String): String? {
        return try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val destinationFile = File(imagesDir, "${fileName}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getImageUri(imagePath: String): Uri? {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                Uri.fromFile(file)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Update profile with image
    suspend fun updateUserProfileWithImage(updatedUser: User, imageUri: Uri?): Result<User> {
        return try {
            var finalUser = updatedUser

            // Save image if provided
            if (imageUri != null) {
                val imageResult = saveUserProfileImage(updatedUser.id, imageUri)
                if (imageResult.isSuccess) {
                    finalUser = updatedUser.copy(profileImageUrl = imageResult.getOrNull() ?: "")
                }
            }

            // Get the existing user to preserve the password hash
            val existingUserEntity = database.userDao().getUserById(finalUser.id)
            if (existingUserEntity == null) {
                return Result.failure(Exception("User not found"))
            }

            // Update user in database while preserving the password hash
            val userEntity = UserEntity(
                id = finalUser.id,
                name = finalUser.name,
                email = finalUser.email,
                passwordHash = existingUserEntity.passwordHash, // Preserve existing password hash
                profileImageUrl = finalUser.profileImageUrl,
                rating = finalUser.rating,
                location = finalUser.location,
                isLoggedIn = true
            )

            database.userDao().updateUser(userEntity)

            // Update preferences
            userPreferences.saveLoginState(
                isLoggedIn = true,
                userId = finalUser.id,
                userName = finalUser.name,
                userEmail = finalUser.email,
                profileImage = finalUser.profileImageUrl
            )

            Result.success(finalUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add book with image
    suspend fun addBookWithImage(book: Book, imageUri: Uri?): Result<Unit> {
        return try {
            var finalBook = book

            // Save image if provided
            if (imageUri != null) {
                val imageResult = saveBookImage(book.id, imageUri)
                if (imageResult.isSuccess) {
                    finalBook = book.copy(coverImageUrl = imageResult.getOrNull() ?: "")
                }
            }

            // Add book to database
            val bookEntity = BookEntity(
                id = finalBook.id,
                title = finalBook.title,
                author = finalBook.author,
                coverImageUrl = finalBook.coverImageUrl,
                price = finalBook.price,
                rentalPrice = finalBook.rentalPrice,
                condition = finalBook.condition.name,
                category = finalBook.category,
                description = finalBook.description,
                sellerId = finalBook.seller.id,
                rating = finalBook.rating,
                isAvailableForRent = finalBook.isAvailableForRent,
                isAvailableForPurchase = finalBook.isAvailableForPurchase,
                isNewBook = finalBook.isNewBook,
                sellerName = finalBook.seller.name,
                sellerRating = finalBook.seller.rating,
                sellerLocation = finalBook.seller.location
            )

            database.bookDao().insertBook(bookEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Order operations
    suspend fun createOrderFromCart(userId: String, deliveryAddress: String = ""): Result<Order> {
        return try {
            val cartItems = getCartItems(userId).first()
            if (cartItems.isEmpty()) {
                return Result.failure(Exception("Cart is empty"))
            }

            val orderId = UUID.randomUUID().toString()
            val orderDate = System.currentTimeMillis()

            val orderItems = mutableListOf<OrderItem>()
            val userBooks = mutableListOf<UserBookEntity>()
            var totalAmount = 0.0

            for (cartItem in cartItems) {
                val book = database.bookDao().getBookById(cartItem.book.id)?.toBook()
                if (book != null) {
                    val itemPrice = if (cartItem.isRental) {
                        book.rentalPrice * cartItem.quantity * cartItem.rentalDays
                    } else {
                        book.price * cartItem.quantity
                    }

                    val rentalStartDate = if (cartItem.isRental) orderDate else null
                    val rentalEndDate = if (cartItem.isRental) {
                        orderDate + (cartItem.rentalDays * 24 * 60 * 60 * 1000L)
                    } else null

                    val orderItem = OrderItem(
                        book = book,
                        quantity = cartItem.quantity,
                        isRental = cartItem.isRental,
                        rentalDays = cartItem.rentalDays,
                        itemPrice = itemPrice,
                        rentalStartDate = rentalStartDate,
                        rentalEndDate = rentalEndDate
                    )

                    orderItems.add(orderItem)
                    totalAmount += itemPrice

                    // Update stock quantity for purchases
                    if (!cartItem.isRental) {
                        database.bookDao().updateStock(book.id, cartItem.quantity)
                    }

                    // Create rental record if it's a rental
                    if (cartItem.isRental) {
                        val rentalEntity = RentalEntity(
                            id = UUID.randomUUID().toString(),
                            bookId = book.id,
                            userId = userId,
                            startDate = orderDate,
                            endDate = rentalEndDate!!,
                            totalDays = cartItem.rentalDays,
                            isActive = true,
                            rentalPrice = itemPrice
                        )
                        database.rentalDao().insertRental(rentalEntity)
                    }

                    // Add book to user's collection (for both purchases and rentals)
                    val userBook = UserBookEntity(
                        userId = userId,
                        bookId = book.id,
                        purchaseDate = orderDate,
                        orderId = orderId,
                        quantity = cartItem.quantity,
                        purchasePrice = itemPrice,
                        accessExpiryDate = rentalEndDate, // null for purchases, expiry date for rentals
                        isRental = cartItem.isRental
                    )
                    userBooks.add(userBook)
                }
            }

            // Create order entity
            val orderEntity = OrderEntity(
                id = orderId,
                userId = userId,
                totalAmount = totalAmount,
                orderStatus = OrderStatus.CONFIRMED.name,
                paymentMethod = "To be integrated", // Placeholder for payment gateway
                deliveryAddress = deliveryAddress,
                orderDate = orderDate
            )

            // Create order items entities
            val orderItemEntities = orderItems.map { orderItem ->
                OrderItemEntity(
                    orderId = orderId,
                    bookId = orderItem.book.id,
                    quantity = orderItem.quantity,
                    price = orderItem.itemPrice,
                    isRental = orderItem.isRental,
                    rentalDays = orderItem.rentalDays,
                    rentalStartDate = orderItem.rentalStartDate,
                    rentalEndDate = orderItem.rentalEndDate
                )
            }

            // Insert order and order items
            database.orderDao().insertOrder(orderEntity)
            database.orderDao().insertOrderItems(orderItemEntities)

            // Insert user books (purchased/rented books) - THIS IS THE KEY FIX
            database.userBooksDao().insertUserBooks(userBooks)

            // Clear cart after successful order
            database.cartDao().clearCart(userId)

            val order = Order(
                id = orderId,
                userId = userId,
                items = orderItems,
                totalAmount = totalAmount,
                orderDate = orderDate,
                status = OrderStatus.CONFIRMED,
                paymentMethod = "To be integrated",
                deliveryAddress = deliveryAddress
            )

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get user orders
    fun getUserOrders(userId: String): Flow<List<Order>> {
        return database.orderDao().getUserOrders(userId).map { orderEntities ->
            orderEntities.map { orderEntity ->
                val orderItems = database.orderDao().getOrderItems(orderEntity.id).map { orderItemEntity ->
                    val book = database.bookDao().getBookById(orderItemEntity.bookId)?.toBook()
                    if (book != null) {
                        OrderItem(
                            book = book,
                            quantity = orderItemEntity.quantity,
                            isRental = orderItemEntity.isRental,
                            rentalDays = orderItemEntity.rentalDays,
                            itemPrice = orderItemEntity.price,
                            rentalStartDate = orderItemEntity.rentalStartDate,
                            rentalEndDate = orderItemEntity.rentalEndDate
                        )
                    } else null
                }.filterNotNull()

                Order(
                    id = orderEntity.id,
                    userId = orderEntity.userId,
                    items = orderItems,
                    totalAmount = orderEntity.totalAmount,
                    orderDate = orderEntity.orderDate,
                    status = OrderStatus.valueOf(orderEntity.orderStatus),
                    paymentMethod = orderEntity.paymentMethod,
                    deliveryAddress = orderEntity.deliveryAddress
                )
            }
        }
    }

    suspend fun getOrderById(orderId: String): Order? {
        return try {
            val orderEntity = database.orderDao().getOrderById(orderId) ?: return null
            val orderItems = database.orderDao().getOrderItems(orderId).map { orderItemEntity ->
                val book = database.bookDao().getBookById(orderItemEntity.bookId)?.toBook()
                if (book != null) {
                    OrderItem(
                        book = book,
                        quantity = orderItemEntity.quantity,
                        isRental = orderItemEntity.isRental,
                        rentalDays = orderItemEntity.rentalDays,
                        itemPrice = orderItemEntity.price,
                        rentalStartDate = orderItemEntity.rentalStartDate,
                        rentalEndDate = orderItemEntity.rentalEndDate
                    )
                } else null
            }.filterNotNull()

            Order(
                id = orderEntity.id,
                userId = orderEntity.userId,
                items = orderItems,
                totalAmount = orderEntity.totalAmount,
                orderDate = orderEntity.orderDate,
                status = OrderStatus.valueOf(orderEntity.orderStatus),
                paymentMethod = orderEntity.paymentMethod,
                deliveryAddress = orderEntity.deliveryAddress
            )
        } catch (e: Exception) {
            null
        }
    }

    // Rental operations
    fun getActiveRentals(userId: String): kotlinx.coroutines.flow.Flow<List<com.booknest.app.ui.rentals.RentalItem>> {
        return database.rentalDao().getActiveRentals(userId).map { rentalEntities ->
            rentalEntities.mapNotNull { rentalEntity ->
                val book = database.bookDao().getBookById(rentalEntity.bookId)?.toBook()
                if (book != null) {
                    val remainingDays = ((rentalEntity.endDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                    com.booknest.app.ui.rentals.RentalItem(
                        book = book,
                        remainingDays = maxOf(0, remainingDays),
                        totalDays = rentalEntity.totalDays,
                        isActive = rentalEntity.isActive && remainingDays > 0
                    )
                } else null
            }
        }
    }

    fun getPastRentals(userId: String): kotlinx.coroutines.flow.Flow<List<com.booknest.app.ui.rentals.RentalItem>> {
        return database.rentalDao().getPastRentals(userId).map { rentalEntities ->
            rentalEntities.mapNotNull { rentalEntity ->
                val book = database.bookDao().getBookById(rentalEntity.bookId)?.toBook()
                if (book != null) {
                    com.booknest.app.ui.rentals.RentalItem(
                        book = book,
                        remainingDays = 0,
                        totalDays = rentalEntity.totalDays,
                        isActive = false
                    )
                } else null
            }
        }
    }

    // User Books operations - NEW METHODS FOR PURCHASED BOOKS
    fun getUserPurchasedBooks(userId: String): Flow<List<Book>> {
        return database.userBooksDao().getUserPurchasedBooks(userId).map { userBookEntities ->
            userBookEntities.mapNotNull { userBookEntity ->
                database.bookDao().getBookById(userBookEntity.bookId)?.toBook()
            }
        }
    }

    fun getUserRentedBooks(userId: String): Flow<List<Book>> {
        return database.userBooksDao().getUserActiveRentedBooks(userId).map { userBookEntities ->
            userBookEntities.mapNotNull { userBookEntity ->
                database.bookDao().getBookById(userBookEntity.bookId)?.toBook()
            }
        }
    }

    fun getAllUserBooks(userId: String): Flow<List<Book>> {
        return database.userBooksDao().getAllUserBooks(userId).map { userBookEntities ->
            userBookEntities.mapNotNull { userBookEntity ->
                database.bookDao().getBookById(userBookEntity.bookId)?.toBook()
            }
        }
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
        passwordHash = "", // This will be handled separately when updating passwords
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
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/81Lb75rUhLL.jpg",
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
            coverImageUrl = "https://m.media-amazon.com/images/I/71HMyqG6MRL.jpg",
            price = 650.0,
            rentalPrice = 150.0,
            condition = BookCondition.GOOD.name,
            category = "Academic",
            description = "Complete guide to DSA with implementation in Java and C++",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
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
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/91bYsX41DVL.jpg",
            price = 450.0,
            rentalPrice = 120.0,
            condition = BookCondition.LIKE_NEW.name,
            category = "Self Help",
            description = "An easy & proven way to build good habits and break bad ones",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
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
            coverImageUrl = "https://m.media-amazon.com/images/I/81SwKCia7VL.jpg",
            price = 800.0,
            rentalPrice = 200.0,
            condition = BookCondition.GOOD.name,
            category = "Academic",
            description = "Essential concepts in operating systems design and implementation",
            sellerId = "seller4",
            sellerName = "Mike Johnson",
            sellerRating = 4.4f,
            sellerLocation = "Chennai",
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
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/71aFt4+OTOL.jpg",
            price = 299.0,
            rentalPrice = 89.0,
            condition = BookCondition.GOOD.name,
            category = "Fiction",
            description = "A magical story about following your dreams and finding your purpose",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.4f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 4
        ),
        BookEntity(
            id = "6",
            title = "Harry Potter and the Philosopher's Stone",
            author = "J.K. Rowling",
            coverImageUrl = "https://m.media-amazon.com/images/I/81YOuOGFCJL.jpg",
            price = 350.0,
            rentalPrice = 75.0,
            condition = BookCondition.NEW.name,
            category = "Fantasy",
            description = "The magical beginning of Harry Potter's journey at Hogwarts",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
            rating = 4.9f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 8
        ),
        BookEntity(
            id = "7",
            title = "Clean Code",
            author = "Robert C. Martin",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/41jEbK-jG+L.jpg",
            price = 550.0,
            rentalPrice = 140.0,
            condition = BookCondition.LIKE_NEW.name,
            category = "Programming",
            description = "A handbook of agile software craftsmanship",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
            rating = 4.7f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 6
        ),
        BookEntity(
            id = "8",
            title = "Sapiens",
            author = "Yuval Noah Harari",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/713jIoMO3UL.jpg",
            price = 425.0,
            rentalPrice = 110.0,
            condition = BookCondition.NEW.name,
            category = "History",
            description = "A brief history of humankind and our journey from cavemen to space explorers",
            sellerId = "seller4",
            sellerName = "Mike Johnson",
            sellerRating = 4.4f,
            sellerLocation = "Chennai",
            rating = 4.6f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 7
        ),
        BookEntity(
            id = "9",
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            coverImageUrl = "https://www.gutenberg.org/cache/epub/64317/pg64317.cover.medium.jpg",
            price = 250.0,
            rentalPrice = 65.0,
            condition = BookCondition.GOOD.name,
            category = "Classic Literature",
            description = "A timeless American classic about the Jazz Age and the American Dream",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.3f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 3
        ),
        BookEntity(
            id = "10",
            title = "Think and Grow Rich",
            author = "Napoleon Hill",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/71UypkUjStL.jpg",
            price = 320.0,
            rentalPrice = 85.0,
            condition = BookCondition.FAIR.name,
            category = "Self Help",
            description = "The classic guide to wealth and success through the power of thought",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
            rating = 4.2f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 4
        ),
        BookEntity(
            id = "11",
            title = "Introduction to Machine Learning",
            author = "Alpaydin Ethem",
            coverImageUrl = "https://m.media-amazon.com/images/I/71EUnD27B0L._SY522_.jpg",
            price = 750.0,
            rentalPrice = 180.0,
            condition = BookCondition.NEW.name,
            category = "Academic",
            description = "Comprehensive introduction to machine learning algorithms and applications",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
            rating = 4.5f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 5
        ),
        BookEntity(
            id = "12",
            title = "The Hobbit",
            author = "J.R.R. Tolkien",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/91b0C2YNSrL.jpg",
            price = 380.0,
            rentalPrice = 95.0,
            condition = BookCondition.GOOD.name,
            category = "Fantasy",
            description = "A delightful adventure story that precedes The Lord of the Rings",
            sellerId = "seller4",
            sellerName = "Mike Johnson",
            sellerRating = 4.4f,
            sellerLocation = "Chennai",
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 6
        ),
        BookEntity(
            id = "13",
            title = "Rich Dad Poor Dad",
            author = "Robert Kiyosaki",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/81bsw6fnUiL.jpg",
            price = 340.0,
            rentalPrice = 90.0,
            condition = BookCondition.LIKE_NEW.name,
            category = "Finance",
            description = "What the rich teach their kids about money that the poor and middle class do not",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.4f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 8
        ),
        BookEntity(
            id = "14",
            title = "The Pragmatic Programmer",
            author = "David Thomas",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/41as+WafrFL.jpg",
            price = 620.0,
            rentalPrice = 155.0,
            condition = BookCondition.GOOD.name,
            category = "Programming",
            description = "Your journey to mastery in software development",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
            rating = 4.6f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 4
        ),
        BookEntity(
            id = "15",
            title = "1984",
            author = "George Orwell",
            coverImageUrl = "https://m.media-amazon.com/images/I/81YxjLkcAdL._SY522_.jpg",
            price = 275.0,
            rentalPrice = 70.0,
            condition = BookCondition.GOOD.name,
            category = "Classic Literature",
            description = "A dystopian social science fiction novel about totalitarian control",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
            rating = 4.7f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 5
        ),

        BookEntity(
            id = "16",
            title = "Zero to One",
            author = "Peter Thiel",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/71m-MxdJ2WL.jpg",
            price = 400.0,
            rentalPrice = 100.0,
            condition = BookCondition.NEW.name,
            category = "Finance",
            description = "Notes on startups and how to build the future",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.5f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 7
        ),
        BookEntity(
            id = "17",
            title = "Cracking the Coding Interview",
            author = "Gayle Laakmann McDowell",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/41+e3refnZL.jpg",
            price = 850.0,
            rentalPrice = 220.0,
            condition = BookCondition.GOOD.name,
            category = "Academic",
            description = "189 programming questions and solutions for interview prep",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
            rating = 4.9f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 3
        ),
        BookEntity(
            id = "18",
            title = "The Subtle Art of Not Giving a F*ck",
            author = "Mark Manson",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/71QKQ9mwV7L.jpg",
            price = 399.0,
            rentalPrice = 99.0,
            condition = BookCondition.LIKE_NEW.name,
            category = "Self Help",
            description = "A counterintuitive approach to living a good life",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
            rating = 4.6f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 5
        ),
        BookEntity(
            id = "19",
            title = "To Kill a Mockingbird",
            author = "Harper Lee",
            coverImageUrl = "https://m.media-amazon.com/images/I/81gepf1eMqL._SY522_.jpg",
            price = 310.0,
            rentalPrice = 80.0,
            condition = BookCondition.GOOD.name,
            category = "Fiction",
            description = "A Pulitzer Prize-winning novel exploring racial injustice",
            sellerId = "seller4",
            sellerName = "Mike Johnson",
            sellerRating = 4.4f,
            sellerLocation = "Chennai",
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 4
        ),
        BookEntity(
            id = "20",
            title = "Harry Potter and the Chamber of Secrets",
            author = "J.K. Rowling",
            coverImageUrl = "https://m.media-amazon.com/images/I/81lAPl9Fl0L.jpg",
            price = 370.0,
            rentalPrice = 85.0,
            condition = BookCondition.NEW.name,
            category = "Fantasy",
            description = "The second year at Hogwarts brings new dangers and mysteries",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 8
        ),
        BookEntity(
            id = "21",
            title = "Design Patterns: Elements of Reusable Object-Oriented Software",
            author = "Erich Gamma",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/81gtKoapHFL.jpg",
            price = 980.0,
            rentalPrice = 250.0,
            condition = BookCondition.GOOD.name,
            category = "Programming",
            description = "The classic book on software design patterns",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
            rating = 4.9f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 3
        ),
        BookEntity(
            id = "22",
            title = "Homo Deus",
            author = "Yuval Noah Harari",
            coverImageUrl = "https://m.media-amazon.com/images/I/71N6LbagzSL._UF1000,1000_QL80_.jpg",
            price = 430.0,
            rentalPrice = 115.0,
            condition = BookCondition.NEW.name,
            category = "History",
            description = "A look at the future of humanity",
            sellerId = "seller4",
            sellerName = "Mike Johnson",
            sellerRating = 4.4f,
            sellerLocation = "Chennai",
            rating = 4.5f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 6
        ),
        BookEntity(
            id = "23",
            title = "Pride and Prejudice",
            author = "Jane Austen",
            coverImageUrl = "https://m.media-amazon.com/images/M/MV5BMTA1NDQ3NTcyOTNeQTJeQWpwZ15BbWU3MDA0MzA4MzE@._V1_.jpg",
            price = 270.0,
            rentalPrice = 70.0,
            condition = BookCondition.GOOD.name,
            category = "Classic Literature",
            description = "A classic romantic novel set in 19th-century England",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.6f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 5
        ),
        BookEntity(
            id = "24",
            title = "The Intelligent Investor",
            author = "Benjamin Graham",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/91+t0Di07FL.jpg",
            price = 550.0,
            rentalPrice = 140.0,
            condition = BookCondition.LIKE_NEW.name,
            category = "Finance",
            description = "The definitive book on value investing",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 5
        ),
        BookEntity(
            id = "25",
            title = "Introduction to Algorithms",
            author = "Thomas H. Cormen",
            coverImageUrl = "https://m.media-amazon.com/images/I/61Mw06x2XcL.jpg",
            price = 1150.0,
            rentalPrice = 300.0,
            condition = BookCondition.GOOD.name,
            category = "Academic",
            description = "The classic algorithms textbook",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
            rating = 4.9f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 3
        ),
        BookEntity(
            id = "26",
            title = "The Power of Now",
            author = "Eckhart Tolle",
            coverImageUrl = "https://m.media-amazon.com/images/I/61Ij8nLooNL._UF1000,1000_QL80_.jpg",
            price = 380.0,
            rentalPrice = 95.0,
            condition = BookCondition.NEW.name,
            category = "Self Help",
            description = "A guide to spiritual enlightenment",
            sellerId = "seller4",
            sellerName = "Mike Johnson",
            sellerRating = 4.4f,
            sellerLocation = "Chennai",
            rating = 4.7f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 6
        ),
        BookEntity(
            id = "27",
            title = "The Catcher in the Rye",
            author = "J.D. Salinger",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/81OthjkJBuL.jpg",
            price = 320.0,
            rentalPrice = 80.0,
            condition = BookCondition.GOOD.name,
            category = "Fiction",
            description = "A story of teenage angst and alienation",
            sellerId = "seller1",
            sellerName = "John Doe",
            sellerRating = 4.5f,
            sellerLocation = "Mumbai",
            rating = 4.3f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 4
        ),
        BookEntity(
            id = "28",
            title = "Harry Potter and the Prisoner of Azkaban",
            author = "J.K. Rowling",
            coverImageUrl = "https://m.media-amazon.com/images/I/81a4kCNuH+L.jpg",
            price = 390.0,
            rentalPrice = 95.0,
            condition = BookCondition.NEW.name,
            category = "Fantasy",
            description = "The third installment of Harry's adventures at Hogwarts",
            sellerId = "seller2",
            sellerName = "Jane Smith",
            sellerRating = 4.8f,
            sellerLocation = "Pune",
            rating = 4.9f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 8
        ),
        BookEntity(
            id = "29",
            title = "Refactoring: Improving the Design of Existing Code",
            author = "Martin Fowler",
            coverImageUrl = "https://m.media-amazon.com/images/I/71yPDDIwcoL._UF1000,1000_QL80_.jpg",
            price = 980.0,
            rentalPrice = 240.0,
            condition = BookCondition.GOOD.name,
            category = "Programming",
            description = "Classic guide on improving existing codebases",
            sellerId = "seller3",
            sellerName = "Alice Johnson",
            sellerRating = 4.6f,
            sellerLocation = "Bangalore",
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false,
            stockQuantity = 5
        ),
        BookEntity(
            id = "30",
            title = "A Brief History of Time",
            author = "Stephen Hawking",
            coverImageUrl = "https://images-na.ssl-images-amazon.com/images/I/71UypkUjStL.jpg",
            price = 420.0,
            rentalPrice = 110.0,
            condition = BookCondition.NEW.name,
            category = "History",
            description = "Hawking's classic on the nature of the universe",
            sellerId = "seller4",
            sellerName = "Mike Johnson",
            sellerRating = 4.4f,
            sellerLocation = "Chennai",
            rating = 4.7f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true,
            stockQuantity = 6
        )
    )
}

private fun getSampleUsers(): List<UserEntity> {
    return listOf(
        UserEntity(
            id = "user1",
            name = "Test User",
            email = "test@example.com",
            passwordHash = PasswordUtils.hashPassword("password123"),
            profileImageUrl = "",
            rating = 4.5f,
            location = "Mumbai, India",
            isLoggedIn = false
        ),
        UserEntity(
            id = "seller1",
            name = "John Doe",
            email = "john.doe@example.com",
            passwordHash = PasswordUtils.hashPassword("password123"),
            profileImageUrl = "",
            rating = 4.5f,
            location = "Mumbai, India",
            isLoggedIn = false
        ),
        UserEntity(
            id = "seller2",
            name = "Jane Smith",
            email = "jane.smith@example.com",
            passwordHash = PasswordUtils.hashPassword("password123"),
            profileImageUrl = "",
            rating = 4.8f,
            location = "Pune, India",
            isLoggedIn = false
        ),
        UserEntity(
            id = "seller3",
            name = "Alice Johnson",
            email = "alice.johnson@example.com",
            passwordHash = PasswordUtils.hashPassword("password123"),
            profileImageUrl = "",
            rating = 4.6f,
            location = "Bangalore, India",
            isLoggedIn = false
        ),
        UserEntity(
            id = "seller4",
            name = "Mike Johnson",
            email = "mike.johnson@example.com",
            passwordHash = PasswordUtils.hashPassword("password123"),
            profileImageUrl = "",
            rating = 4.4f,
            location = "Chennai, India",
            isLoggedIn = false
        )
    )
}

private fun getSampleCartItems(): List<CartItemEntity> {
    return listOf(
        CartItemEntity(
            bookId = "1",
            userId = "user1",
            quantity = 1,
            isRental = false,
            rentalDays = 0
        ),
        CartItemEntity(
            bookId = "2",
            userId = "user1",
            quantity = 2,
            isRental = true,
            rentalDays = 7
        ),
        CartItemEntity(
            bookId = "3",
            userId = "user2",
            quantity = 1,
            isRental = false,
            rentalDays = 0
        )
    )
}

private fun getSampleWishlistItems(): List<WishlistItemEntity> {
    return listOf(
        WishlistItemEntity(
            bookId = "1",
            userId = "user1"
        ),
        WishlistItemEntity(
            bookId = "2",
            userId = "user2"
        ),
        WishlistItemEntity(
            bookId = "3",
            userId = "user2"
        )
    )
}

private fun getSampleOrders(): List<OrderEntity> {
    return listOf(
        OrderEntity(
            id = "order1",
            userId = "user1",
            totalAmount = 399.0,
            orderStatus = OrderStatus.CONFIRMED.name,
            paymentMethod = "Credit Card",
            deliveryAddress = "123, Baker Street, Mumbai",
            orderDate = System.currentTimeMillis()
        ),
        OrderEntity(
            id = "order2",
            userId = "user2",
            totalAmount = 650.0,
            orderStatus = OrderStatus.PENDING.name,
            paymentMethod = "Debit Card",
            deliveryAddress = "456, Elm Street, Pune",
            orderDate = System.currentTimeMillis()
        )
    )
}

private fun getSampleRentals(): List<RentalEntity> {
    return listOf(
        RentalEntity(
            id = "rental1",
            bookId = "2",
            userId = "user1",
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000),
            totalDays = 7,
            isActive = true,
            rentalPrice = 150.0
        ),
        RentalEntity(
            id = "rental2",
            bookId = "3",
            userId = "user2",
            startDate = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000),
            endDate = System.currentTimeMillis() + (5 * 24 * 60 * 60 * 1000),
            totalDays = 5,
            isActive = true,
            rentalPrice = 120.0
        )
    )
}

