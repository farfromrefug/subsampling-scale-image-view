package com.davemorrissey.labs.subscaleview.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.provider.InputProvider

/**
 * Default implementation of [ImageDecoder] using Android's [BitmapFactory].
 * This class processes the [SubsamplingScaleImageView.preferredBitmapConfig]
 * if set.
 */
class SkiaImageDecoder(
    private val bitmapConfig: Bitmap.Config? = null
) : ImageDecoder {

    override fun init(context: Context, provider: InputProvider): Bitmap {
        val options = BitmapFactory.Options()
        val config = bitmapConfig ?: SubsamplingScaleImageView.getPreferredBitmapConfig()
        if (config != null) {
            options.inPreferredConfig = config
        }

        // Optimize for quality
        options.inDither = true
        options.inPreferQualityOverSpeed = true

        provider.openStream().use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            if (bitmap == null) {
                throw RuntimeException("Skia image decoder returned null bitmap - image format may not be supported")
            }
            return bitmap
        }
    }
}
