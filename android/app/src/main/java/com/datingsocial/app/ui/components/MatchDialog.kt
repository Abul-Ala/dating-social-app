package com.datingsocial.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.datingsocial.app.data.model.UserProfile
import com.datingsocial.app.ui.theme.*

@Composable
fun MatchDialog(
    matchedProfile: UserProfile,
    currentUserProfile: UserProfile,
    onSendMessage: () -> Unit,
    onKeepSwiping: () -> Unit
) {
    Dialog(
        onDismissRequest = onKeepSwiping,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60C0D14))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Text(
                    text = "It's a Match! 🎉",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You and ${matchedProfile.displayName} have liked each other.",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Overlapping Avatars
                Box(
                    modifier = Modifier.height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Current User Avatar
                    AsyncImage(
                        model = currentUserProfile.primaryPhotoUrl,
                        contentDescription = "You",
                        modifier = Modifier
                            .offset(x = (-45).dp)
                            .size(110.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .border(3.dp, PrimaryPink, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    // Matched User Avatar
                    AsyncImage(
                        model = matchedProfile.primaryPhotoUrl,
                        contentDescription = matchedProfile.displayName,
                        modifier = Modifier
                            .offset(x = 45.dp)
                            .size(110.dp)
                            .shadow(16.dp, CircleShape)
                            .clip(CircleShape)
                            .border(3.dp, NeonCyan, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Send Message Button
                Button(
                    onClick = onSendMessage,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp)
                        .shadow(12.dp, RoundedCornerShape(27.dp), spotColor = PrimaryPink),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(FlameGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SEND A MESSAGE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Keep Swiping Button
                OutlinedButton(
                    onClick = onKeepSwiping,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = RomanticGradient,
                        width = 1.5.dp
                    )
                ) {
                    Text(
                        text = "KEEP SWIPING",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
