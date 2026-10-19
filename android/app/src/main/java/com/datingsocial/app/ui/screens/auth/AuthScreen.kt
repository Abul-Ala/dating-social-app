package com.datingsocial.app.ui.screens.auth

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.datingsocial.app.data.DemoDataProvider
import com.datingsocial.app.data.api.ApiClient
import com.datingsocial.app.data.model.*
import com.datingsocial.app.util.ImageUtils
import com.datingsocial.app.ui.screens.profile.ServerSettingsDialog
import com.datingsocial.app.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(true) }
    var showServerSettings by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    // Register fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }
    var interestedIn by remember { mutableStateOf("EVERYONE") }

    // Login fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun doRegister() {
        if (name.isBlank() || email.isBlank()) {
            errorMessage = "Please enter both name and email"
            return
        }
        if (password.length < 3) {
            errorMessage = "Password must be at least 3 characters"
            return
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                var uploadedPhotoUrl: String? = null
                if (selectedPhotoUri != null) {
                    try {
                        val part = ImageUtils.preparePhotoPart(context, selectedPhotoUri!!)
                        if (part != null) {
                            val upRes = ApiClient.service.uploadRegistrationPhoto(part)
                            if (upRes.isSuccessful && upRes.body() != null) {
                                uploadedPhotoUrl = upRes.body()!!["photoUrl"]
                            }
                        }
                    } catch (e: Exception) {}
                }

                val finalPhotoUrl = uploadedPhotoUrl ?: if (gender == "MALE")
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800"
                else
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800"

                val req = RegisterRequestDto(
                    displayName = name.trim(),
                    email = email.trim(),
                    password = password,
                    gender = gender,
                    interestedIn = interestedIn,
                    birthDate = "2000-01-01",
                    city = "Mumbai, India",
                    initialPhotoUrl = finalPhotoUrl,
                    interestIds = listOf(1, 2, 3)
                )
                val res = ApiClient.service.register(req)
                if (res.isSuccessful && res.body() != null) {
                    val auth = res.body()!!
                    ApiClient.saveAuth(auth.token, auth.userId)
                    val profile = auth.profile ?: UserProfile(
                        id = auth.userId,
                        userId = auth.userId,
                        displayName = name.trim(),
                        age = 25,
                        gender = gender,
                        interestedIn = interestedIn,
                        city = "Mumbai, India",
                        photos = listOf(Photo(1, finalPhotoUrl, true))
                    )
                    DemoDataProvider.currentUser = profile
                    Toast.makeText(context, "Welcome, ${name.trim()}!", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                } else {
                    val errStr = res.errorBody()?.string() ?: ""
                    val cleanMsg = try {
                        org.json.JSONObject(errStr).optString("message", "")
                    } catch (e: Exception) { "" }
                    errorMessage = if (cleanMsg.isNotBlank()) cleanMsg else "Could not register ($errStr)"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error: ${e.localizedMessage}. Check server URL."
            } finally {
                isLoading = false
            }
        }
    }

    fun doLogin() {
        if (loginEmail.isBlank() || loginPassword.isBlank()) {
            errorMessage = "Please enter your email and password"
            return
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val req = LoginRequestDto(email = loginEmail.trim(), password = loginPassword)
                val res = ApiClient.service.login(req)
                if (res.isSuccessful && res.body() != null) {
                    val auth = res.body()!!
                    ApiClient.saveAuth(auth.token, auth.userId)
                    auth.profile?.let { DemoDataProvider.currentUser = it }
                    Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                } else {
                    errorMessage = "Invalid email or password."
                }
            } catch (e: Exception) {
                errorMessage = "Failed to connect: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            Surface(
                color = SurfaceDark,
                shadowElevation = 16.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRegisterMode) doRegister() else doLogin()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
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
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (isRegisterMode) "REGISTER & START  ➔" else "SIGN IN  ➔",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Logo
            Text(
                text = "AURA",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryPink,
                letterSpacing = 3.sp
            )

            Text(
                text = "Real Connections. Date by Vibe.",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            // Server connection info pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceCard)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { showServerSettings = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val displayUrl = ApiClient.baseUrl.removePrefix("https://").removePrefix("http://").take(22) + "..."
                Text(
                    text = "Server: $displayUrl",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
            }

            // Tab Selector: Register vs Login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(if (isRegisterMode) Modifier.background(FlameGradient) else Modifier.background(Color.Transparent))
                        .clickable {
                            isRegisterMode = true
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Account",
                        color = if (isRegisterMode) Color.White else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(if (!isRegisterMode) Modifier.background(FlameGradient) else Modifier.background(Color.Transparent))
                        .clickable {
                            isRegisterMode = false
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = if (!isRegisterMode) Color.White else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Error Message
            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PassRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = PassRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isRegisterMode) {
                // Profile Photo Upload Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(FlameGradient)
                                    .padding(3.dp)
                            ) {
                                if (selectedPhotoUri != null) {
                                    AsyncImage(
                                        model = selectedPhotoUri,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(SurfaceDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = "Pick Photo",
                                            tint = PrimaryPink,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }

                            // Camera badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPink)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedPhotoUri != null) "Tap to change photo" else "Upload Profile Picture",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name (e.g. Abul / Sara)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCard
                    )
                )

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCard
                    )
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (at least 3 characters)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCard
                    )
                )

                // Gender selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("I am a:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("MALE" to "👨 Man", "FEMALE" to "👩 Woman").forEach { (valG, labelG) ->
                            val isSelected = gender == valG
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryPink else SurfaceCard)
                                    .clickable { gender = valG }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    labelG,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Interested in selector
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Looking to meet:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("FEMALE" to "Women", "MALE" to "Men", "EVERYONE" to "Everyone").forEach { (valI, labelI) ->
                            val isSelected = interestedIn == valI
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryPurple else SurfaceCard)
                                    .clickable { interestedIn = valI }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    labelI,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

            } else {
                // SIGN IN FORM
                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = { loginEmail = it },
                    label = { Text("Email", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCard
                    )
                )

                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text("Password", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPink,
                        unfocusedBorderColor = SurfaceCard
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Server URL configuration dialog
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
