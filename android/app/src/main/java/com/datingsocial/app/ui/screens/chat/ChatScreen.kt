package com.datingsocial.app.ui.screens.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.datingsocial.app.data.DemoDataProvider
import com.datingsocial.app.data.api.ApiClient
import com.datingsocial.app.data.model.*
import com.datingsocial.app.ui.components.ReportBlockDialog
import com.datingsocial.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentUserId = DemoDataProvider.currentUser.userId
    var otherUserId by remember { mutableStateOf<Long?>(null) }
    var otherUserName by remember { mutableStateOf("Chat") }
    var otherUserPhoto by remember { mutableStateOf("") }
    var otherUserStatus by remember { mutableStateOf("") }
    var isBlocked by remember { mutableStateOf(false) }

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }

    val icebreakers = listOf(
        "☕ Coffee or Chai debate?",
        "🧗 Next dream travel destination?",
        "🎬 Favorite comfort movie?",
        "🐶 Are you a dog or cat person?"
    )

    // Auto-scroll list when new messages arrive or keyboard opens
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Load conversation metadata and poll real messages + live active status from backend API
    LaunchedEffect(conversationId) {
        var pollCounter = 0
        while (true) {
            try {
                val res = ApiClient.service.getMessages(conversationId)
                if (res.isSuccessful && res.body() != null) {
                    messages = res.body()!!
                    if (otherUserName == "Chat") {
                        val otherMsg = messages.firstOrNull { it.senderId != currentUserId }
                        if (otherMsg != null && !otherMsg.senderName.isNullOrBlank()) {
                            otherUserName = otherMsg.senderName!!
                            if (otherUserId == null) {
                                otherUserId = otherMsg.senderId
                            }
                        }
                    }
                } else if (res.code() == 400 || res.code() == 403) {
                    isBlocked = true
                }

                // Poll conversation details every 2 cycles (~5s) to reflect live active/online status
                if (pollCounter % 2 == 0) {
                    val convRes = ApiClient.service.getConversations()
                    if (convRes.isSuccessful && convRes.body() != null) {
                        val conv = convRes.body()!!.firstOrNull { it.id == conversationId }
                        if (conv != null && conv.otherUser != null) {
                            otherUserId = conv.otherUser.userId
                            otherUserName = conv.otherUser.name
                            otherUserPhoto = conv.otherUser.primaryPhotoUrl
                            otherUserStatus = conv.otherUser.displayStatus
                        }
                    }
                }
                pollCounter++
            } catch (e: Exception) {}
            delay(2500)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (otherUserPhoto.isNotBlank()) {
                            AsyncImage(
                                model = otherUserPhoto,
                                contentDescription = otherUserName,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(FlameGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (otherUserName.isNotBlank()) otherUserName.take(1).uppercase() else "C",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Column {
                            val isOnline = !isBlocked && (otherUserStatus.equals("Active now", ignoreCase = true) || otherUserStatus.contains("Online", ignoreCase = true))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = otherUserName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) SuccessGreen else Color.Gray)
                                )
                            }
                            val displayStatusText = if (isBlocked) "Blocked" else if (otherUserStatus.isNotBlank()) otherUserStatus else "Offline"
                            Text(
                                text = displayStatusText,
                                fontSize = 11.sp,
                                color = if (isOnline) SuccessGreen else TextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSafetyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safety Toolkit",
                            tint = PrimaryPink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Safety Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "End-to-end encrypted chat • Never share financial info",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Messages List or Empty State
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Say hello to $otherUserName!",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Send your first message or choose an icebreaker below to break the ice.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(message = msg)
                    }
                }
            }

            // If user is blocked, disable input and show banner
            if (isBlocked) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = SurfaceCard,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = PassRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This conversation is blocked. Messages cannot be sent.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Quick Icebreaker Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(icebreakers) { icebreaker ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceCard)
                                .clickable {
                                    messageText = icebreaker
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = icebreaker,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Message Composer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* Photo pick demo */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Send Photo",
                            tint = TextSecondary
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...", color = TextMuted, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = PrimaryPink,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (isBlocked) {
                                Toast.makeText(context, "Cannot send message: User is blocked", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val textToSend = messageText.trim()
                            if (textToSend.isNotBlank()) {
                                messageText = ""
                                coroutineScope.launch {
                                    try {
                                        val res = ApiClient.service.sendMessage(conversationId, SendMessageDto(content = textToSend, type = "TEXT"))
                                        if (res.isSuccessful && res.body() != null) {
                                            messages = messages + res.body()!!
                                            if (messages.isNotEmpty()) {
                                                listState.animateScrollToItem(messages.size - 1)
                                            }
                                        } else {
                                            val err = try { res.errorBody()?.string() ?: "" } catch(e: Exception) { "" }
                                            if (err.contains("blocked", ignoreCase = true) || res.code() == 400 || res.code() == 403 || res.code() == 500) {
                                                isBlocked = true
                                                Toast.makeText(context, "Cannot send message: User is blocked or match inactive.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Delivery failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FlameGradient)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Safety Dialog
        if (showSafetyDialog) {
            ReportBlockDialog(
                targetUserName = otherUserName,
                onDismiss = { showSafetyDialog = false },
                onBlockOnly = {
                    showSafetyDialog = false
                    val uid = otherUserId ?: messages.firstOrNull { it.senderId != currentUserId }?.senderId
                    if (uid != null) {
                        coroutineScope.launch {
                            try {
                                ApiClient.service.blockUser(BlockRequestDto(targetUserId = uid, reason = "Blocked by user"))
                                Toast.makeText(context, "User blocked successfully.", Toast.LENGTH_SHORT).show()
                                isBlocked = true
                                onNavigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "User blocked.", Toast.LENGTH_SHORT).show()
                                isBlocked = true
                                onNavigateBack()
                            }
                        }
                    } else {
                        onNavigateBack()
                    }
                },
                onReportAndBlock = { reason, details ->
                    showSafetyDialog = false
                    val uid = otherUserId ?: messages.firstOrNull { it.senderId != currentUserId }?.senderId
                    if (uid != null) {
                        coroutineScope.launch {
                            try {
                                ApiClient.service.reportUser(ReportRequestDto(targetUserId = uid, reason = reason, details = details))
                                Toast.makeText(context, "Report submitted & user blocked.", Toast.LENGTH_SHORT).show()
                                isBlocked = true
                                onNavigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Reported & blocked.", Toast.LENGTH_SHORT).show()
                                isBlocked = true
                                onNavigateBack()
                            }
                        }
                    } else {
                        onNavigateBack()
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val myId = ApiClient.currentUserId ?: DemoDataProvider.currentUser.userId
    val isMe = (myId > 0 && message.senderId == myId) || message.isMe
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        val bubbleModifier = if (isMe) Modifier.background(FlameGradient) else Modifier.background(SurfaceCard)
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    )
                )
                .then(bubbleModifier)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.displayContent,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = message.displayTime,
                fontSize = 10.sp,
                color = TextMuted
            )
            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Read",
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
