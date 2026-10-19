package com.datingsocial.app.data.model

import com.google.gson.annotations.SerializedName
data class Photo(
    val id: Long = 0,
    val photoUrl: String? = null,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0
) {
    val fullUrl: String
        get() {
            val raw = photoUrl ?: ""
            return if (raw.startsWith("/")) {
                val base = com.datingsocial.app.data.api.ApiClient.baseUrl.removeSuffix("/api/v1/").removeSuffix("/")
                base + raw
            } else {
                raw
            }
        }
}

data class Interest(
    val id: Long = 0,
    val name: String? = null,
    val category: String = "Lifestyle",
    val icon: String = "✨"
)

data class UserProfile(
    val id: Long = 0,
    val userId: Long = 0,
    val displayName: String? = "",
    val age: Int = 25,
    val gender: String? = "FEMALE",
    val interestedIn: String? = "EVERYONE",
    val bio: String? = null,
    val occupation: String? = null,
    val city: String? = null,
    val isVerified: Boolean = false,
    val hideDistance: Boolean = false,
    val hideOnlineStatus: Boolean = false,
    val photos: List<Photo>? = emptyList(),
    val interests: List<Interest>? = emptyList(),
    val distanceKm: Double? = null,
    val compatibilityScore: Int = 85,
    val userStatus: String? = "Offline"
) {
    val name: String
        get() = if (!displayName.isNullOrBlank()) displayName else "User"

    val displayStatus: String
        get() = if (hideOnlineStatus) "Offline" else (userStatus ?: "Offline")

    val primaryPhotoUrl: String
        get() {
            val list = photos ?: emptyList()
            val raw = list.firstOrNull { it.isPrimary }?.photoUrl
                ?: list.firstOrNull()?.photoUrl
                ?: if (gender == "MALE") "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800"
                else "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800"
            return if (raw.startsWith("/")) {
                val base = com.datingsocial.app.data.api.ApiClient.baseUrl.removeSuffix("/api/v1/").removeSuffix("/")
                base + raw
            } else {
                raw
            }
        }
}

data class SwipeResult(
    @SerializedName("isMatch")
    val isMatch: Boolean = false,
    @SerializedName("matchId")
    val matchId: Long? = null,
    @SerializedName("conversationId")
    val conversationId: Long? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("matchedProfile")
    val matchedProfile: UserProfile? = null
)

data class MatchItem(
    val matchId: Long = 0,
    val conversationId: Long? = null,
    val matchedUser: UserProfile? = null,
    val matchedAt: String? = ""
)

data class ConversationItem(
    val id: Long = 0,
    val matchId: Long = 0,
    val otherUser: UserProfile? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val updatedAt: String? = null,
    val unreadCount: Long = 0
) {
    val displayLastMessage: String
        get() = if (!lastMessage.isNullOrBlank()) lastMessage else "Start a conversation..."

    val displayTime: String
        get() = when {
            !lastMessageTime.isNullOrBlank() && lastMessageTime.contains("T") -> lastMessageTime.substringAfter("T").take(5)
            !lastMessageTime.isNullOrBlank() -> lastMessageTime
            !updatedAt.isNullOrBlank() && updatedAt.contains("T") -> updatedAt.substringAfter("T").take(5)
            !updatedAt.isNullOrBlank() -> updatedAt
            else -> ""
        }
}

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long = 0,
    val senderId: Long = 0,
    val senderName: String? = null,
    val content: String? = null,
    val type: String? = "TEXT",
    val isRead: Boolean = false,
    val isMe: Boolean = false,
    val createdAt: String? = null,
    val time: String? = null
) {
    val displayContent: String
        get() = content ?: ""

    val displayTime: String
        get() = when {
            !time.isNullOrBlank() -> time
            !createdAt.isNullOrBlank() && createdAt.contains("T") -> createdAt.substringAfter("T").take(5)
            else -> "Just now"
        }

    val displaySenderName: String
        get() = if (!senderName.isNullOrBlank()) senderName else "User"
}

data class AuthData(
    val token: String,
    val userId: Long,
    val email: String,
    val profile: UserProfile?
)

data class RegisterRequestDto(
    val displayName: String,
    val email: String,
    val password: String,
    val gender: String,
    val interestedIn: String,
    val birthDate: String = "2000-01-01",
    val city: String = "Mumbai, India",
    val initialPhotoUrl: String? = null,
    val interestIds: List<Long> = listOf(1, 2, 3)
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class SwipeRequestDto(
    @SerializedName("targetUserId")
    val targetUserId: Long,
    @SerializedName("isLike")
    val isLike: Boolean
)

data class SendMessageDto(
    val content: String,
    val type: String = "TEXT"
)

data class UpdateProfileDto(
    val displayName: String? = null,
    val bio: String? = null,
    val occupation: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val interestedIn: String? = null,
    val interestIds: List<Long>? = null,
    val hideDistance: Boolean? = null,
    val hideOnlineStatus: Boolean? = null
)

data class BlockRequestDto(
    val targetUserId: Long,
    val reason: String = "Blocked by user"
)

data class ReportRequestDto(
    val targetUserId: Long,
    val reason: String = "Inappropriate behavior",
    val details: String = ""
)


data class StoryItem(
    val id: Long = 0,
    val userId: Long = 0,
    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val mediaUrl: String? = null,
    val caption: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val timeAgo: String? = null,
    val viewCount: Int = 0
) {
    val fullMediaUrl: String
        get() {
            val raw = mediaUrl ?: ""
            return if (raw.startsWith("/")) {
                val base = com.datingsocial.app.data.api.ApiClient.baseUrl.removeSuffix("/api/v1/").removeSuffix("/")
                base + raw
            } else {
                raw
            }
        }

    val fullAvatarUrl: String
        get() {
            val raw = userAvatarUrl ?: ""
            return if (raw.startsWith("/")) {
                val base = com.datingsocial.app.data.api.ApiClient.baseUrl.removeSuffix("/api/v1/").removeSuffix("/")
                base + raw
            } else {
                raw
            }
        }

    val displayTimeAgo: String
        get() = if (!timeAgo.isNullOrBlank()) timeAgo else "Recently"
}

data class UserStoryGroup(
    val userId: Long = 0,
    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val isMe: Boolean = false,
    val stories: List<StoryItem> = emptyList()
) {
    val displayName: String
        get() = if (isMe) "Your Story" else (userName?.split(" ")?.firstOrNull() ?: (userName ?: "User"))

    val fullAvatarUrl: String
        get() {
            val raw = userAvatarUrl ?: ""
            return if (raw.startsWith("/")) {
                val base = com.datingsocial.app.data.api.ApiClient.baseUrl.removeSuffix("/api/v1/").removeSuffix("/")
                base + raw
            } else {
                raw
            }
        }
}


data class LikeNotificationDto(
    val likerUserId: Long = 0,
    val likerName: String? = null,
    val likerPhotoUrl: String? = null,
    val message: String? = null,
    val likedAt: String? = null,
    val isMatched: Boolean = false
) {
    val fullPhotoUrl: String
        get() {
            val raw = likerPhotoUrl ?: ""
            return if (raw.startsWith("/")) {
                val base = com.datingsocial.app.data.api.ApiClient.baseUrl.removeSuffix("/api/v1/").removeSuffix("/")
                base + raw
            } else {
                raw
            }
        }

    val displayName: String
        get() = if (!likerName.isNullOrBlank()) likerName else "Someone"

    val displayMessage: String
        get() = if (!message.isNullOrBlank()) message else "liked your profile! 💕"
}

