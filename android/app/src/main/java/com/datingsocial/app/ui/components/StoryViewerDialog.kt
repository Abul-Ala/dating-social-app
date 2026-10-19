package com.datingsocial.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.datingsocial.app.data.model.UserStoryGroup
import com.datingsocial.app.ui.theme.*
import kotlinx.coroutines.isActive

@Composable
fun StoryViewerDialog(
    userGroups: List<UserStoryGroup>,
    initialGroupIndex: Int = 0,
    onDismiss: () -> Unit,
    onDeleteStory: (Long) -> Unit
) {
    if (userGroups.isEmpty()) {
        onDismiss()
        return
    }

    var groupIndex by remember { mutableStateOf(initialGroupIndex.coerceIn(0, userGroups.size - 1)) }
    val currentGroup = userGroups.getOrNull(groupIndex)
    if (currentGroup == null || currentGroup.stories.isEmpty()) {
        onDismiss()
        return
    }

    var storyIndex by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    val stories = currentGroup.stories
    val currentStory = stories.getOrNull(storyIndex) ?: stories.first()

    fun moveToNext() {
        if (storyIndex < stories.size - 1) {
            storyIndex++
        } else if (groupIndex < userGroups.size - 1) {
            groupIndex++
            storyIndex = 0
        } else {
            onDismiss()
        }
    }

    fun moveToPrevious() {
        if (storyIndex > 0) {
            storyIndex--
        } else if (groupIndex > 0) {
            groupIndex--
            val prevStories = userGroups[groupIndex].stories
            storyIndex = (prevStories.size - 1).coerceAtLeast(0)
        }
    }

    // Animated progress bar for current story
    val progress = remember(groupIndex, storyIndex) { Animatable(0f) }

    LaunchedEffect(groupIndex, storyIndex, isPaused) {
        if (!isPaused) {
            val remaining = (1f - progress.value).coerceAtLeast(0.01f)
            val duration = (5000 * remaining).toInt()
            val result = progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration, easing = LinearEasing)
            )
            if (progress.value >= 0.99f) {
                moveToNext()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(groupIndex, storyIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val width = size.width
                            if (offset.x < width * 0.35f) {
                                moveToPrevious()
                            } else {
                                moveToNext()
                            }
                        }
                    )
                }
        ) {
            // Full Screen Story Image
            AsyncImage(
                model = currentStory.fullMediaUrl,
                contentDescription = "Story Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top gradient overlay for header visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            // Bottom gradient overlay for caption visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
            )

            // Top Controls (Progress bars + Header)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Segmented Progress Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val barProgress = when {
                            index < storyIndex -> 1f
                            index == storyIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentGroup.fullAvatarUrl,
                        contentDescription = currentGroup.displayName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentGroup.displayName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentStory.displayTimeAgo,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }

                    // Delete button if own story
                    if (currentGroup.isMe) {
                        IconButton(
                            onClick = {
                                onDeleteStory(currentStory.id)
                                if (stories.size <= 1) {
                                    onDismiss()
                                } else {
                                    moveToNext()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Story",
                                tint = Color.White
                            )
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Story",
                            tint = Color.White
                        )
                    }
                }
            }

            // Caption at Bottom
            if (!currentStory.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp)
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentStory.caption!!,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
