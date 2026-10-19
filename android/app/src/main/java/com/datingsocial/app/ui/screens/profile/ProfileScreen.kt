package com.datingsocial.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.datingsocial.app.data.DemoDataProvider
import com.datingsocial.app.data.api.ApiClient
import com.datingsocial.app.data.model.*
import com.datingsocial.app.util.ImageUtils
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.datingsocial.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var user by remember { mutableStateOf(DemoDataProvider.currentUser) }
    var hideDistance by remember { mutableStateOf(user.hideDistance) }
    var hideOnlineStatus by remember { mutableStateOf(user.hideOnlineStatus) }
    var showServerSettings by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingPhoto = true
            coroutineScope.launch {
                try {
                    val part = ImageUtils.preparePhotoPart(context, uri)
                    if (part != null) {
                        val res = ApiClient.service.uploadProfilePhoto(part, isPrimary = false)
                        if (res.isSuccessful && res.body() != null) {
                            user = res.body()!!
                            DemoDataProvider.currentUser = user
                            Toast.makeText(context, "Photo added to gallery!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not upload photo", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to process photo", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingPhoto = true
            coroutineScope.launch {
                try {
                    val part = ImageUtils.preparePhotoPart(context, uri)
                    if (part != null) {
                        val res = ApiClient.service.uploadProfilePhoto(part, isPrimary = true)
                        if (res.isSuccessful && res.body() != null) {
                            user = res.body()!!
                            DemoDataProvider.currentUser = user
                            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not upload photo", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to process photo", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val res = ApiClient.service.getMyProfile()
            if (res.isSuccessful && res.body() != null) {
                user = res.body()!!
                DemoDataProvider.currentUser = user
            }
        } catch (e: Exception) {}
    }

    Scaffold(
        containerColor = BackgroundDark,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header with Switch Account, Server Config & Edit Profile Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Profile",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Server URL / Ngrok button
                        IconButton(
                            onClick = { showServerSettings = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceCard)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Server URL",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Sign Out Button
                        Button(
                            onClick = {
                                ApiClient.saveAuth("", -1L)
                                DemoDataProvider.reset()
                                onSignOut()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PassRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PassRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out", color = PassRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceCard)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Profile Card Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(RomanticGradient)
                                .padding(4.dp)
                                .clickable { profilePhotoLauncher.launch("image/*") }
                        ) {
                            AsyncImage(
                                model = user.primaryPhotoUrl,
                                contentDescription = user.displayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Upload Camera Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryPink)
                                .clickable { profilePhotoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Upload Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap camera to change photo",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        modifier = Modifier.clickable { profilePhotoLauncher.launch("image/*") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${user.displayName}, ${user.age}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Text(
                            text = user.displayStatus,
                            fontSize = 12.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = user.occupation ?: "Add occupation",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Text(
                        text = user.city ?: "Add city",
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Ngrok Connected Pill
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(SurfaceCard, RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { showServerSettings = true }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🌐 Connected: " + ApiClient.baseUrl.removePrefix("https://").take(25) + "...",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Profile Strength", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("95%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { 0.95f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan,
                                trackColor = SurfaceCardLight
                            )
                        }
                    }
                }
            }

            // Photos Grid
            item {
                val photoList = user.photos ?: emptyList()
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Photos (${photoList.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        TextButton(onClick = { galleryPhotoLauncher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Photo", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        photoList.forEach { photo ->
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceCard)
                            ) {
                                AsyncImage(
                                    model = photo.fullUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (photo.isPrimary) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .background(PrimaryPink, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("MAIN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bio Card
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "About Me",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = user.bio ?: "Add a bio to introduce yourself!",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Interests Section
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(
                        text = "Interests & Passions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (user.interests ?: emptyList()).forEach { interest ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, PrimaryPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${interest.icon} ${interest.name}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Privacy Controls
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Privacy Controls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hide exact distance", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Only display city and approximate zone", color = TextMuted, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = hideDistance,
                                    onCheckedChange = { hideDistance = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPink, checkedTrackColor = PrimaryPink.copy(alpha = 0.5f))
                                )
                            }

                            HorizontalDivider(color = SurfaceCardLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Incognito status", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Do not show 'Active now' status dot to matches", color = TextMuted, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = hideOnlineStatus,
                                    onCheckedChange = { hideOnlineStatus = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPink, checkedTrackColor = PrimaryPink.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }

            // Sign Out / Switch Account Button
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Button(
                        onClick = {
                            ApiClient.saveAuth("", -1L)
                            DemoDataProvider.reset()
                            onSignOut()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PassRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PassRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of Account", color = PassRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Server URL / Ngrok Settings Dialog
        if (showServerSettings) {
            ServerSettingsDialog(
                currentUrl = ApiClient.baseUrl,
                onDismiss = { showServerSettings = false },
                onSave = { newUrl ->
                    ApiClient.baseUrl = newUrl
                    showServerSettings = false
                    Toast.makeText(context, "Server URL updated!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun ServerSettingsDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backend Server URL",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "If you restart ngrok or change tunnels, paste your ngrok HTTPS URL here so your phone and your friend's phone can connect:",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Server Base URL", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceCardLight
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(urlText.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Connect", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterAccountDialog(
    onDismiss: () -> Unit,
    onRegistered: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("password123") }
    var gender by remember { mutableStateOf("MALE") }
    var occupation by remember { mutableStateOf("Explorer") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Create New Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                errorMessage?.let {
                    Text(text = it, color = PassRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text("Profession / Occupation", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("MALE" to "I am Male", "FEMALE" to "I am Female").forEach { (valG, labelG) ->
                        val isSelected = gender == valG
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryPink else SurfaceCard)
                                .clickable { gender = valG }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(labelG, color = if (isSelected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank()) {
                            errorMessage = "Please enter both name and email"
                            return@Button
                        }
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val photo = if (gender == "MALE")
                                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800"
                                else
                                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800"

                                val req = RegisterRequestDto(
                                    displayName = name.trim(),
                                    email = email.trim(),
                                    password = password,
                                    gender = gender,
                                    interestedIn = if (gender == "MALE") "FEMALE" else "MALE",
                                    birthDate = "2000-01-01",
                                    city = "Mumbai, India",
                                    initialPhotoUrl = photo,
                                    interestIds = listOf(1, 2, 3)
                                )
                                val response = ApiClient.service.register(req)
                                if (response.isSuccessful && response.body() != null) {
                                    val auth = response.body()!!
                                    ApiClient.saveAuth(auth.token, auth.userId)
                                    val createdUser = auth.profile ?: UserProfile(
                                        id = auth.userId,
                                        userId = auth.userId,
                                        displayName = name.trim(),
                                        age = 26,
                                        gender = gender,
                                        occupation = occupation,
                                        city = "Mumbai, India",
                                        photos = listOf(Photo(1, photo, true))
                                    )
                                    onRegistered(createdUser)
                                } else {
                                    errorMessage = "Registration response: " + (response.errorBody()?.string() ?: "Failed")
                                }
                            } catch (e: Exception) {
                                // Fallback for offline demo
                                val fallbackUser = UserProfile(
                                    id = System.currentTimeMillis() % 10000,
                                    userId = System.currentTimeMillis() % 10000,
                                    displayName = name.trim(),
                                    age = 24,
                                    gender = gender,
                                    occupation = occupation,
                                    city = "Mumbai, India",
                                    photos = listOf(Photo(1, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800", true))
                                )
                                onRegistered(fallbackUser)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Register & Start Dating", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    }
}

