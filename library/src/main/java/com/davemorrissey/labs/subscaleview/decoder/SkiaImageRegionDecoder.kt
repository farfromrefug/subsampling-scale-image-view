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
 * if set and supports automatic border detection and cropping.
 */
class SkiaImageRegionDecoder(
    private val bitmapConfig: Bitmap.Config? = null,
    private val cropBorders: Boolean = false
) : ImageRegionDecoder {

    private var decoder: BitmapRegionDecoder? = null
    private val decoderLock: ReadWriteLock = ReentrantReadWriteLock(true)
    private var contentRect: Rect? = null

    companion object {
        private const val MAX_BORDER_DETECTION_DIMENSION = 1500
    }

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
        
        // Perform border detection if enabled
        if (cropBorders) {
            contentRect = detectImageBorders(d)
        }
        
        return Point(d.width, d.height)
    }

    /**
     * Detect borders in the image by sampling a low-resolution version.
     * This is done once during initialization to minimize performance impact.
     */
    private fun detectImageBorders(decoder: BitmapRegionDecoder): Rect? {
        try {
            val width = decoder.width
            val height = decoder.height
            
            // Sample the image at a lower resolution for border detection
            // Use a sample size that gives us roughly 1000-2000 pixels on the longest dimension
            val sampleSize = maxOf(1, maxOf(width, height) / MAX_BORDER_DETECTION_DIMENSION)
            
            val options = BitmapFactory.Options()
            options.inSampleSize = sampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory for detection
            
            // Decode the entire image at low resolution
            val sampledBitmap = decoder.decodeRegion(
                Rect(0, 0, width, height),
                options
            )
            
            if (sampledBitmap != null) {
                val detectedRect = BorderDetector.detectBorders(sampledBitmap)
                sampledBitmap.recycle()
                
                // Scale the detected rect back to full resolution
                if (detectedRect != null) {
                    return Rect(
                        detectedRect.left * sampleSize,
                        detectedRect.top * sampleSize,
                        detectedRect.right * sampleSize,
                        detectedRect.bottom * sampleSize
                    )
                }
            }
        } catch (e: Exception) {
            // If border detection fails, just return null and use full image
            e.printStackTrace()
        }
        return null
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
            contentRect = null
        } finally {
            decoderLock.writeLock().unlock()
        }
    }

    override fun getContentRect(): Rect? = contentRect
}
