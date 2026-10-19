package com.datingsocial.app.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.datingsocial.app.ui.theme.*
import com.datingsocial.app.util.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var user by remember { mutableStateOf(DemoDataProvider.currentUser) }

    var displayName by remember { mutableStateOf(user.displayName ?: "") }
    var occupation by remember { mutableStateOf(user.occupation ?: "") }
    var city by remember { mutableStateOf(user.city ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var deletingPhotoId by remember { mutableStateOf<Long?>(null) }

    // Primary photo launcher
    val primaryPhotoLauncher = rememberLauncherForActivityResult(
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
                            Toast.makeText(context, "Main profile photo updated!", Toast.LENGTH_SHORT).show()
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

    // Additional photo launcher
    val additionalPhotoLauncher = rememberLauncherForActivityResult(
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

    fun saveProfile() {
        if (displayName.isBlank()) {
            Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        isSaving = true
        coroutineScope.launch {
            try {
                val updateDto = UpdateProfileDto(
                    displayName = displayName.trim(),
                    occupation = occupation.trim(),
                    city = city.trim(),
                    bio = bio.trim()
                )
                val res = ApiClient.service.updateProfile(updateDto)
                if (res.isSuccessful && res.body() != null) {
                    user = res.body()!!
                    DemoDataProvider.currentUser = user
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    // Update locally as fallback
                    val updated = user.copy(
                        displayName = displayName.trim(),
                        occupation = occupation.trim(),
                        city = city.trim(),
                        bio = bio.trim()
                    )
                    DemoDataProvider.currentUser = updated
                    Toast.makeText(context, "Saved changes!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            } catch (e: Exception) {
                val updated = user.copy(
                    displayName = displayName.trim(),
                    occupation = occupation.trim(),
                    city = city.trim(),
                    bio = bio.trim()
                )
                DemoDataProvider.currentUser = updated
                Toast.makeText(context, "Saved changes locally!", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                    IconButton(
                        onClick = { saveProfile() },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = PrimaryPink, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = PrimaryPink
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Main Avatar Changer
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { primaryPhotoLauncher.launch("image/*") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(FlameGradient)
                                .padding(3.dp)
                        ) {
                            AsyncImage(
                                model = user.primaryPhotoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryPink)
                                .clickable { primaryPhotoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Change Primary Avatar", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Photo Gallery & Management
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Photos Gallery (${(user.photos ?: emptyList()).size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { additionalPhotoLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Photo", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Grid of photos
                val photoList = user.photos ?: emptyList()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    photoList.forEach { photo ->
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                                        .padding(4.dp)
                                        .background(PrimaryPink, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("MAIN", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Delete button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable {
                                        if (deletingPhotoId == null) {
                                            deletingPhotoId = photo.id
                                            coroutineScope.launch {
                                                try {
                                                    val res = ApiClient.service.deletePhoto(photo.id)
                                                    if (res.isSuccessful) {
                                                        // Refresh profile
                                                        val pRes = ApiClient.service.getMyProfile()
                                                        if (pRes.isSuccessful && pRes.body() != null) {
                                                            user = pRes.body()!!
                                                            DemoDataProvider.currentUser = user
                                                        } else {
                                                            val updatedList = (user.photos ?: emptyList()).filter { it.id != photo.id }
                                                            user = user.copy(photos = updatedList)
                                                            DemoDataProvider.currentUser = user
                                                        }
                                                        Toast.makeText(context, "Photo deleted", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Could not delete photo", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Delete failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    deletingPhotoId = null
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (deletingPhotoId == photo.id) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Add Photo Placeholder Card
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, SurfaceCardLight, RoundedCornerShape(12.dp))
                            .clickable { additionalPhotoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = TextMuted, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Add Photo", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Divider(color = SurfaceCardLight.copy(alpha = 0.5f), thickness = 1.dp)

            // Display Name
            Column {
                Text("Display Name", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCardLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Occupation
            Column {
                Text("Occupation / Profession", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCardLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // City
            Column {
                Text("City & Location", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCardLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // Bio
            Column {
                Text("Bio", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCardLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { saveProfile() },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FlameGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("SAVE CHANGES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
