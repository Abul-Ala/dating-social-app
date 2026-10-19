package com.datingsocial.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.datingsocial.app.data.model.*

object DemoDataProvider {

    val emptyProfile = UserProfile(
        id = 0,
        userId = 0,
        displayName = "My Profile",
        age = 25,
        gender = "MALE",
        interestedIn = "FEMALE",
        bio = "",
        occupation = "",
        city = "",
        isVerified = false,
        photos = emptyList(),
        interests = emptyList(),
        distanceKm = 0.0,
        compatibilityScore = 100
    )

    // Dynamic state variables for the current logged-in real user
    var currentUser by mutableStateOf(emptyProfile)
    var candidateProfiles by mutableStateOf<List<UserProfile>>(emptyList())
    var sampleMatches by mutableStateOf<List<MatchItem>>(emptyList())
    var sampleConversations by mutableStateOf<List<ConversationItem>>(emptyList())
    var initialMessages by mutableStateOf<List<ChatMessage>>(emptyList())
    val allUsersList = emptyList<UserProfile>()

    fun reset() {
        currentUser = emptyProfile
        candidateProfiles = emptyList()
        sampleMatches = emptyList()
        sampleConversations = emptyList()
        initialMessages = emptyList()
    }
}
