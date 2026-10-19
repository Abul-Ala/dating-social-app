package com.datingsocial.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.datingsocial.app.ui.theme.*

@Composable
fun ActionButtons(
    onRewind: () -> Unit,
    onPass: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rewind Button
        IconButton(
            onClick = onRewind,
            modifier = Modifier
                .size(46.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, GoldSuperlike.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Rewind",
                tint = GoldSuperlike,
                modifier = Modifier.size(22.dp)
            )
        }

        // Pass / Dislike Button
        IconButton(
            onClick = onPass,
            modifier = Modifier
                .size(62.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.5.dp, PassRed.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Pass",
                tint = PassRed,
                modifier = Modifier.size(30.dp)
            )
        }

        // Super Like Button
        IconButton(
            onClick = onSuperLike,
            modifier = Modifier
                .size(52.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Super Like",
                tint = NeonCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        // Like Button (Primary Heart)
        IconButton(
            onClick = onLike,
            modifier = Modifier
                .size(62.dp)
                .shadow(16.dp, CircleShape, spotColor = PrimaryPink)
                .clip(CircleShape)
                .background(FlameGradient)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
