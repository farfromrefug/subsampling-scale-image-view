package com.davemorrissey.labs.subscaleview.decoder

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.roundToInt

/**
 * Utility class for detecting borders (black or white edges) in images.
 * Implements an optimized algorithm to find content boundaries by scanning
 * the edges of an image and detecting uniform color borders.
 */
object BorderDetector {
    
    /**
     * Calculate the filled limit threshold for a given dimension.
     */
    private fun calculateFilledLimit(dimension: Int, filledRatioLimit: Float): Int {
        return (dimension * filledRatioLimit / 2).roundToInt()
    }
    
    /**
     * Detect borders in a bitmap and return the content rect.
     * This analyzes the edges of the image to find black or white borders
     * and returns a rectangle representing the actual content area.
     *
     * @param bitmap The bitmap to analyze
     * @param config Configuration for border detection parameters
     * @return A Rect representing the content area, or null if borders should not be cropped
     */
    fun detectBorders(bitmap: Bitmap, config: BorderDetectionConfig = BorderDetectionConfig.DEFAULT): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= 0 || height <= 0) {
            return null
        }
        
        // Convert bitmap to grayscale values for efficient processing
        val pixels = extractGrayscalePixels(bitmap)
        
        // Calculate threshold values from config
        val thresholdForBlack = 255.0 * config.threshold
        val thresholdForWhite = 255.0 - 255.0 * config.threshold
        
        // Detect borders from each edge
        val top = findBorderTop(pixels, width, height, thresholdForBlack, thresholdForWhite, config.filledRatioLimit, config.cropOnlyWhite)
        val bottom = findBorderBottom(pixels, width, height, thresholdForBlack, thresholdForWhite, config.filledRatioLimit, config.cropOnlyWhite)
        val left = findBorderLeft(pixels, width, height, top, bottom, thresholdForBlack, thresholdForWhite, config.filledRatioLimit, config.cropOnlyWhite)
        val right = findBorderRight(pixels, width, height, top, bottom, thresholdForBlack, thresholdForWhite, config.filledRatioLimit, config.cropOnlyWhite)
        
        // Check if crop percentage limits are exceeded
        if (config.maxCropPercentage != null) {
            val maxCropPixelsWidth = (width * config.maxCropPercentage).toInt()
            val maxCropPixelsHeight = (height * config.maxCropPercentage).toInt()
            
            // Check each dimension independently
            val leftCrop = left
            val rightCrop = width - right
            val topCrop = top
            val bottomCrop = height - bottom
            
            // If any dimension exceeds the limit, don't crop that dimension
            val finalLeft = if (leftCrop > maxCropPixelsWidth) 0 else left
            val finalRight = if (rightCrop > maxCropPixelsWidth) width else right
            val finalTop = if (topCrop > maxCropPixelsHeight) 0 else top
            val finalBottom = if (bottomCrop > maxCropPixelsHeight) height else bottom
            
            // If no borders detected after applying limits, return null
            if (finalLeft == 0 && finalTop == 0 && finalRight == width && finalBottom == height) {
                return null
            }
            
            // Ensure valid rectangle
            if (finalRight <= finalLeft || finalBottom <= finalTop) {
                return null
            }
            
            return Rect(finalLeft, finalTop, finalRight, finalBottom)
        }
        
        // If no borders detected or borders are the full image, return null
        if (left == 0 && top == 0 && right == width && bottom == height) {
            return null
        }
        
        // Ensure valid rectangle
        if (right <= left || bottom <= top) {
            return null
        }
        
        return Rect(left, top, right, bottom)
    }
    
    /**
     * Extract grayscale pixel values from a bitmap for efficient border detection.
     * Uses luminance formula to convert RGB to grayscale.
     */
    private fun extractGrayscalePixels(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val grayscale = ByteArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Standard luminance formula
            grayscale[i] = ((0.299 * r + 0.587 * g + 0.114 * b).toInt() and 0xFF).toByte()
        }
        
        return grayscale
    }
    
    /**
     * Check if a pixel is black based on the threshold.
     */
    private inline fun isBlackPixel(pixels: ByteArray, width: Int, x: Int, y: Int, thresholdForBlack: Double): Boolean {
        val pixel = pixels[y * width + x].toInt() and 0xFF
        return pixel < thresholdForBlack
    }
    
    /**
     * Check if a pixel is white based on the threshold.
     */
    private inline fun isWhitePixel(pixels: ByteArray, width: Int, x: Int, y: Int, thresholdForWhite: Double): Boolean {
        val pixel = pixels[y * width + x].toInt() and 0xFF
        return pixel > thresholdForWhite
    }
    
    /**
     * Find the top border by scanning from top down.
     */
    private fun findBorderTop(pixels: ByteArray, width: Int, height: Int, thresholdForBlack: Double, thresholdForWhite: Double, filledRatioLimit: Float, cropOnlyWhite: Boolean): Int {
        val filledLimit = calculateFilledLimit(width, filledRatioLimit)
        
        // Scan first line to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        
        var x = 0
        while (x < width) {
            if (isBlackPixel(pixels, width, x, 0, thresholdForBlack)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, x, 0, thresholdForWhite)) {
                whitePixels++
            }
            x += 2
        }
        
        // If cropOnlyWhite is true, only detect white borders
        if (cropOnlyWhite) {
            if (whitePixels <= filledLimit) {
                // Not enough white pixels, don't crop
                return 0
            }
            // Detect when we hit non-white (black) content
            val threshold = thresholdForWhite
            for (y in 1 until height) {
                var filledCount = 0
                
                x = 0
                while (x < width) {
                    if (isBlackPixel(pixels, width, x, y, thresholdForBlack)) {
                        filledCount++
                    }
                    x += 2
                }
                
                if (filledCount > filledLimit) {
                    return y
                }
            }
            return 0
        }
        
        val detectFunc: (ByteArray, Int, Int, Int, Double) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return 0
            }
            blackPixels > filledLimit -> { pixels, w, x, y, threshold -> isWhitePixel(pixels, w, x, y, threshold) }
            else -> { pixels, w, x, y, threshold -> isBlackPixel(pixels, w, x, y, threshold) }
        }
        
        val threshold = if (blackPixels > filledLimit) thresholdForWhite else thresholdForBlack
        
        // Scan horizontal lines in search of filled lines
        for (y in 1 until height) {
            var filledCount = 0
            
            x = 0
            while (x < width) {
                if (detectFunc(pixels, width, x, y, threshold)) {
                    filledCount++
                }
                x += 2
            }
            
            if (filledCount > filledLimit) {
                // This line contains enough fill
                return y
            }
        }
        
        // No fill found, don't crop
        return 0
    }
    
    /**
     * Find the bottom border by scanning from bottom up.
     */
    private fun findBorderBottom(pixels: ByteArray, width: Int, height: Int, thresholdForBlack: Double, thresholdForWhite: Double, filledRatioLimit: Float, cropOnlyWhite: Boolean): Int {
        val filledLimit = calculateFilledLimit(width, filledRatioLimit)
        
        // Scan last line to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        val lastY = height - 1
        
        var x = 0
        while (x < width) {
            if (isBlackPixel(pixels, width, x, lastY, thresholdForBlack)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, x, lastY, thresholdForWhite)) {
                whitePixels++
            }
            x += 2
        }
        
        // If cropOnlyWhite is true, only detect white borders
        if (cropOnlyWhite) {
            if (whitePixels <= filledLimit) {
                // Not enough white pixels, don't crop
                return height
            }
            // Detect when we hit non-white (black) content
            val threshold = thresholdForWhite
            for (y in height - 2 downTo 1) {
                var filledCount = 0
                
                x = 0
                while (x < width) {
                    if (isBlackPixel(pixels, width, x, y, thresholdForBlack)) {
                        filledCount++
                    }
                    x += 2
                }
                
                if (filledCount > filledLimit) {
                    return y + 1
                }
            }
            return height
        }
        
        val detectFunc: (ByteArray, Int, Int, Int, Double) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return height
            }
            blackPixels > filledLimit -> { pixels, w, x, y, threshold -> isWhitePixel(pixels, w, x, y, threshold) }
            else -> { pixels, w, x, y, threshold -> isBlackPixel(pixels, w, x, y, threshold) }
        }
        
        val threshold = if (blackPixels > filledLimit) thresholdForWhite else thresholdForBlack
        
        // Scan horizontal lines in search of filled lines
        for (y in height - 2 downTo 1) {
            var filledCount = 0
            
            x = 0
            while (x < width) {
                if (detectFunc(pixels, width, x, y, threshold)) {
                    filledCount++
                }
                x += 2
            }
            
            if (filledCount > filledLimit) {
                // This line contains enough fill
                return y + 1
            }
        }
        
        // No fill found, don't crop
        return height
    }
    
    /**
     * Find the left border by scanning from left to right.
     */
    private fun findBorderLeft(pixels: ByteArray, width: Int, height: Int, top: Int, bottom: Int, thresholdForBlack: Double, thresholdForWhite: Double, filledRatioLimit: Float, cropOnlyWhite: Boolean): Int {
        val effectiveHeight = bottom - top
        val filledLimit = calculateFilledLimit(effectiveHeight, filledRatioLimit)
        
        // Scan first column to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        
        var y = top
        while (y < bottom) {
            if (isBlackPixel(pixels, width, 0, y, thresholdForBlack)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, 0, y, thresholdForWhite)) {
                whitePixels++
            }
            y += 2
        }
        
        // If cropOnlyWhite is true, only detect white borders
        if (cropOnlyWhite) {
            if (whitePixels <= filledLimit) {
                // Not enough white pixels, don't crop
                return 0
            }
            // Detect when we hit non-white (black) content
            val threshold = thresholdForWhite
            for (x in 1 until width) {
                var filledCount = 0
                
                y = top
                while (y < bottom) {
                    if (isBlackPixel(pixels, width, x, y, thresholdForBlack)) {
                        filledCount++
                    }
                    y += 2
                }
                
                if (filledCount > filledLimit) {
                    return x
                }
            }
            return 0
        }
        
        val detectFunc: (ByteArray, Int, Int, Int, Double) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return 0
            }
            blackPixels > filledLimit -> { pixels, w, x, y, threshold -> isWhitePixel(pixels, w, x, y, threshold) }
            else -> { pixels, w, x, y, threshold -> isBlackPixel(pixels, w, x, y, threshold) }
        }
        
        val threshold = if (blackPixels > filledLimit) thresholdForWhite else thresholdForBlack
        
        // Scan vertical lines in search of filled lines
        for (x in 1 until width) {
            var filledCount = 0
            
            y = top
            while (y < bottom) {
                if (detectFunc(pixels, width, x, y, threshold)) {
                    filledCount++
                }
                y += 2
            }
            
            if (filledCount > filledLimit) {
                // This line contains enough fill
                return x
            }
        }
        
        // No fill found, don't crop
        return 0
    }
    
    /**
     * Find the right border by scanning from right to left.
     */
    private fun findBorderRight(pixels: ByteArray, width: Int, height: Int, top: Int, bottom: Int, thresholdForBlack: Double, thresholdForWhite: Double, filledRatioLimit: Float, cropOnlyWhite: Boolean): Int {
        val effectiveHeight = bottom - top
        val filledLimit = calculateFilledLimit(effectiveHeight, filledRatioLimit)
        
        // Scan last column to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        val lastX = width - 1
        
        var y = top
        while (y < bottom) {
            if (isBlackPixel(pixels, width, lastX, y, thresholdForBlack)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, lastX, y, thresholdForWhite)) {
                whitePixels++
            }
            y += 2
        }
        
        // If cropOnlyWhite is true, only detect white borders
        if (cropOnlyWhite) {
            if (whitePixels <= filledLimit) {
                // Not enough white pixels, don't crop
                return width
            }
            // Detect when we hit non-white (black) content
            val threshold = thresholdForWhite
            for (x in width - 2 downTo 1) {
                var filledCount = 0
                
                y = top
                while (y < bottom) {
                    if (isBlackPixel(pixels, width, x, y, thresholdForBlack)) {
                        filledCount++
                    }
                    y += 2
                }
                
                if (filledCount > filledLimit) {
                    return x + 1
                }
            }
            return width
        }
        
        val detectFunc: (ByteArray, Int, Int, Int, Double) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return width
            }
            blackPixels > filledLimit -> { pixels, w, x, y, threshold -> isWhitePixel(pixels, w, x, y, threshold) }
            else -> { pixels, w, x, y, threshold -> isBlackPixel(pixels, w, x, y, threshold) }
        }
        
        val threshold = if (blackPixels > filledLimit) thresholdForWhite else thresholdForBlack
        
        // Scan vertical lines in search of filled lines
        for (x in width - 2 downTo 1) {
            var filledCount = 0
            
            y = top
            while (y < bottom) {
                if (detectFunc(pixels, width, x, y, threshold)) {
                    filledCount++
                }
                y += 2
            }
            
            if (filledCount > filledLimit) {
                // This line contains enough fill
                return x + 1
            }
        }
        
        // No fill found, don't crop
        return width
    }
}
