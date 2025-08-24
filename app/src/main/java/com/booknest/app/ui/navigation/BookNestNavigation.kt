package com.booknest.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavBackStackEntry
import com.booknest.app.ui.auth.LoginScreen
import com.booknest.app.ui.auth.SignUpScreen
import com.booknest.app.ui.book.BookDetailsScreen
import com.booknest.app.ui.books.MyBooksScreen
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
import com.booknest.app.ui.settings.SettingsScreen
import com.booknest.app.ui.settings.ChangePasswordScreen
import com.booknest.app.ui.settings.NotificationSettingsScreen
import com.booknest.app.ui.settings.PrivacySettingsScreen
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
                    currentUser = currentUser, // Pass current user
                    isInWishlist = wishlistItems.any { it.id == book.id },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAddToCart = {
                        // Only add to cart if not own book
                        if (currentUser?.id != book.seller.id) {
                            viewModel.addToCart(book)
                            navController.navigate("cart")
                        }
                    },
                    onAddToWishlist = {
                        // Only allow wishlist if not own book
                        if (currentUser?.id != book.seller.id) {
                            if (wishlistItems.any { it.id == book.id }) {
                                viewModel.removeFromWishlist(book.id)
                            } else {
                                viewModel.addToWishlist(book)
                            }
                        }
                    },
                    onSellerClick = {
                        // Navigate to seller profile
                    },
                    onRentClick = {
                        // Only allow rent if not own book
                        if (currentUser?.id != book.seller.id) {
                            navController.navigate("rental_viewer/${book.id}")
                        }
                    }
                )
            }
        }

        composable("sell") {
            SellBookScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSubmit = { book, imageUri ->
                    viewModel.addBookWithImage(book, imageUri) { success, message ->
                        if (success) {
                            navController.navigate("home") {
                                popUpTo("sell") { inclusive = true }
                            }
                        }
                        // You can also show a toast or snackbar with the message here
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
                        navController.navigate("settings")
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
                MyBooksScreen(
                    user = currentUser!!,
                    myPurchases = allBooks.take(3),
                    myRentals = allBooks.drop(1).take(2),
                    myListings = allBooks.drop(2).take(2),
                    onBookClick = { book ->
                        navController.navigate("book_details/${book.id}")
                    },
                    onSellClick = {
                        navController.navigate("sell")
                    },
                    onSearchClick = {
                        // Navigate to search within my books
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
                    onSaveClick = { updatedUser, imageUri ->
                        viewModel.updateProfile(updatedUser, imageUri) { success, message ->
                            if (success) {
                                navController.popBackStack()
                            }
                        }
                    },
                    onImageClick = {
                        // Handle image picker
                    },
                    onChangePasswordClick = {
                        navController.navigate("change_password")
                    },
                    onNotificationSettingsClick = {
                        navController.navigate("notification_settings")
                    },
                    onPrivacySettingsClick = {
                        navController.navigate("privacy_settings")
                    },
                    onDeleteAccountClick = {
                        // Handle account deletion - for now just logout
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Add placeholder routes for settings screens
        composable("settings") {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onChangePasswordClick = {
                    navController.navigate("change_password")
                },
                onNotificationSettingsClick = {
                    navController.navigate("notification_settings")
                },
                onPrivacySettingsClick = {
                    navController.navigate("privacy_settings")
                },
                onLogoutClick = {
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = viewModel // Pass ViewModel to SettingsScreen
            )
        }

        composable("change_password") {
            ChangePasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onPasswordChanged = {
                    navController.popBackStack()
                },
                viewModel = viewModel
            )
        }

        composable("notification_settings") {
            NotificationSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("privacy_settings") {
            PrivacySettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// Sample rental data functions
fun getSampleActiveRentals(): List<RentalItem> {
    val sampleBooks = getSampleBooks()
    return listOf(
        RentalItem(
            book = sampleBooks[1], // Data Structures and Algorithms
            remainingDays = 12,
            totalDays = 30,
            isActive = true
        ),
        RentalItem(
            book = sampleBooks[3], // Operating System Concepts
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
            book = sampleBooks[4], // Psychology of Money (index 4, not 0)
            remainingDays = 0,
            totalDays = 30,
            isActive = false
        ),
        RentalItem(
            book = sampleBooks[5], // Atomic Habits (index 5, not 2)
            remainingDays = 0,
            totalDays = 30,
            isActive = false
        )
    )
}
