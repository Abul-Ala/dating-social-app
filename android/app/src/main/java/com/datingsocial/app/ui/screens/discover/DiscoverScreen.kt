package com.datingsocial.app.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datingsocial.app.data.DemoDataProvider
import com.datingsocial.app.data.api.ApiClient
import com.datingsocial.app.data.model.*
import com.datingsocial.app.ui.components.ActionButtons
import com.datingsocial.app.ui.components.MatchDialog
import com.datingsocial.app.ui.components.SwipeableCard
import com.datingsocial.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToChat: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var matchedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var matchedConversationId by remember { mutableStateOf<Long?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Filter states
    var ageRange by remember { mutableStateOf(18f..60f) }
    var maxDistance by remember { mutableFloatStateOf(100f) }
    var selectedGender by remember { mutableStateOf("ALL") }

    // Function to load live discovery candidates from backend API
    fun loadCandidates() {
        coroutineScope.launch {
            isLoading = true
            try {
                val res = ApiClient.service.getDiscoveryFeed()
                if (res.isSuccessful && res.body() != null) {
                    profiles = res.body()!!
                } else {
                    profiles = emptyList()
                }
            } catch (e: Exception) {
                profiles = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(DemoDataProvider.currentUser.userId) {
        loadCandidates()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AURA",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryPink,
                    letterSpacing = 2.sp
                )

                // Refresh / Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceCard)
                        .clickable { loadCandidates() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Refresh Feed",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Filter Button
                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceCard, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Card Deck Stack Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = PrimaryPink, modifier = Modifier.size(40.dp))
                } else if (profiles.isNotEmpty()) {
                    profiles.take(3).reversed().forEach { profile ->
                        val isTopCard = profile == profiles.first()
                        val topPadding = if (isTopCard) 0.dp else 12.dp

                        SwipeableCard(
                            profile = profile,
                            isTopCard = isTopCard,
                            onSwipeRight = {
                                val current = profile
                                profiles = profiles.filter { it.userId != current.userId }
                                if (profiles.isEmpty()) {
                                    loadCandidates()
                                }
                                coroutineScope.launch {
                                    try {
                                        val res = ApiClient.service.swipe(SwipeRequestDto(targetUserId = current.userId, isLike = true))
                                        if (res.isSuccessful && res.body()?.isMatch == true) {
                                            matchedProfile = res.body()?.matchedProfile ?: current
                                            matchedConversationId = res.body()?.conversationId
                                        } else if (res.isSuccessful) {
                                            Toast.makeText(context, "Liked ${current.name}! 💕 When they like you back, you'll match!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Could not record like (${res.code()})", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Network error recording like", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSwipeLeft = {
                                val current = profile
                                profiles = profiles.filter { it.userId != current.userId }
                                if (profiles.isEmpty()) {
                                    loadCandidates()
                                }
                                coroutineScope.launch {
                                    try {
                                        ApiClient.service.swipe(SwipeRequestDto(targetUserId = current.userId, isLike = false))
                                    } catch (e: Exception) {}
                                }
                            },
                            onCardClick = {
                                showDetailSheet = profile
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topPadding)
                        )
                    }
                } else {
                    // Empty State
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(RomanticGradient, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Text(
                            text = "You're All Caught Up!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "When your friend registers on their phone, tap 'Refresh Feed' to discover and match with them!",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { loadCandidates() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh Discovery Feed", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom Action Buttons
            ActionButtons(
                onRewind = { loadCandidates() },
                onPass = {
                    if (profiles.isNotEmpty()) {
                        val current = profiles.first()
                        profiles = profiles.drop(1)
                        if (profiles.isEmpty()) loadCandidates()
                        coroutineScope.launch {
                            try { ApiClient.service.swipe(SwipeRequestDto(targetUserId = current.userId, isLike = false)) } catch (e: Exception) {}
                        }
                    }
                },
                onSuperLike = {
                    if (profiles.isNotEmpty()) {
                        val current = profiles.first()
                        profiles = profiles.drop(1)
                        if (profiles.isEmpty()) loadCandidates()
                        coroutineScope.launch {
                            try {
                                val res = ApiClient.service.swipe(SwipeRequestDto(targetUserId = current.userId, isLike = true))
                                if (res.isSuccessful && res.body()?.isMatch == true) {
                                    matchedProfile = res.body()?.matchedProfile ?: current
                                    matchedConversationId = res.body()?.conversationId
                                }
                            } catch (e: Exception) {}
                        }
                    }
                },
                onLike = {
                    if (profiles.isNotEmpty()) {
                        val current = profiles.first()
                        profiles = profiles.drop(1)
                        if (profiles.isEmpty()) loadCandidates()
                        coroutineScope.launch {
                            try {
                                val res = ApiClient.service.swipe(SwipeRequestDto(targetUserId = current.userId, isLike = true))
                                if (res.isSuccessful && res.body()?.isMatch == true) {
                                    matchedProfile = res.body()?.matchedProfile ?: current
                                    matchedConversationId = res.body()?.conversationId
                                } else if (res.isSuccessful) {
                                    Toast.makeText(context, "Liked ${current.name}! 💕 When they like you back, you'll match!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Could not record like (${res.code()})", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network error recording like", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }

        // Celebration Match Dialog
        matchedProfile?.let { match ->
            MatchDialog(
                matchedProfile = match,
                currentUserProfile = DemoDataProvider.currentUser,
                onSendMessage = {
                    val targetConvoId = matchedConversationId ?: 1L
                    matchedProfile = null
                    onNavigateToChat(targetConvoId)
                },
                onKeepSwiping = {
                    matchedProfile = null
                }
            )
        }

        // Filter Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discovery Filters",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(onClick = {
                            showFilterSheet = false
                            loadCandidates()
                        }) {
                            Text("Apply", color = PrimaryPink, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Age Range
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Age Range", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${ageRange.start.toInt()} - ${ageRange.endInclusive.toInt()}", color = PrimaryPink, fontWeight = FontWeight.Bold)
                        }
                        RangeSlider(
                            value = ageRange,
                            onValueChange = { ageRange = it },
                            valueRange = 18f..65f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryPink, activeTrackColor = PrimaryPink)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
