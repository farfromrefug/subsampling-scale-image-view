package com.davemorrissey.labs.subscaleview.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.provider.InputProvider
import java.io.InputStream
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Default implementation of [ImageRegionDecoder] using Android's [BitmapRegionDecoder].
 * This class processes the [SubsamplingScaleImageView.preferredBitmapConfig]
 * if set.
 */
class SkiaImageRegionDecoder(
    private val bitmapConfig: Bitmap.Config? = null
) : ImageRegionDecoder {

    private var decoder: BitmapRegionDecoder? = null
    private val decoderLock: ReadWriteLock = ReentrantReadWriteLock(true)

    @Synchronized
    override fun init(context: Context, provider: InputProvider): Point {
        provider.openStream().use { inputStream ->
            decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(inputStream!!)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(inputStream!!, false)
            }
        }
        val d = decoder ?: throw IllegalStateException("BitmapRegionDecoder failed to load")
        return Point(d.width, d.height)
    }

    override fun decodeRegion(sRect: Rect, sampleSize: Int): Bitmap {
        decoderLock.readLock().lock()
        try {
            val decoder = this.decoder ?: throw IllegalStateException("Decoder not initialized")

            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            val config = bitmapConfig ?: SubsamplingScaleImageView.getPreferredBitmapConfig()
            if (config != null) {
                options.inPreferredConfig = config
            }
            options.inDither = true
            options.inPreferQualityOverSpeed = true

            // BitmapRegionDecoder is thread-safe in newer Android versions, but we lock to be safe
            // across all versions and consistent with original library structure.
            val bitmap = decoder.decodeRegion(sRect, options)

            if (bitmap == null) {
                throw RuntimeException("Skia image decoder returned null bitmap - image format may not be supported")
            }
            return bitmap
        } finally {
            decoderLock.readLock().unlock()
        }
    }

    @Synchronized
    override fun isReady(): Boolean {
        return decoder != null && !decoder!!.isRecycled
    }

    @Synchronized
    override fun recycle() {
        decoderLock.writeLock().lock()
        try {
            decoder?.recycle()
            decoder = null
        } finally {
            decoderLock.writeLock().unlock()
        }
    }
}
