package com.booknest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.booknest.app.database.BookNestDatabase
import com.booknest.app.datastore.UserPreferences
import com.booknest.app.repository.BookNestRepository
import com.booknest.app.ui.navigation.BookNestBottomNavigation
import com.booknest.app.ui.navigation.BookNestNavigation
import com.booknest.app.ui.theme.BookNestTheme
import com.booknest.app.viewmodel.BookNestViewModel
import com.booknest.app.viewmodel.BookNestViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var repository: BookNestRepository
    private lateinit var database: BookNestDatabase
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database and repository
        database = BookNestDatabase.getDatabase(this)
        userPreferences = UserPreferences(this)
        repository = BookNestRepository(database, userPreferences, this)

        setContent {
            // Create ViewModel
            val viewModel: BookNestViewModel = viewModel(
                factory = BookNestViewModelFactory(repository)
            )

            // Observe dark mode preference
            val isDarkMode by viewModel.isDarkMode.collectAsState(initial = false)

            BookNestTheme(darkTheme = isDarkMode) { // Use preference-based dark mode
                BookNestApp(repository, viewModel)
            }
        }
    }
}

@Composable
fun BookNestApp(repository: BookNestRepository, viewModel: BookNestViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    // Observe user login state to determine start destination
    val currentUser by viewModel.currentUser.collectAsState()
    val isFirstTime by repository.userPreferences.isFirstTimeLaunch.collectAsState(initial = true)

    val startDestination = when {
        isFirstTime -> "onboarding"
        currentUser != null -> "home"
        else -> "login"
    }

    // Routes that should show bottom navigation
    val bottomNavRoutes = listOf("home", "my_books", "sell", "rentals", "profile")
    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                BookNestBottomNavigation(
                    currentRoute = currentRoute ?: "home",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the graph's start destination to avoid building up a large stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        BookNestNavigation(
            navController = navController,
            startDestination = startDestination,
            viewModel = viewModel
        )
    }
}