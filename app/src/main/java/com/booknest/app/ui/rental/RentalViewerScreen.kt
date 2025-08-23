package com.booknest.app.ui.rental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.booknest.app.data.Book
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalViewerScreen(
    book: Book,
    userEmail: String,
    remainingDays: Int,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    var fontSizeValue by remember { mutableStateOf(16f) }
    var currentPage by remember { mutableStateOf(1) }
    val totalPages = 320 // Sample page count
    val progress = (currentPage.toFloat() / totalPages * 100).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with Reading Controls
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "Page $currentPage of $totalPages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onBookmarkClick) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Bookmark")
                }
                IconButton(onClick = { /* Search in book */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { /* More options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Reading Progress Bar
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Rental Info Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$remainingDays days remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Rental",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Reading Content Area
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Chapter Title
                    Text(
                        text = "Chapter 1: Introduction",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    // Book Content with Watermark
                    Box {
                        Text(
                            text = getSampleBookContent(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSizeValue.sp,
                                lineHeight = fontSizeValue.sp * 1.5f
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Justify
                        )

                        // Watermark
                        Text(
                            text = "BookNest Rental - $userEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp)
                        )
                    }
                }
            }

            // Reading Controls FAB
            FloatingActionButton(
                onClick = { /* Show reading controls */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Reading settings"
                )
            }
        }

        // Bottom Navigation Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Page
                IconButton(
                    onClick = {
                        if (currentPage > 1) currentPage--
                    },
                    enabled = currentPage > 1
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous page")
                }

                // Font Size Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (fontSizeValue > 12f) fontSizeValue -= 2f
                        }
                    ) {
                        Text("A-", style = MaterialTheme.typography.bodySmall)
                    }

                    Text(
                        text = "${fontSizeValue.toInt()}sp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            if (fontSizeValue < 24f) fontSizeValue += 2f
                        }
                    ) {
                        Text("A+", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Next Page
                IconButton(
                    onClick = {
                        if (currentPage < totalPages) currentPage++
                    },
                    enabled = currentPage < totalPages
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next page")
                }
            }
        }
    }
}

@Composable
fun RentalExpiredScreen(
    book: Book,
    onExtendRental: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccessTime,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Rental Expired",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your rental period for \"${book.title}\" has ended.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onExtendRental,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Extend Rental")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back to Library")
        }
    }
}

fun getSampleBookContent(): String {
    return """
        Welcome to this comprehensive guide on modern software development practices. In today's rapidly evolving technological landscape, the ability to adapt and learn new methodologies is crucial for any developer or technology professional.

        This book aims to provide you with practical insights and real-world examples that you can immediately apply to your projects. We'll explore various aspects of software development, from planning and design to implementation and deployment.

        Throughout this journey, you'll discover how to leverage modern tools and frameworks to build robust, scalable applications. We'll also discuss best practices for code organization, testing strategies, and performance optimization.

        The software development industry has undergone tremendous changes in recent years. The rise of cloud computing, microservices architecture, and DevOps practices has fundamentally altered how we approach building and delivering software solutions.

        As we progress through the chapters, you'll gain a deeper understanding of these concepts and learn how to integrate them into your development workflow. Each chapter builds upon the previous one, creating a comprehensive learning experience.

        Remember, software development is not just about writing code—it's about solving problems, creating value for users, and continuously improving your craft. The principles and practices outlined in this book will serve as your foundation for building exceptional software products.

        Let's begin this exciting journey together and unlock the full potential of modern software development.
    """.trimIndent()
}
