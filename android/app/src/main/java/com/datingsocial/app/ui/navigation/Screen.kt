package com.datingsocial.app.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Discover : Screen("discover")
    object Matches : Screen("matches")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: Long) = "chat/$conversationId"
    }
}
