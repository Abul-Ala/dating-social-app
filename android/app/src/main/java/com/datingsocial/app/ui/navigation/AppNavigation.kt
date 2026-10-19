package com.datingsocial.app.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.datingsocial.app.data.api.ApiClient
import com.datingsocial.app.data.DemoDataProvider
import com.datingsocial.app.ui.screens.auth.AuthScreen
import com.datingsocial.app.ui.screens.chat.ChatScreen
import com.datingsocial.app.ui.screens.discover.DiscoverScreen
import com.datingsocial.app.ui.screens.matches.MatchesScreen
import com.datingsocial.app.ui.screens.profile.EditProfileScreen
import com.datingsocial.app.ui.screens.profile.ProfileScreen
import com.datingsocial.app.ui.theme.*

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Discover.route,
        Screen.Matches.route,
        Screen.Profile.route
    )

    val startDestination = if (!ApiClient.authToken.isNullOrBlank()) Screen.Discover.route else Screen.Auth.route

    var lastKnownLikesCount by remember { mutableIntStateOf(-1) }
    var likesReceivedCount by remember { mutableIntStateOf(0) }
    var hasSeenCurrentLikes by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.Matches.route) {
            hasSeenCurrentLikes = true
        }
    }

    LaunchedEffect(ApiClient.authToken) {
        if (!ApiClient.authToken.isNullOrBlank()) {
            try {
                val res = ApiClient.service.getMyProfile()
                if (res.isSuccessful && res.body() != null) {
                    DemoDataProvider.currentUser = res.body()!!
                } else if (res.code() == 401 || res.code() == 403) {
                    ApiClient.saveAuth("", -1L)
                    DemoDataProvider.reset()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } catch (e: Exception) {}

            // Periodically check for new incoming likes to notify the user in real-time
            while (true) {
                try {
                    val likesRes = ApiClient.service.getLikesReceived()
                    if (likesRes.isSuccessful && likesRes.body() != null) {
                        val currentLikes = likesRes.body()!!
                        likesReceivedCount = currentLikes.size
                        if (lastKnownLikesCount != -1 && currentLikes.size > lastKnownLikesCount) {
                            hasSeenCurrentLikes = false
                            val newLiker = currentLikes.firstOrNull()?.displayName ?: "Someone"
                            Toast.makeText(
                                context,
                                "❤️ $newLiker liked your profile! Open Matches to see!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        lastKnownLikesCount = currentLikes.size
                    }
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(4000)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Discover.route,
                        onClick = {
                            if (currentRoute != Screen.Discover.route) {
                                navController.navigate(Screen.Discover.route) {
                                    popUpTo(Screen.Discover.route) { inclusive = true }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Discover",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Discover", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryPink,
                            selectedTextColor = PrimaryPink,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PrimaryPink.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Matches.route,
                        onClick = {
                            if (currentRoute != Screen.Matches.route) {
                                navController.navigate(Screen.Matches.route)
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    val showLikesBadge = likesReceivedCount > 0 && !hasSeenCurrentLikes && currentRoute != Screen.Matches.route
                                    if (showLikesBadge) {
                                        Badge(containerColor = PrimaryPink) {
                                            Text(
                                                text = "$likesReceivedCount",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Matches & Chat",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = { Text("Matches", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryPink,
                            selectedTextColor = PrimaryPink,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PrimaryPink.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            if (currentRoute != Screen.Profile.route) {
                                navController.navigate(Screen.Profile.route)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Profile", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryPink,
                            selectedTextColor = PrimaryPink,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PrimaryPink.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onNavigateToChat = { convoId ->
                        navController.navigate(Screen.Chat.createRoute(convoId))
                    }
                )
            }

            composable(Screen.Matches.route) {
                MatchesScreen(
                    onNavigateToChat = { convoId ->
                        navController.navigate(Screen.Chat.createRoute(convoId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEditProfile = {
                        navController.navigate(Screen.EditProfile.route)
                    },
                    onSignOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val convoId = backStackEntry.arguments?.getLong("conversationId") ?: 1L
                ChatScreen(
                    conversationId = convoId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
