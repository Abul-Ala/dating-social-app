package com.datingsocial.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.datingsocial.app.data.api.ApiClient

class DatingSocialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)

        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient { ApiClient.okHttpClient }
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
