package com.booknest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavBackStackEntry
import com.booknest.app.ui.auth.LoginScreen
import com.booknest.app.ui.auth.SignUpScreen
import com.booknest.app.ui.book.BookDetailsScreen
import com.booknest.app.ui.cart.CartScreen
import com.booknest.app.ui.cart.CheckoutScreen
import com.booknest.app.ui.home.HomeScreen
import com.booknest.app.ui.home.getSampleBooks
import com.booknest.app.ui.onboarding.OnboardingScreen
import com.booknest.app.ui.profile.ProfileScreen
import com.booknest.app.ui.profile.EditProfileScreen
import com.booknest.app.ui.rentals.RentalsScreen
import com.booknest.app.ui.rentals.RentalItem
import com.booknest.app.ui.rental.RentalViewerScreen
import com.booknest.app.ui.sell.SellBookScreen
import com.booknest.app.viewmodel.BookNestViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun BookNestNavigation(
    navController: NavHostController,
    startDestination: String = "onboarding",
    viewModel: BookNestViewModel
) {
    // Observe data from ViewModel
    val currentUser by viewModel.currentUser.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState(initial = emptyList())
    val trendingBooks by viewModel.trendingBooks.collectAsState(initial = emptyList())
    val cartItems by viewModel.cartItems.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate("signup")
                },
                viewModel = viewModel
            )
        }

        composable("signup") {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        composable("home") {
            HomeScreen(
                books = allBooks,
                trendingBooks = trendingBooks,
                onBookClick = { book ->
                    navController.navigate("book_details/${book.id}")
                },
                onSearchClick = {
                    // Navigate to search screen
                },
                onNotificationClick = {
                    // Navigate to notifications
                },
                onSellClick = {
                    navController.navigate("sell")
                }
            )
        }

        composable("book_details/{bookId}") { backStackEntry: NavBackStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            val book = allBooks.find { it.id == bookId }

            if (book != null) {
                BookDetailsScreen(
                    book = book,
                    isInWishlist = wishlistItems.any { it.id == book.id },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAddToCart = {
                        viewModel.addToCart(book)
                        navController.navigate("cart")
                    },
                    onAddToWishlist = {
                        if (wishlistItems.any { it.id == book.id }) {
                            viewModel.removeFromWishlist(book.id)
                        } else {
                            viewModel.addToWishlist(book)
                        }
                    },
                    onSellerClick = {
                        // Navigate to seller profile
                    },
                    onRentClick = {
                        navController.navigate("rental_viewer/${book.id}")
                    }
                )
            }
        }

        composable("sell") {
            SellBookScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSubmit = { book ->
                    viewModel.addBook(book) { success, message ->
                        if (success) {
                            navController.navigate("home") {
                                popUpTo("sell") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable("cart") {
            CartScreen(
                cartItems = cartItems,
                onBackClick = {
                    navController.popBackStack()
                },
                onQuantityChange = { cartItem, newQuantity ->
                    viewModel.updateCartItemQuantity(cartItem.book.id, newQuantity)
                },
                onRemoveItem = { cartItem ->
                    viewModel.removeFromCart(cartItem.book.id)
                },
                onCheckout = {
                    navController.navigate("checkout")
                }
            )
        }

        composable("checkout") {
            CheckoutScreen(
                totalAmount = cartItems.sumOf { it.book.price * it.quantity },
                onBackClick = {
                    navController.popBackStack()
                },
                onPlaceOrder = {
                    // Clear cart after successful order
                    viewModel.clearCart()
                    navController.navigate("home") {
                        popUpTo("checkout") { inclusive = true }
                    }
                }
            )
        }

        composable("rental_viewer/{bookId}") { backStackEntry: NavBackStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            val book = allBooks.find { it.id == bookId }

            if (book != null && currentUser != null) {
                RentalViewerScreen(
                    book = book,
                    userEmail = currentUser!!.email,
                    remainingDays = 25,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onBookmarkClick = {
                        // Bookmark logic
                    }
                )
            }
        }

        composable("profile") {
            if (currentUser != null) {
                ProfileScreen(
                    user = currentUser!!,
                    myPurchases = allBooks.take(3),
                    myRentals = wishlistItems.take(2),
                    myListings = allBooks.drop(2).take(2),
                    wishlist = wishlistItems,
                    onEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onSettingsClick = {
                        // Navigate to settings
                    },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBookClick = { book ->
                        navController.navigate("book_details/${book.id}")
                    }
                )
            }
        }

        composable("my_books") {
            if (currentUser != null) {
                ProfileScreen(
                    user = currentUser!!,
                    myPurchases = allBooks.take(3),
                    myRentals = allBooks.drop(1).take(2),
                    myListings = allBooks.drop(2).take(2),
                    wishlist = wishlistItems,
                    onEditProfile = {
                        navController.navigate("edit_profile")
                    },
                    onSettingsClick = {
                        // Navigate to settings
                    },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBookClick = { book ->
                        navController.navigate("book_details/${book.id}")
                    }
                )
            }
        }

        composable("rentals") {
            RentalsScreen(
                activeRentals = getSampleActiveRentals(),
                pastRentals = getSamplePastRentals(),
                onBackClick = {
                    navController.popBackStack()
                },
                onBookClick = { book ->
                    navController.navigate("book_details/${book.id}")
                },
                onExtendRental = { rental ->
                    // Handle rental extension
                },
                onDownloadBook = { book ->
                    navController.navigate("rental_viewer/${book.id}")
                }
            )
        }

        composable("edit_profile") {
            if (currentUser != null) {
                EditProfileScreen(
                    user = currentUser!!,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onSaveClick = { updatedUser ->
                        viewModel.updateProfile(updatedUser) { success, message ->
                            if (success) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onImageClick = {
                        // Handle image picker
                    }
                )
            }
        }
    }
}

// Sample rental data functions
fun getSampleActiveRentals(): List<RentalItem> {
    val sampleBooks = getSampleBooks()
    return listOf(
        RentalItem(
            book = sampleBooks[1], // Data Structures book
            remainingDays = 12,
            totalDays = 30,
            isActive = true
        ),
        RentalItem(
            book = sampleBooks[3], // Operating System book
            remainingDays = 3,
            totalDays = 30,
            isActive = true
        )
    )
}

fun getSamplePastRentals(): List<RentalItem> {
    val sampleBooks = getSampleBooks()
    return listOf(
        RentalItem(
            book = sampleBooks[0], // Psychology of Money
            remainingDays = 0,
            totalDays = 30,
            isActive = false
        ),
        RentalItem(
            book = sampleBooks[2], // Atomic Habits
            remainingDays = 0,
            totalDays = 30,
            isActive = false
        )
    )
}
