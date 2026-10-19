package com.datingsocial.app.data.api

import com.datingsocial.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface DatingApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<AuthData>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<AuthData>

    @Multipart
    @POST("auth/upload-photo")
    suspend fun uploadRegistrationPhoto(
        @Part file: okhttp3.MultipartBody.Part
    ): Response<Map<String, String>>

    @Multipart
    @POST("profiles/me/photos/upload")
    suspend fun uploadProfilePhoto(
        @Part file: okhttp3.MultipartBody.Part,
        @Query("isPrimary") isPrimary: Boolean = true
    ): Response<UserProfile>

    @DELETE("profiles/me/photos/{id}")
    suspend fun deletePhoto(
        @Path("id") photoId: Long
    ): Response<Unit>

    @GET("discovery")
    suspend fun getDiscoveryFeed(
        @Query("minAge") minAge: Int? = null,
        @Query("maxAge") maxAge: Int? = null,
        @Query("gender") gender: String? = null,
        @Query("maxDistanceKm") maxDistanceKm: Double? = null
    ): Response<List<UserProfile>>

    @POST("discovery/swipe")
    suspend fun swipe(
        @Body request: SwipeRequestDto
    ): Response<SwipeResult>

    @GET("matches")
    suspend fun getMatches(): Response<List<MatchItem>>

    @GET("matches/likes-received")
    suspend fun getLikesReceived(): Response<List<LikeNotificationDto>>

    @GET("chat/conversations")
    suspend fun getConversations(): Response<List<ConversationItem>>

    @GET("chat/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: Long
    ): Response<List<ChatMessage>>

    @POST("chat/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") conversationId: Long,
        @Body body: SendMessageDto
    ): Response<ChatMessage>

    @GET("profiles/me")
    suspend fun getMyProfile(): Response<UserProfile>

    @PUT("profiles/me")
    suspend fun updateProfile(
        @Body body: UpdateProfileDto
    ): Response<UserProfile>

    @POST("safety/block")
    suspend fun blockUser(
        @Body body: BlockRequestDto
    ): Response<Map<String, String>>

    @POST("safety/report")
    suspend fun reportUser(
        @Body body: ReportRequestDto
    ): Response<Map<String, String>>

    @GET("stories/tray")
    suspend fun getStoriesTray(): Response<List<UserStoryGroup>>

    @GET("stories/me")
    suspend fun getMyStories(): Response<List<StoryItem>>

    @Multipart
    @POST("stories")
    suspend fun uploadStory(
        @Part file: okhttp3.MultipartBody.Part,
        @Part("caption") caption: okhttp3.RequestBody? = null
    ): Response<StoryItem>

    @DELETE("stories/{id}")
    suspend fun deleteStory(
        @Path("id") storyId: Long
    ): Response<Unit>

    @POST("stories/{id}/view")
    suspend fun viewStory(
        @Path("id") storyId: Long
    ): Response<Unit>

}
