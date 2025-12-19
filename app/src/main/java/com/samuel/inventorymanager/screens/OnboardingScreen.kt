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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
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
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onSignInWithGoogle: () -> Unit
) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    // Total pages: 6 feature pages + 1 sign-in page
    val totalPages = 7
    val isLastPage = pagerState.currentPage == totalPages - 1

    // Auto-advance only on feature pages (not the sign-in page)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < totalPages - 1) {
            delay(5000)
            if (pagerState.currentPage < totalPages - 2) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
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
            // Animated App Header
            AnimatedAppHeader()

            // Horizontal Pager
            HorizontalPager(
                count = totalPages,
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        OnboardingPageData(
                            icon = Icons.Default.Inventory,
                            title = "Welcome to Android Inventory Pro",
                            description = "The ultimate solution for organizing everything you own. From garage tools to kitchen supplies, never lose track of anything again! Create a digital inventory with photos, locations, and detailed information.",
                            emoji = "📦",
                            features = listOf(
                                "📱 Works completely offline",
                                "🔒 Your data stays private",
                                "🎯 Simple and intuitive design"
                            )
                        )
                    )
                    1 -> OnboardingPage(
                        OnboardingPageData(
                            icon = Icons.Default.LocationOn,
                            title = "Smart 4-Level Location Hierarchy",
                            description = "Know exactly where everything is stored with our intuitive organization system:",
                            emoji = "🏠",
                            features = listOf(
                                "🏢 Garage: Main storage areas (Workshop, Kitchen, etc.)",
                                "🗄️ Cabinet: Containers within garages",
                                "📚 Shelf: Sections within cabinets",
                                "📦 Box: Individual storage boxes"
                            )
                        )
                    )
                    2 -> OnboardingPage(
                        OnboardingPageData(
                            icon = Icons.Default.CameraAlt,
                            title = "AI-Powered Item Recognition",
                            description = "Take a photo and let AI do the heavy lifting! Automatically detect item names, model numbers, and descriptions.",
                            emoji = "🤖",
                            features = listOf(
                                "📸 Camera integration for quick photos",
                                "🔍 OCR text extraction from images",
                                "🧠 AI identifies items automatically",
                                "⚡ Save hours of manual data entry"
                            )
                        )
                    )
                    3 -> OnboardingPage(
                        OnboardingPageData(
                            icon = Icons.Default.Search,
                            title = "Powerful Search & Filtering",
                            description = "Find any item in seconds with advanced search capabilities and smart filters.",
                            emoji = "🔍",
                            features = listOf(
                                "🎯 Instant item search",
                                "🏷️ Filter by location, condition, tags",
                                "📊 Overview dashboard with spreadsheet view",
                                "⚡ Your entire inventory at your fingertips"
                            )
                        )
                    )
                    4 -> OnboardingPage(
                        OnboardingPageData(
                            icon = Icons.Default.Edit,
                            title = "Smart Auto-Save & Custom Fields",
                            description = "The app works intelligently to make data entry effortless and flexible.",
                            emoji = "💾",
                            features = listOf(
                                "⚡ Auto-save after you stop typing",
                                "✏️ Add custom condition options",
                                "🎨 Create custom functionality states",
                                "📝 All changes saved automatically"
                            )
                        )
                    )
                    5 -> OnboardingPage(
                        OnboardingPageData(
                            icon = Icons.Default.Backup,
                            title = "Data Backup & Export",
                            description = "Never lose your inventory data with multiple backup options.",
                            emoji = "☁️",
                            features = listOf(
                                "💾 Export data as JSON files",
                                "📥 Import data from backups",
                                "🔄 Sync with desktop companion app",
                                "☁️ Google Drive backup (with sign-in)"
                            )
                        )
                    )
                    6 -> SignInPage(
                        onSignInWithGoogle = onSignInWithGoogle,
                        onContinueWithoutSignIn = onGetStarted
                    )
                }
            }

            // Page Indicators
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.padding(16.dp),
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.3f)
            )

            // Navigation Buttons
            BottomNavigationButtons(
                pagerState = pagerState,
                totalPages = totalPages,
                onSkip = {
                    scope.launch {
                        pagerState.animateScrollToPage(totalPages - 1)
                    }
                },
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
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
                Text("📦", fontSize = 48.sp)
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
    val emoji: String,
    val features: List<String> = emptyList()
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
                fontSize = 72.sp,
                modifier = Modifier.padding(bottom = 16.dp)
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
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Description
            Text(
                page.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Features List
            if (page.features.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        page.features.forEach { feature ->
                            Text(
                                feature,
                                fontSize = 14.sp,
                                color = Color.White,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun BottomNavigationButtons(
    pagerState: PagerState,
    totalPages: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val isLastPage = pagerState.currentPage == totalPages - 1

    if (!isLastPage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip Button
            TextButton(onClick = onSkip) {
                Text("Skip", color = Color.White, fontSize = 16.sp)
            }

            // Next Button
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A237E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(20.dp))
            }
        }
    } else {
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SignInPage(
    onSignInWithGoogle: () -> Unit,
    onContinueWithoutSignIn: () -> Unit
) {
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
            Text("🚀", fontSize = 72.sp)
            Spacer(Modifier.height(24.dp))

            Text(
                "You're All Set!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Ready to start organizing your inventory?",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Benefits Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Sign in with Google for:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text("☁️ Automatic cloud backup to Google Drive", fontSize = 14.sp, color = Color.White)
                    Text("🔄 Sync across multiple devices", fontSize = 14.sp, color = Color.White)
                    Text("🔒 Secure data protection", fontSize = 14.sp, color = Color.White)
                    Text("💾 Never lose your inventory", fontSize = 14.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Sign in Button
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

            Spacer(Modifier.height(16.dp))

            // Continue without sign in
            TextButton(
                onClick = onContinueWithoutSignIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Continue without signing in",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "You can always sign in later from Settings",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}