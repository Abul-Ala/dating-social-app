package com.datingsocial.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.datingsocial.app.data.model.UserProfile
import com.datingsocial.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwipeableCard(
    profile: UserProfile,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTopCard: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember(profile.id) { Animatable(0f) }
    val offsetY = remember(profile.id) { Animatable(0f) }
    var currentPhotoIndex by remember(profile.id) { mutableIntStateOf(0) }

    val photos = (profile.photos ?: emptyList()).ifEmpty {
        listOf(com.datingsocial.app.data.model.Photo(0, profile.primaryPhotoUrl, true))
    }

    if (currentPhotoIndex >= photos.size) {
        currentPhotoIndex = 0
    }

    val rotation = (offsetX.value / 30f).coerceIn(-20f, 20f)
    val swipeThreshold = 300f

    Card(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate(rotation)
            .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Color(0x66000000))
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (isTopCard) {
                    Modifier.pointerInput(profile.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDragX = 0f
                            var totalDragY = 0f
                            var isDrag = false
                            val touchSlop = viewConfiguration.touchSlop

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || change.isConsumed) {
                                    // Pointer lost or cancelled - gracefully reset card position
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f, tween(200))
                                        offsetY.animateTo(0f, tween(200))
                                    }
                                    break
                                }

                                if (change.changedToUp()) {
                                    if (!isDrag) {
                                        // Tap gesture detected
                                        val tapX = change.position.x
                                        val tapY = change.position.y
                                        val cardWidth = size.width.toFloat()
                                        val cardHeight = size.height.toFloat()

                                        // If tapping in the photo area and multiple photos exist
                                        if (tapY < cardHeight * 0.72f && photos.size > 1) {
                                            if (tapX < cardWidth * 0.45f) {
                                                if (currentPhotoIndex > 0) currentPhotoIndex--
                                            } else if (tapX > cardWidth * 0.55f) {
                                                if (currentPhotoIndex < photos.size - 1) currentPhotoIndex++
                                            } else {
                                                onCardClick()
                                            }
                                        } else {
                                            onCardClick()
                                        }
                                    } else {
                                        // Drag completed
                                        coroutineScope.launch {
                                            if (offsetX.value > swipeThreshold) {
                                                offsetX.animateTo(1200f, tween(250))
                                                onSwipeRight()
                                            } else if (offsetX.value < -swipeThreshold) {
                                                offsetX.animateTo(-1200f, tween(250))
                                                onSwipeLeft()
                                            } else {
                                                offsetX.animateTo(0f, tween(200))
                                                offsetY.animateTo(0f, tween(200))
                                            }
                                        }
                                    }
                                    break
                                }

                                val dragX = change.position.x - change.previousPosition.x
                                val dragY = change.position.y - change.previousPosition.y
                                totalDragX += dragX
                                totalDragY += dragY

                                if (!isDrag && (abs(totalDragX) > touchSlop || abs(totalDragY) > touchSlop)) {
                                    isDrag = true
                                }

                                if (isDrag) {
                                    change.consume()
                                    coroutineScope.launch {
                                        offsetX.snapTo(offsetX.value + dragX)
                                        offsetY.snapTo(offsetY.value + dragY * 0.4f)
                                    }
                                }
                            }
                        }
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Photo Image
            val currentPhoto = photos.getOrNull(currentPhotoIndex)?.fullUrl ?: profile.primaryPhotoUrl
            AsyncImage(
                model = currentPhoto,
                contentDescription = profile.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top Photo Indicator Bars
            if (photos.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    photos.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (index == currentPhotoIndex) Color.White else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                // Photo Counter Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 26.dp, end = 16.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "📷 ${currentPhotoIndex + 1}/${photos.size}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Explicit Chevron Navigation Buttons (Zero gesture conflict)
                if (currentPhotoIndex > 0) {
                    IconButton(
                        onClick = { currentPhotoIndex-- },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (currentPhotoIndex < photos.size - 1) {
                    IconButton(
                        onClick = { currentPhotoIndex++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Swipe Badges (LIKE / NOPE)
            if (offsetX.value > 60f) {
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp, start = 24.dp)
                        .rotate(-15f)
                        .border(3.dp, SuccessGreen, RoundedCornerShape(12.dp))
                        .background(SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "LIKE",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                }
            } else if (offsetX.value < -60f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 24.dp)
                        .rotate(15f)
                        .border(3.dp, PassRed, RoundedCornerShape(12.dp))
                        .background(PassRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "NOPE",
                        color = PassRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                }
            }

            // Bottom Profile Info Overlay with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x66000000),
                                Color(0xEE0D0E15),
                                Color(0xFF0D0E15)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Match Score & Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Compatibility Score
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057))),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⚡ ${profile.compatibilityScore}% Match",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // User Status Badge
                            val isOnline = profile.displayStatus.contains("Active", ignoreCase = true) || profile.displayStatus.contains("Online", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isOnline) SuccessGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isOnline) SuccessGreen.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.6f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) SuccessGreen else Color.Gray)
                                    )
                                    Text(
                                        text = profile.displayStatus,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onCardClick,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "View Profile",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Name, Age and Verification Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${profile.displayName}, ${profile.age}",
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Profile",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Occupation & Location
                    if (!profile.occupation.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = profile.occupation ?: "",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        val distText = if (profile.distanceKm != null) " • ${profile.distanceKm} km away" else ""
                        Text(
                            text = "${profile.city ?: ""}$distText",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    // Bio
                    if (!profile.bio.isNullOrBlank()) {
                        Text(
                            text = profile.bio ?: "",
                            color = Color(0xFFD1D5DB),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            maxLines = 2
                        )
                    }

                    // Interests Chips
                    if (!profile.interests.isNullOrEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            (profile.interests ?: emptyList()).take(4).forEach { interest ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${interest.icon} ${interest.name}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
