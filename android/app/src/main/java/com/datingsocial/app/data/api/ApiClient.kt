package com.datingsocial.app.data.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREFS_NAME = "aura_dating_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USER_ID = "current_user_id"

    // Default ngrok public tunnel URL for real-device testing over internet
    const val DEFAULT_URL = "https://usage-wharf-reoccupy.ngrok-free.dev/api/v1/"

    private var _baseUrl: String = DEFAULT_URL
    var authToken: String? = null
    var currentUserId: Long? = null
    private var prefs: SharedPreferences? = null

    var baseUrl: String
        get() = _baseUrl
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            val finalUrl = if (formatted.endsWith("api/v1/")) formatted else "${formatted}api/v1/"
            _baseUrl = finalUrl
            prefs?.edit()?.putString(KEY_BASE_URL, finalUrl)?.apply()
            rebuildRetrofit()
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs?.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
        authToken = prefs?.getString(KEY_AUTH_TOKEN, null)
        val savedUserId = prefs?.getLong(KEY_USER_ID, -1L) ?: -1L
        if (savedUserId != -1L) currentUserId = savedUserId
        baseUrl = savedUrl
    }

    fun saveAuth(token: String, userId: Long) {
        authToken = token
        currentUserId = userId
        prefs?.edit()?.putString(KEY_AUTH_TOKEN, token)?.putLong(KEY_USER_ID, userId)?.apply()
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        authToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        // Essential for ngrok: bypass interstitial browser warning screen
        requestBuilder.header("ngrok-skip-browser-warning", "true")
        requestBuilder.header("User-Agent", "AuraDatingApp/1.0.0")
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var _service: DatingApiService? = null

    val service: DatingApiService
        get() {
            if (_service == null) {
                rebuildRetrofit()
            }
            return _service!!
        }

    private fun rebuildRetrofit() {
        _service = Retrofit.Builder()
            .baseUrl(_baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DatingApiService::class.java)
    }
}
