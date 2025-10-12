package com.booknest.app.ui.books

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
import coil.compose.AsyncImage
import com.booknest.app.data.Book
import com.booknest.app.data.BookCondition
import com.booknest.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBooksScreen(
    books: List<Book>,
    category: String? = null,
    onBackClick: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Title") }
    var showFilters by remember { mutableStateOf(false) }

    // Filter and sort books
    val filteredBooks = remember(books, searchQuery, selectedFilter, sortBy) {
        var filtered = books

        // Apply category filter if specified
        if (!category.isNullOrBlank() && category != "All") {
            filtered = filtered.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Apply search filter
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                book.author.contains(searchQuery, ignoreCase = true) ||
                book.category.contains(searchQuery, ignoreCase = true) ||
                        book.id.contains(searchQuery, ignoreCase = true)
            }
        }

        // Apply condition filter
        when (selectedFilter) {
            "New" -> filtered = filtered.filter { it.condition == BookCondition.NEW }
            "Like New" -> filtered = filtered.filter { it.condition == BookCondition.LIKE_NEW }
            "Good" -> filtered = filtered.filter { it.condition == BookCondition.GOOD }
            "Fair" -> filtered = filtered.filter { it.condition == BookCondition.FAIR }
            "Academic" -> filtered = filtered.filter { it.category == "Academic" }
            "Fiction" -> filtered = filtered.filter { it.category == "Fiction" }
            "Non-Fiction" -> filtered = filtered.filter { it.category == "Non-Fiction" }
            "Under ₹500" -> filtered = filtered.filter { it.price < 500 }
            "Under ₹1000" -> filtered = filtered.filter { it.price < 1000 }
        }

        // Apply sorting
        when (sortBy) {
            "Title" -> filtered.sortedBy { it.title }
            "Price: Low to High" -> filtered.sortedBy { it.price }
            "Price: High to Low" -> filtered.sortedByDescending { it.price }
            "Author" -> filtered.sortedBy { it.author }
            "Condition" -> filtered.sortedByDescending { it.condition.ordinal }
            "Category" -> filtered.sortedBy { it.category }
            "id" -> filtered.sortedBy { it.id }
            else -> filtered
        }
    }

    val screenTitle = when {
        !category.isNullOrBlank() && category != "All" -> "$category Books"
        category == "Academic" -> "Academic Books"
        category == "Trending" -> "Trending Books"
        category == "Recommended" -> "Recommended Books"
        category == "Nearby" -> "Books Near You"
        else -> "All Books"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = screenTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Filters"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Search Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search books...",
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
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
        }

        // Filters and Sort
        if (showFilters) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Filter by:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter chips
                    val filters = listOf("All", "New", "Like New", "Good", "Fair", "Academic", "Fiction", "Non-Fiction", "Under ₹500", "Under ₹1000")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filters) { filter ->
                            FilterChip(
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                selected = selectedFilter == filter
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sort by:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort chips
                    val sortOptions = listOf("Title", "Price: Low to High", "Price: High to Low", "Author", "Condition", "Category", "id")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortOptions) { sort ->
                            FilterChip(
                                onClick = { sortBy = sort },
                                label = { Text(sort) },
                                selected = sortBy == sort
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Results count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredBooks.size} books found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (searchQuery.isNotBlank() || selectedFilter != "All") {
                TextButton(
                    onClick = {
                        searchQuery = ""
                        selectedFilter = "All"
                    }
                ) {
                    Text("Clear filters")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Books List
        if (filteredBooks.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = "No books found",
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
                    text = "Try adjusting your search or filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBooks) { book ->
                    AllBooksBookCard(
                        book = book,
                        onClick = { onBookClick(book) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AllBooksBookCard(
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
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Book Image
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Book Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "by ${book.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (book.condition) {
                                    BookCondition.NEW -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    BookCondition.LIKE_NEW -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                    BookCondition.GOOD -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                    else -> Color(0xFFF44336).copy(alpha = 0.1f)
                                }
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = book.condition.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (book.condition) {
                                    BookCondition.NEW -> Color(0xFF4CAF50)
                                    BookCondition.LIKE_NEW -> Color(0xFF2196F3)
                                    BookCondition.GOOD -> Color(0xFFFF9800)
                                    else -> Color(0xFFF44336)
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = book.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Price and Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${book.price}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View Details")
                    }
                }
            }
        }
    }
}
