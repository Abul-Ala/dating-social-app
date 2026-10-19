package com.datingsocial.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {
    fun preparePhotoPart(context: Context, uri: Uri, partName: String = "file"): MultipartBody.Part? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            // Downscale to max 1280px on larger side to save bandwidth and prevent upload limits
            val maxDim = max(originalBitmap.width, originalBitmap.height)
            val scaledBitmap = if (maxDim > 1280) {
                val scale = 1280f / maxDim
                val newW = (originalBitmap.width * scale).toInt()
                val newH = (originalBitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val bytes = baos.toByteArray()
            val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData(partName, "photo.jpg", reqBody)
        } catch (e: Exception) {
            null
        }
    }
}
