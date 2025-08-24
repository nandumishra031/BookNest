package com.booknest.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booknest.app.data.Book
import com.booknest.app.data.BookCondition
import com.booknest.app.data.User
import com.booknest.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    books: List<Book> = emptyList(),
    trendingBooks: List<Book> = emptyList(),
    onBookClick: (Book) -> Unit,
    onNotificationClick: () -> Unit,
    onSellClick: () -> Unit = {}
) {
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Use provided data or fallback to sample data if empty
    val displayBooks = if (books.isNotEmpty()) books else getSampleBooks()
    val displayTrendingBooks = if (trendingBooks.isNotEmpty()) trendingBooks else {
        // Dynamic trending books: mix of different categories
        displayBooks.shuffled().take(6)
    }

    // Filter books based on search query
    val filteredBooks = remember(searchQuery, displayBooks) {
        if (searchQuery.isBlank()) {
            displayBooks
        } else {
            displayBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                book.author.contains(searchQuery, ignoreCase = true) ||
                book.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Dynamic section filtering with fallbacks
    val academicBooks = remember(displayBooks) {
        val academic = displayBooks.filter { it.category == "Academic" }
        if (academic.isEmpty()) {
            // Fallback: show books with "academic" keywords in title/description
            displayBooks.filter {
                it.title.contains("algorithm", ignoreCase = true) ||
                it.title.contains("system", ignoreCase = true) ||
                it.title.contains("data", ignoreCase = true) ||
                it.description.contains("guide", ignoreCase = true)
            }.take(4)
        } else {
            academic
        }
    }

    val recommendedBooks = remember(filteredBooks) {
        // More intelligent recommendation: exclude academic books, mix different categories
        val nonAcademic = filteredBooks.filter { it.category != "Academic" }
        if (nonAcademic.size >= 4) {
            nonAcademic.shuffled().take(4)
        } else {
            // Fallback: take any available books
            filteredBooks.take(4)
        }
    }

    val nearbyBooks = remember(filteredBooks) {
        // Simulate "nearby" books: prioritize good condition and reasonable price
        val nearbyFiltered = filteredBooks.filter {
            it.condition in listOf(BookCondition.NEW, BookCondition.LIKE_NEW, BookCondition.GOOD) &&
            it.price < 1000 // Reasonable price range
        }
        if (nearbyFiltered.isEmpty()) {
            filteredBooks.take(3) // Fallback to any books
        } else {
            nearbyFiltered.take(3)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with search and notifications
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSearchActive) "Search Results" else "Discover Books 📚",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (!isSearchActive) {
                IconButton(onClick = onNotificationClick) {
                    Badge(
                        modifier = Modifier.offset(x = (-8).dp, y = 8.dp)
                    ) {
                        Text("3")
                    }
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    isSearchActive = it.isNotBlank()
                },
                placeholder = {
                    Text(
                        text = "Search books, authors, categories...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                isSearchActive = false
                            }
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearchActive && searchQuery.isNotBlank()) {
            // Search Results
            if (filteredBooks.isEmpty()) {
                // No results found
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = "No results",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No books found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try searching for different books, authors, or categories",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Search results content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredBooks.size} books found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(filteredBooks) { book ->
                        SearchResultBookCard(
                            book = book,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
        } else {
            // Original home content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 140.dp // Increased from 100dp to properly clear bottom navigation
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            title = "Buy",
                            icon = Icons.Default.ShoppingCart,
                            color = BuyAccent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (displayTrendingBooks.isNotEmpty()) {
                                    onBookClick(displayTrendingBooks.first())
                                }
                            }
                        )
                        QuickActionCard(
                            title = "Sell",
                            icon = Icons.Default.AttachMoney,
                            color = SellAccent,
                            modifier = Modifier.weight(1f),
                            onClick = onSellClick
                        )
                        QuickActionCard(
                            title = "Rent",
                            icon = Icons.Default.FileDownload,
                            color = RentAccent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (academicBooks.isNotEmpty()) {
                                    onBookClick(academicBooks.first())
                                }
                            }
                        )
                    }
                }

                // Trending Books Section - Only show if has items
                if (displayTrendingBooks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Trending Books",
                            actionText = "See All",
                            onActionClick = { }
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(200.dp) // Fixed height to prevent layout issues
                        ) {
                            items(displayTrendingBooks) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { onBookClick(book) }
                                )
                            }
                        }
                    }
                }

                // Recommended Books Section - Only show if has items
                if (recommendedBooks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recommended For You",
                            actionText = "See All",
                            onActionClick = { }
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(200.dp) // Fixed height to prevent layout issues
                        ) {
                            items(recommendedBooks) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { onBookClick(book) }
                                )
                            }
                        }
                    }
                }

                // Academic Books Section - Only show if has items
                if (academicBooks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Rent Academic Books",
                            actionText = "Browse All",
                            onActionClick = { }
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(220.dp) // Slightly taller for academic cards
                        ) {
                            items(academicBooks) { book ->
                                AcademicBookCard(
                                    book = book,
                                    onClick = { onBookClick(book) }
                                )
                            }
                        }
                    }
                }

                // Nearby Books Section - Only show if has items
                if (nearbyBooks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Used Books Near You",
                            actionText = "View Map",
                            onActionClick = { }
                        )
                    }

                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            nearbyBooks.forEach { book ->
                                NearbyBookCard(
                                    book = book,
                                    onClick = { onBookClick(book) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = actionText,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Book cover placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "Book cover",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Safe title handling with fallback
            Text(
                text = book.title.takeIf { it.isNotBlank() } ?: "Unknown Title",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 2 // Ensure consistent height
            )

            // Safe author handling with fallback
            Text(
                text = book.author.takeIf { it.isNotBlank() } ?: "Unknown Author",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                minLines = 1 // Ensure consistent height
            )

            Spacer(modifier = Modifier.weight(1f))

            // Safe price handling with fallback
            Text(
                text = if (book.price > 0) "₹${book.price.toInt()}" else "Price not available",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AcademicBookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Book cover placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = RentAccent.copy(alpha = 0.2f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "Academic book",
                        tint = RentAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Safe title handling with fallback
            Text(
                text = book.title.takeIf { it.isNotBlank() } ?: "Unknown Title",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 2 // Ensure consistent height
            )

            // Safe author handling with fallback
            Text(
                text = book.author.takeIf { it.isNotBlank() } ?: "Unknown Author",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                minLines = 1 // Ensure consistent height
            )

            Spacer(modifier = Modifier.weight(1f))

            // Safe rental price handling with fallback
            Text(
                text = if (book.rentalPrice > 0) "Rent: ₹${book.rentalPrice.toInt()}/month" else "Rental price not available",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = RentAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NearbyBookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover placeholder
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "Book cover",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Safe title handling with fallback
                Text(
                    text = book.title.takeIf { it.isNotBlank() } ?: "Unknown Title",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Safe author handling with fallback
                Text(
                    text = book.author.takeIf { it.isNotBlank() } ?: "Unknown Author",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Safe condition handling
                Text(
                    text = "2.5 km away • ${book.condition.toString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                // Safe price handling with fallback
                Text(
                    text = if (book.price > 0) "₹${book.price.toInt()}" else "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Safe condition color handling
                Text(
                    text = book.condition.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (book.condition) {
                        BookCondition.NEW -> Color(0xFF4CAF50)
                        BookCondition.LIKE_NEW -> Color(0xFF8BC34A)
                        BookCondition.GOOD -> Color(0xFFFF9800)
                        BookCondition.FAIR -> Color(0xFFFF5722)
                        BookCondition.POOR -> Color(0xFF9E9E9E)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchResultBookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover placeholder
            Card(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = "Book cover",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Safe title handling with fallback
                Text(
                    text = book.title.takeIf { it.isNotBlank() } ?: "Unknown Title",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Safe author handling with fallback
                Text(
                    text = "by ${book.author.takeIf { it.isNotBlank() } ?: "Unknown Author"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Safe category handling with fallback
                Text(
                    text = book.category.takeIf { it.isNotBlank() } ?: "General",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                // Safe price handling with fallback
                Text(
                    text = if (book.price > 0) "₹${book.price.toInt()}" else "Price N/A",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Safe condition color handling
                Text(
                    text = book.condition.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (book.condition) {
                        BookCondition.NEW -> Color(0xFF4CAF50)
                        BookCondition.LIKE_NEW -> Color(0xFF8BC34A)
                        BookCondition.GOOD -> Color(0xFFFF9800)
                        BookCondition.FAIR -> Color(0xFFFF5722)
                        BookCondition.POOR -> Color(0xFF9E9E9E)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Sample data function
fun getSampleBooks(): List<Book> {
    return listOf(
        Book(
            id = "1",
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            coverImageUrl = "",
            price = 299.0,
            rentalPrice = 50.0,
            condition = BookCondition.GOOD,
            category = "Classic Literature",
            description = "A classic American novel",
            seller = User(
                id = "user1",
                name = "John Doe",
                email = "john@example.com",
                profileImageUrl = "",
                rating = 4.5f,
                location = "Delhi"
            ),
            rating = 4.2f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        ),
        Book(
            id = "2",
            title = "Data Structures and Algorithms",
            author = "Thomas Cormen",
            coverImageUrl = "",
            price = 899.0,
            rentalPrice = 150.0,
            condition = BookCondition.LIKE_NEW,
            category = "Academic",
            description = "Comprehensive guide to algorithms",
            seller = User(
                id = "user2",
                name = "Jane Smith",
                email = "jane@example.com",
                profileImageUrl = "",
                rating = 4.8f,
                location = "Mumbai"
            ),
            rating = 4.7f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        ),
        Book(
            id = "3",
            title = "Harry Potter and the Philosopher's Stone",
            author = "J.K. Rowling",
            coverImageUrl = "",
            price = 399.0,
            rentalPrice = 75.0,
            condition = BookCondition.NEW,
            category = "Fantasy",
            description = "First book in the Harry Potter series",
            seller = User(
                id = "user3",
                name = "Alice Johnson",
                email = "alice@example.com",
                profileImageUrl = "",
                rating = 4.3f,
                location = "Bangalore"
            ),
            rating = 4.9f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true
        ),
        Book(
            id = "4",
            title = "Operating System Concepts",
            author = "Abraham Silberschatz",
            coverImageUrl = "",
            price = 1299.0,
            rentalPrice = 200.0,
            condition = BookCondition.GOOD,
            category = "Academic",
            description = "Comprehensive guide to operating systems",
            seller = User(
                id = "user4",
                name = "Mike Johnson",
                email = "mike@example.com",
                profileImageUrl = "",
                rating = 4.6f,
                location = "Chennai"
            ),
            rating = 4.5f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        ),
        Book(
            id = "5",
            title = "Psychology of Money",
            author = "Morgan Housel",
            coverImageUrl = "",
            price = 450.0,
            rentalPrice = 80.0,
            condition = BookCondition.LIKE_NEW,
            category = "Finance",
            description = "Timeless lessons on wealth, greed, and happiness",
            seller = User(
                id = "user5",
                name = "Sarah Wilson",
                email = "sarah@example.com",
                profileImageUrl = "",
                rating = 4.7f,
                location = "Pune"
            ),
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        ),
        Book(
            id = "6",
            title = "Atomic Habits",
            author = "James Clear",
            coverImageUrl = "",
            price = 399.0,
            rentalPrice = 70.0,
            condition = BookCondition.NEW,
            category = "Self Help",
            description = "An easy and proven way to build good habits and break bad ones",
            seller = User(
                id = "user6",
                name = "David Brown",
                email = "david@example.com",
                profileImageUrl = "",
                rating = 4.4f,
                location = "Hyderabad"
            ),
            rating = 4.9f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true
        )
    )
}
