package com.datingsocial.app.ui.screens.matches

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.datingsocial.app.data.DemoDataProvider
import com.datingsocial.app.data.api.ApiClient
import com.datingsocial.app.data.model.ConversationItem
import com.datingsocial.app.data.model.LikeNotificationDto
import com.datingsocial.app.data.model.MatchItem
import com.datingsocial.app.data.model.SwipeRequestDto
import com.datingsocial.app.data.model.UserStoryGroup
import com.datingsocial.app.ui.components.AddStoryDialog
import com.datingsocial.app.ui.components.StoriesTray
import com.datingsocial.app.ui.components.StoryViewerDialog
import com.datingsocial.app.ui.theme.*
import com.datingsocial.app.util.ImageUtils
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun MatchesScreen(
    onNavigateToChat: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<MatchItem>>(emptyList()) }
    var conversations by remember { mutableStateOf<List<ConversationItem>>(emptyList()) }
    var likesReceived by remember { mutableStateOf<List<LikeNotificationDto>>(emptyList()) }
    var previousLikeCount by remember { mutableIntStateOf(-1) }
    var userStoryGroups by remember { mutableStateOf<List<UserStoryGroup>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Story Creation State
    var selectedStoryPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isPostingStory by remember { mutableStateOf(false) }
    var activeViewerGroupIndex by remember { mutableStateOf<Int?>(null) }

    val storyPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedStoryPhotoUri = uri
    }

    suspend fun refreshMatchesAndChats() {
        isRefreshing = true
        try {
            val matchesRes = ApiClient.service.getMatches()
            if (matchesRes.isSuccessful && matchesRes.body() != null) {
                matches = matchesRes.body()!!
            }
        } catch (e: Exception) {}

        try {
            val convRes = ApiClient.service.getConversations()
            if (convRes.isSuccessful && convRes.body() != null) {
                conversations = convRes.body()!!
            }
        } catch (e: Exception) {}

        try {
            val likesRes = ApiClient.service.getLikesReceived()
            if (likesRes.isSuccessful && likesRes.body() != null) {
                val newLikes = likesRes.body()!!
                if (previousLikeCount != -1 && newLikes.size > previousLikeCount) {
                    val latestLiker = newLikes.firstOrNull()?.displayName ?: "Someone"
                    Toast.makeText(context, "❤️ $latestLiker liked your profile!", Toast.LENGTH_LONG).show()
                }
                previousLikeCount = newLikes.size
                likesReceived = newLikes
            }
        } catch (e: Exception) {}

        try {
            val storiesRes = ApiClient.service.getStoriesTray()
            if (storiesRes.isSuccessful && storiesRes.body() != null) {
                userStoryGroups = storiesRes.body()!!
            }
        } catch (e: Exception) {}

        isRefreshing = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshMatchesAndChats()
            kotlinx.coroutines.delay(4000)
        }
    }

    val filteredConversations = remember(searchQuery, conversations) {
        if (searchQuery.isBlank()) conversations
        else conversations.filter {
            (it.otherUser?.name?.contains(searchQuery, ignoreCase = true) == true) ||
            it.displayLastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen Header with Refresh
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Matches & Messages",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${likesReceived.size} likes • ${matches.size} matches • ${conversations.size} active chats",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { coroutineScope.launch { refreshMatchesAndChats() } },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryPink,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search matches & chats...", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            // Stories Tray (Moments)
            item {
                StoriesTray(
                    userStoryGroups = userStoryGroups,
                    currentUserAvatarUrl = DemoDataProvider.currentUser.primaryPhotoUrl,
                    onAddStoryClick = { storyPhotoPickerLauncher.launch("image/*") },
                    onStoryGroupClick = { groupIndex -> activeViewerGroupIndex = groupIndex },
                    modifier = Modifier.padding(top = 18.dp, bottom = 2.dp)
                )
            }

            // Likes Received Section (Who Liked You) - only show when there are active incoming likes!
            if (likesReceived.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "❤️ Likes Received (${likesReceived.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryPink, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Dismiss All",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            val currentList = likesReceived
                                            likesReceived = emptyList()
                                            currentList.forEach { lk ->
                                                try {
                                                    ApiClient.service.swipe(
                                                        SwipeRequestDto(targetUserId = lk.likerUserId, isLike = false)
                                                    )
                                                } catch (e: Exception) {}
                                            }
                                            refreshMatchesAndChats()
                                        }
                                    }
                                )
                                Text(
                                    text = "Tap to Match",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            items(likesReceived, key = { it.likerUserId }) { like ->
                                val likerName = like.displayName
                                val photoUrl = like.fullPhotoUrl

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(115.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(SurfaceCard)
                                        .border(
                                            1.5.dp,
                                            Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057))),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = likerName,
                                            modifier = Modifier
                                                .size(62.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, PrimaryPink, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryPink)
                                                .border(1.5.dp, BackgroundDark, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = likerName.split(" ").firstOrNull() ?: likerName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Liked you 💕",
                                        color = NeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Pass / Dismiss Button
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        likesReceived = likesReceived.filter { it.likerUserId != like.likerUserId }
                                                        ApiClient.service.swipe(
                                                            SwipeRequestDto(targetUserId = like.likerUserId, isLike = false)
                                                        )
                                                        Toast.makeText(context, "Passed on $likerName", Toast.LENGTH_SHORT).show()
                                                        refreshMatchesAndChats()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error updating", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.08f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Pass",
                                                tint = PassRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        // Quick Match Button
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        likesReceived = likesReceived.filter { it.likerUserId != like.likerUserId }
                                                        val res = ApiClient.service.swipe(
                                                            SwipeRequestDto(targetUserId = like.likerUserId, isLike = true)
                                                        )
                                                        if (res.isSuccessful && res.body()?.isMatch == true) {
                                                            Toast.makeText(
                                                                context,
                                                                "🎉 It's a Match with $likerName!",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            val convoId = res.body()?.conversationId
                                                            refreshMatchesAndChats()
                                                            if (convoId != null) {
                                                                onNavigateToChat(convoId)
                                                            }
                                                        } else if (res.isSuccessful) {
                                                            Toast.makeText(context, "Liked back! 💕", Toast.LENGTH_SHORT).show()
                                                            refreshMatchesAndChats()
                                                        } else {
                                                            Toast.makeText(context, "Error matching (${res.code()})", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                        ) {
                                            Text(
                                                text = "Match",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // New Matches Section
            item {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    Text(
                        text = "New Matches (${matches.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    if (matches.isEmpty()) {
                        Text(
                            text = "No matches yet. When you and your friend swipe right on each other, they will appear here!",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(matches) { match ->
                                val matchedUser = match.matchedUser
                                val mName = matchedUser?.name ?: "User"
                                val photoUrl = matchedUser?.primaryPhotoUrl ?: ""
                                val score = matchedUser?.compatibilityScore ?: 85
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable {
                                        val convoId = match.conversationId ?: 1L
                                        onNavigateToChat(convoId)
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(CircleShape)
                                                .background(FlameGradient)
                                                .padding(3.dp)
                                        ) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = mName,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .offset(y = 4.dp)
                                                .background(PrimaryPurple, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$score%",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = mName.split(" ").firstOrNull() ?: mName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Messages Section Title
            item {
                Text(
                    text = "Conversations",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // Conversations List
            if (filteredConversations.isEmpty()) {
                item {
                    Text(
                        text = "No messages yet. Send a message to your match to start chatting!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }

            items(filteredConversations) { convo ->
                val other = convo.otherUser
                val otherName = other?.name ?: "User"
                val photoUrl = other?.primaryPhotoUrl ?: ""
                val isOnline = other?.displayStatus?.contains("Active", ignoreCase = true) == true ||
                               other?.displayStatus?.contains("Online", ignoreCase = true) == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToChat(convo.id) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = otherName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) SuccessGreen else Color.Gray)
                                .border(2.dp, BackgroundDark, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = otherName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = convo.displayTime,
                                fontSize = 12.sp,
                                color = if (convo.unreadCount > 0) PrimaryPink else TextMuted
                            )
                        }

                        Text(
                            text = convo.displayLastMessage,
                            fontSize = 14.sp,
                            color = if (convo.unreadCount > 0) TextPrimary else TextSecondary,
                            fontWeight = if (convo.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (convo.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(PrimaryPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = convo.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Story Preview & Caption Dialog
    if (selectedStoryPhotoUri != null) {
        AddStoryDialog(
            photoUri = selectedStoryPhotoUri!!,
            isPosting = isPostingStory,
            onDismiss = { selectedStoryPhotoUri = null },
            onPostStory = { caption ->
                isPostingStory = true
                coroutineScope.launch {
                    try {
                        val part = ImageUtils.preparePhotoPart(context, selectedStoryPhotoUri!!)
                        if (part != null) {
                            val captionPart = if (caption.isNotBlank()) {
                                caption.toRequestBody("text/plain".toMediaTypeOrNull())
                            } else null

                            val res = ApiClient.service.uploadStory(part, captionPart)
                            if (res.isSuccessful && res.body() != null) {
                                Toast.makeText(context, "Story shared!", Toast.LENGTH_SHORT).show()
                                selectedStoryPhotoUri = null
                                refreshMatchesAndChats()
                            } else {
                                Toast.makeText(context, "Failed to share story", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to process photo", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error sharing story", Toast.LENGTH_SHORT).show()
                    } finally {
                        isPostingStory = false
                    }
                }
            }
        )
    }

    // Full Screen Story Viewer
    if (activeViewerGroupIndex != null && activeViewerGroupIndex!! < userStoryGroups.size) {
        StoryViewerDialog(
            userGroups = userStoryGroups,
            initialGroupIndex = activeViewerGroupIndex!!,
            onDismiss = { activeViewerGroupIndex = null },
            onDeleteStory = { storyId ->
                coroutineScope.launch {
                    try {
                        val res = ApiClient.service.deleteStory(storyId)
                        if (res.isSuccessful) {
                            Toast.makeText(context, "Story deleted", Toast.LENGTH_SHORT).show()
                            refreshMatchesAndChats()
                        }
                    } catch (e: Exception) {}
                }
            }
        )
    }
}
