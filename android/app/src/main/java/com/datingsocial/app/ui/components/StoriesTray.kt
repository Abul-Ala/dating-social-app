package com.datingsocial.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.datingsocial.app.data.model.UserStoryGroup
import com.datingsocial.app.ui.theme.*

@Composable
fun StoriesTray(
    userStoryGroups: List<UserStoryGroup>,
    currentUserAvatarUrl: String,
    onAddStoryClick: () -> Unit,
    onStoryGroupClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val myGroupIndex = userStoryGroups.indexOfFirst { it.isMe }
    val myGroup = if (myGroupIndex >= 0) userStoryGroups[myGroupIndex] else null
    val hasMyStory = myGroup != null && myGroup.stories.isNotEmpty()

    // Filter other users' story groups
    val otherGroups = userStoryGroups.filter { !it.isMe }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "24h Moments",
                fontSize = 12.sp,
                color = PrimaryPink,
                fontWeight = FontWeight.Medium
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First item: Your Story
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar Box with optional ring
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .then(
                                    if (hasMyStory) Modifier.background(FlameGradient).padding(2.5.dp)
                                    else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape).padding(2.dp)
                                )
                                .clip(CircleShape)
                                .clickable {
                                    if (hasMyStory && myGroupIndex >= 0) {
                                        onStoryGroupClick(myGroupIndex)
                                    } else {
                                        onAddStoryClick()
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = if (hasMyStory) myGroup!!.fullAvatarUrl else currentUserAvatarUrl,
                                contentDescription = "Your Story",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Plus Badge at Bottom End
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(PrimaryPink)
                                .border(2.dp, BackgroundDark, CircleShape)
                                .clickable { onAddStoryClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Story",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Your Story",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Other users' story groups
            itemsIndexed(otherGroups) { index, group ->
                // Map to index in full userStoryGroups
                val fullIndex = userStoryGroups.indexOf(group)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable {
                        onStoryGroupClick(if (fullIndex >= 0) fullIndex else index)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(FlameGradient)
                            .padding(2.5.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = group.fullAvatarUrl,
                            contentDescription = group.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = group.displayName,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
