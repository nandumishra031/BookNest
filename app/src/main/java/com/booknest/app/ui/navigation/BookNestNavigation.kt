package com.booknest.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.booknest.app.data.Order
import com.booknest.app.ui.auth.LoginScreen
import com.booknest.app.ui.auth.SignUpScreen
import com.booknest.app.ui.book.BookDetailsScreen
import com.booknest.app.ui.books.AllBooksScreen
import com.booknest.app.ui.books.MyBooksScreen
import com.booknest.app.ui.cart.CartScreen
import com.booknest.app.ui.cart.CheckoutScreen
import com.booknest.app.ui.home.HomeScreen
import com.booknest.app.ui.onboarding.OnboardingScreen
import com.booknest.app.ui.order.OrderSuccessScreen
import com.booknest.app.ui.profile.ProfileScreen
import com.booknest.app.ui.profile.EditProfileScreen
import com.booknest.app.ui.rentals.RentalsScreen
import com.booknest.app.ui.rental.RentalViewerScreen
import com.booknest.app.ui.sell.SellBookScreen
import com.booknest.app.ui.settings.SettingsScreen
import com.booknest.app.ui.settings.ChangePasswordScreen
import com.booknest.app.ui.settings.NotificationSettingsScreen
import com.booknest.app.ui.settings.PrivacySettingsScreen
import com.booknest.app.viewmodel.BookNestViewModel

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
    val activeRentals by viewModel.activeRentals.collectAsState()
    val pastRentals by viewModel.pastRentals.collectAsState()

    // Add the missing purchased books data - THIS WAS MISSING!
    val userPurchasedBooks by viewModel.userPurchasedBooks.collectAsState()

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
                viewModel = viewModel,
                onBookClick = { book ->
                    navController.navigate("book_details/${book.id}")
                },
                onCategoryClick = { category ->
                    navController.navigate("all_books/$category")
                },
                onSeeAllClick = { category ->
                    navController.navigate("all_books/$category")
                }
            )
        }

        composable("book_details/{bookId}") { backStackEntry: NavBackStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            // Only use database books - no fallback to sample books
            val book = allBooks.find { it.id == bookId }

            if (book != null) {
                BookDetailsScreen(
                    book = book,
                    currentUser = currentUser,
                    isInWishlist = wishlistItems.any { it.id == book.id },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAddToCart = {
                        // Check if user is logged in first
                        if (currentUser == null) {
                            // Navigate to login if not logged in
                            navController.navigate("login")
                        } else if (currentUser?.id != book.seller.id) {
                            viewModel.addToCart(book, quantity = 1, isRental = false, rentalDays = 0)
                            // Navigate to cart to show success
                            navController.navigate("cart")
                        }
                    },
                    onAddToWishlist = {
                        // Check if user is logged in first
                        if (currentUser == null) {
                            // Navigate to login if not logged in
                            navController.navigate("login")
                        } else if (currentUser?.id != book.seller.id) {
                            if (wishlistItems.any { it.id == book.id }) {
                                viewModel.removeFromWishlist(book.id)
                            } else {
                                viewModel.addToWishlist(book)
                            }
                        }
                    },
                    onSellerClick = {
                        // Navigate to seller profile (placeholder for now)
                    },
                    onRentClick = {
                        // Check if user is logged in first
                        if (currentUser == null) {
                            // Navigate to login if not logged in
                            navController.navigate("login")
                        } else if (currentUser?.id != book.seller.id) {
                            // For now, add as rental to cart with default 30 days
                            viewModel.addToCart(book, quantity = 1, isRental = true, rentalDays = 30)
                            navController.navigate("cart")
                        }
                    }
                )
            } else {
                // Show error screen when book is not found
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Book Not Found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The book you're looking for could not be found. Book ID: $bookId",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Text("Go Back")
                    }
                }
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
                totalAmount = viewModel.calculateCartTotal(),
                onBackClick = {
                    navController.popBackStack()
                },
                onPlaceOrder = {
                    // Use the new order system instead of just clearing cart
                    viewModel.placeOrder() { success, message, order ->
                        if (success && order != null) {
                            // Navigate to order success screen (we'll create this route)
                            navController.navigate("order_success/${order.id}") {
                                popUpTo("checkout") { inclusive = true }
                            }
                        } else {
                            // Handle error - for now just go back to cart
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable("order_success/{orderId}") { backStackEntry: NavBackStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            var order by remember { mutableStateOf<Order?>(null) }

            LaunchedEffect(orderId) {
                if (orderId != null) {
                    viewModel.getOrderById(orderId) { foundOrder ->
                        order = foundOrder
                    }
                }
            }

            if (order != null) {
                OrderSuccessScreen(
                    order = order!!,
                    onNavigateToHome = {
                        navController.navigate("home") {
                            popUpTo("order_success/{orderId}") { inclusive = true }
                        }
                    },
                    onViewOrderDetails = {
                        // For now, just go to home - you can add order details screen later
                        navController.navigate("home") {
                            popUpTo("order_success/{orderId}") { inclusive = true }
                        }
                    }
                )
            } else {
                // Show loading or error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (orderId == null) {
                        Text("Invalid order ID")
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
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
            } else {
                // Show error screen when book is not found or user is not logged in
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (currentUser == null) Icons.Default.AccountCircle
                                     else Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (currentUser == null) "Please Log In" else "Book Not Found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentUser == null) "You need to be logged in to view rentals."
                               else "The book you're looking for could not be found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (currentUser == null) {
                                navController.navigate("login") {
                                    popUpTo("rental_viewer/{bookId}") { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Text(if (currentUser == null) "Go to Login" else "Go Back")
                    }
                }
            }
        }

        composable("profile") {
            if (currentUser != null) {
                ProfileScreen(
                    user = currentUser!!,
                    myPurchases = userPurchasedBooks, // Use actual purchased books data
                    myRentals = activeRentals.map { it.book }, // Use synchronized rental data
                    myListings = allBooks.filter { it.seller.id == currentUser!!.id },
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
                    myPurchases = userPurchasedBooks, // Use actual purchased books data
                    myRentals = activeRentals.map { it.book }, // Use synchronized rental data
                    myListings = allBooks.filter { it.seller.id == currentUser!!.id },
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
                activeRentals = activeRentals, // Use synchronized rental data from ViewModel
                pastRentals = pastRentals, // Use synchronized rental data from ViewModel
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

        composable("all_books/{category}") { backStackEntry: NavBackStackEntry ->
            val category = backStackEntry.arguments?.getString("category")

            AllBooksScreen(
                books = allBooks,
                category = "",
                onBookClick = { book ->
                    navController.navigate("book_details/${book.id}")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
