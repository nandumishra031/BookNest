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
import androidx.compose.ui.layout.ContentScale
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
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onSellClick: () -> Unit = {}
) {
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Use provided data or fallback to sample data if empty
    val displayBooks = if (books.isNotEmpty()) books else getSampleBooks()
    val displayTrendingBooks = if (trendingBooks.isNotEmpty()) trendingBooks else getSampleBooks().take(5)

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

    val recommendedBooks = filteredBooks.drop(2).take(4)
    val academicBooks = filteredBooks.filter { it.category == "Academic" }
    val nearbyBooks = filteredBooks.take(3)

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
                    contentPadding = PaddingValues(16.dp),
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
                            searchQuery = searchQuery,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            }
        } else {
            // Original home content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header with search and notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discover Books 📚",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

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

                item {
                    // Search Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSearchClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search books, authors, categories...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

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
                                // Navigate to trending books section or browse
                                onBookClick(displayTrendingBooks.first())
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
                                // Navigate to academic books or show rental options
                                if (academicBooks.isNotEmpty()) {
                                    onBookClick(academicBooks.first())
                                }
                            }
                        )
                    }
                }

                item {
                    // Trending Books Section
                    SectionHeader(
                        title = "Trending Books",
                        actionText = "See All",
                        onActionClick = { }
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(displayTrendingBooks) { book ->
                            BookCard(
                                book = book,
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }

                item {
                    // Recommended Section
                    SectionHeader(
                        title = "Recommended For You",
                        actionText = "See All",
                        onActionClick = { }
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(recommendedBooks) { book ->
                            BookCard(
                                book = book,
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }

                item {
                    // Academic Books Section
                    SectionHeader(
                        title = "Rent Academic Books",
                        actionText = "Browse All",
                        onActionClick = { }
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(academicBooks) { book ->
                            AcademicBookCard(
                                book = book,
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }

                item {
                    // Nearby Used Books
                    SectionHeader(
                        title = "Used Books Near You",
                        actionText = "View Map",
                        onActionClick = { }
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(80.dp),
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
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SectionHeader(
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onActionClick) {
            Text(actionText)
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Book Cover
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BookCover
                )
            ) {
                // Placeholder for AsyncImage
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "₹${book.price}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AcademicBookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = RentAccent.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.GetApp,
                    contentDescription = "Rent",
                    tint = RentAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${book.rentalPrice}/month",
                    style = MaterialTheme.typography.labelMedium,
                    color = RentAccent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PDF",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NearbyBookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover
            Card(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BookCover
                )
            ) {
                // Placeholder for AsyncImage
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "2.3 km away",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${book.price}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = book.condition.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchResultBookCard(
    book: Book,
    searchQuery: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover
            Card(
                modifier = Modifier.size(80.dp, 100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BookCover
                )
            ) {
                // Placeholder for AsyncImage
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Highlight search matches in title
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (book.title.contains(searchQuery, ignoreCase = true))
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )

                // Highlight search matches in author
                Text(
                    text = "by ${book.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (book.author.contains(searchQuery, ignoreCase = true))
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Category chip
                Card(
                    modifier = Modifier.padding(top = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (book.category.contains(searchQuery, ignoreCase = true))
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = book.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (book.category.contains(searchQuery, ignoreCase = true))
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Condition and rating
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.condition.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFFB000)
                    )
                    Text(
                        text = "${book.rating}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${book.price}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (book.isAvailableForRent) {
                    Text(
                        text = "Rent ₹${book.rentalPrice}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RentAccent
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (book.isAvailableForPurchase) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Buy",
                            modifier = Modifier.size(16.dp),
                            tint = BuyAccent
                        )
                    }
                    if (book.isAvailableForRent) {
                        Icon(
                            Icons.Default.GetApp,
                            contentDescription = "Rent",
                            modifier = Modifier.size(16.dp),
                            tint = RentAccent
                        )
                    }
                }
            }
        }
    }
}
// Sample data function
fun getSampleBooks(): List<Book> {
    val sampleUser = User(
        id = "1",
        name = "John Doe",
        email = "john@example.com",
        profileImageUrl = "",
        rating = 4.5f,
        location = "Mumbai"
    )

    return listOf(
        Book(
            id = "1",
            title = "The Psychology of Money",
            author = "Morgan Housel",
            coverImageUrl = "",
            price = 399.0,
            rentalPrice = 99.0,
            condition = BookCondition.NEW,
            category = "Finance",
            description = "Timeless lessons on wealth, greed, and happiness",
            seller = sampleUser,
            rating = 4.6f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = true
        ),
        Book(
            id = "2",
            title = "Data Structures and Algorithms",
            author = "Narasimha Karumanchi",
            coverImageUrl = "",
            price = 650.0,
            rentalPrice = 150.0,
            condition = BookCondition.GOOD,
            category = "Academic",
            description = "Complete guide to DSA",
            seller = sampleUser,
            rating = 4.8f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        ),
        Book(
            id = "3",
            title = "Atomic Habits",
            author = "James Clear",
            coverImageUrl = "",
            price = 450.0,
            rentalPrice = 120.0,
            condition = BookCondition.LIKE_NEW,
            category = "Self Help",
            description = "An easy & proven way to build good habits",
            seller = sampleUser,
            rating = 4.7f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        ),
        Book(
            id = "4",
            title = "Operating System Concepts",
            author = "Abraham Silberschatz",
            coverImageUrl = "",
            price = 800.0,
            rentalPrice = 200.0,
            condition = BookCondition.GOOD,
            category = "Academic",
            description = "Essential concepts in OS",
            seller = sampleUser,
            rating = 4.5f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        ),
        Book(
            id = "5",
            title = "The Alchemist",
            author = "Paulo Coelho",
            coverImageUrl = "",
            price = 299.0,
            rentalPrice = 89.0,
            condition = BookCondition.GOOD,
            category = "Fiction",
            description = "A magical story about following your dreams",
            seller = sampleUser,
            rating = 4.4f,
            isAvailableForRent = true,
            isAvailableForPurchase = true,
            isNewBook = false
        )
    )
}
