package com.samuel.inventorymanager.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onSignInWithGoogle: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    // Auto-advance pages
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // 5 seconds per page
            val nextPage = (pagerState.currentPage + 1) % 5
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1),
                        Color(0xFF01579B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Logo & Title
            AnimatedAppHeader()

            // Horizontal Pager for feature showcase
            HorizontalPager(
                count = 5,
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPage(
                    page = when (page) {
                        0 -> OnboardingPageData(
                            icon = Icons.Default.Inventory,
                            title = "Welcome to Android Inventory Pro",
                            description = "The ultimate solution for organizing everything you own. From your garage tools to your kitchen supplies, never lose track of anything again!",
                            emoji = "📦"
                        )
                        1 -> OnboardingPageData(
                            icon = Icons.Default.LocationOn,
                            title = "Smart Location Hierarchy",
                            description = "Create Garages → Cabinets → Shelves → Boxes. Know exactly where every item is stored with our intuitive 4-level organization system.",
                            emoji = "🏠"
                        )
                        2 -> OnboardingPageData(
                            icon = Icons.Default.CameraAlt,
                            title = "AI-Powered Recognition",
                            description = "Take a photo and let our AI do the work! Automatically detect item names, model numbers, and descriptions. Save hours of manual data entry.",
                            emoji = "🤖"
                        )
                        3 -> OnboardingPageData(
                            icon = Icons.Default.Search,
                            title = "Instant Search & Filters",
                            description = "Find any item in seconds with powerful search. Filter by location, condition, tags, and more. Your entire inventory at your fingertips!",
                            emoji = "🔍"
                        )
                        else -> OnboardingPageData(
                            icon = Icons.Default.Cloud,
                            title = "Secure Cloud Backup",
                            description = "Never lose your data! Sign in with Google to automatically backup your inventory to Google Drive. Sync across all your devices seamlessly.",
                            emoji = "☁️"
                        )
                    }
                )
            }

            // Page Indicators
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.padding(16.dp),
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.3f)
            )

            // Bottom Action Section
            BottomActionSection(
                onSignInWithGoogle = onSignInWithGoogle,
                onContinueWithoutSignIn = onGetStarted
            )
        }
    }
}

@Composable
private fun AnimatedAppHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "header")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon Placeholder (you can replace with actual drawable)
        Surface(
            modifier = Modifier
                .size(100.dp)
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📦",
                    fontSize = 48.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Android Inventory Pro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            "Organize Everything, Find Anything",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val emoji: String
)

@Composable
private fun OnboardingPage(page: OnboardingPageData) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 2 }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large Emoji
            Text(
                page.emoji,
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Icon
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 24.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }

            // Title
            Text(
                page.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Description
            Text(
                page.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun BottomActionSection(
    onSignInWithGoogle: () -> Unit,
    onContinueWithoutSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sign in with Google Button
        Button(
            onClick = onSignInWithGoogle,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1A237E)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Google Icon (simplified)
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Sign in with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Continue without sign in
        TextButton(
            onClick = onContinueWithoutSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Continue without signing in",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Benefits text
        Text(
            "Sign in to enable cloud backup and sync",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// Feature Highlights Composable (Alternative detailed view)
// ==========================================

@Composable
fun FeatureHighlightsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Key Features",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FeatureCard(
            icon = Icons.Default.Inventory2,
            title = "4-Level Organization",
            description = "Garage → Cabinet → Shelf → Box hierarchy"
        )

        FeatureCard(
            icon = Icons.Default.Psychology,
            title = "AI Recognition",
            description = "Auto-detect items from photos with ML Kit"
        )

        FeatureCard(
            icon = Icons.Default.QrCode,
            title = "Barcode & OCR",
            description = "Scan barcodes and extract text from images"
        )

        FeatureCard(
            icon = Icons.Default.Backup,
            title = "Google Drive Backup",
            description = "Automatic cloud backup with your Google account"
        )

        FeatureCard(
            icon = Icons.Default.Dashboard,
            title = "Overview Dashboard",
            description = "Complete spreadsheet view of all items"
        )

        FeatureCard(
            icon = Icons.Default.FilterAlt,
            title = "Advanced Filters",
            description = "Filter by location, condition, tags, and more"
        )
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    description,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}