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
    
    // Threshold values for grayscale detection
    private const val THRESHOLD_FOR_BLACK = 30  // Pixels with value < 30 are considered black
    private const val THRESHOLD_FOR_WHITE = 225 // Pixels with value > 225 are considered white
    private const val FILLED_RATIO_LIMIT = 0.1f // 10% of pixels must be "filled" to detect content
    
    /**
     * Detect borders in a bitmap and return the content rect.
     * This analyzes the edges of the image to find black or white borders
     * and returns a rectangle representing the actual content area.
     *
     * @param bitmap The bitmap to analyze
     * @return A Rect representing the content area, or null if borders should not be cropped
     */
    fun detectBorders(bitmap: Bitmap): Rect? {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= 0 || height <= 0) {
            return null
        }
        
        // Convert bitmap to grayscale values for efficient processing
        val pixels = extractGrayscalePixels(bitmap)
        
        // Detect borders from each edge
        val top = findBorderTop(pixels, width, height)
        val bottom = findBorderBottom(pixels, width, height)
        val left = findBorderLeft(pixels, width, height, top, bottom)
        val right = findBorderRight(pixels, width, height, top, bottom)
        
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
    private inline fun isBlackPixel(pixels: ByteArray, width: Int, x: Int, y: Int): Boolean {
        val pixel = pixels[y * width + x].toInt() and 0xFF
        return pixel < THRESHOLD_FOR_BLACK
    }
    
    /**
     * Check if a pixel is white based on the threshold.
     */
    private inline fun isWhitePixel(pixels: ByteArray, width: Int, x: Int, y: Int): Boolean {
        val pixel = pixels[y * width + x].toInt() and 0xFF
        return pixel > THRESHOLD_FOR_WHITE
    }
    
    /**
     * Find the top border by scanning from top down.
     */
    private fun findBorderTop(pixels: ByteArray, width: Int, height: Int): Int {
        val filledLimit = (width * FILLED_RATIO_LIMIT / 2).roundToInt()
        
        // Scan first line to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        
        var x = 0
        while (x < width) {
            if (isBlackPixel(pixels, width, x, 0)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, x, 0)) {
                whitePixels++
            }
            x += 2
        }
        
        val detectFunc: (ByteArray, Int, Int, Int) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return 0
            }
            blackPixels > filledLimit -> ::isWhitePixel
            else -> ::isBlackPixel
        }
        
        // Scan horizontal lines in search of filled lines
        for (y in 1 until height) {
            var filledCount = 0
            
            x = 0
            while (x < width) {
                if (detectFunc(pixels, width, x, y)) {
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
    private fun findBorderBottom(pixels: ByteArray, width: Int, height: Int): Int {
        val filledLimit = (width * FILLED_RATIO_LIMIT / 2).roundToInt()
        
        // Scan last line to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        val lastY = height - 1
        
        var x = 0
        while (x < width) {
            if (isBlackPixel(pixels, width, x, lastY)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, x, lastY)) {
                whitePixels++
            }
            x += 2
        }
        
        val detectFunc: (ByteArray, Int, Int, Int) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return height
            }
            blackPixels > filledLimit -> ::isWhitePixel
            else -> ::isBlackPixel
        }
        
        // Scan horizontal lines in search of filled lines
        for (y in height - 2 downTo 1) {
            var filledCount = 0
            
            x = 0
            while (x < width) {
                if (detectFunc(pixels, width, x, y)) {
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
    private fun findBorderLeft(pixels: ByteArray, width: Int, height: Int, top: Int, bottom: Int): Int {
        val effectiveHeight = bottom - top
        val filledLimit = (effectiveHeight * FILLED_RATIO_LIMIT / 2).roundToInt()
        
        // Scan first column to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        
        var y = top
        while (y < bottom) {
            if (isBlackPixel(pixels, width, 0, y)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, 0, y)) {
                whitePixels++
            }
            y += 2
        }
        
        val detectFunc: (ByteArray, Int, Int, Int) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return 0
            }
            blackPixels > filledLimit -> ::isWhitePixel
            else -> ::isBlackPixel
        }
        
        // Scan vertical lines in search of filled lines
        for (x in 1 until width) {
            var filledCount = 0
            
            y = top
            while (y < bottom) {
                if (detectFunc(pixels, width, x, y)) {
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
    private fun findBorderRight(pixels: ByteArray, width: Int, height: Int, top: Int, bottom: Int): Int {
        val effectiveHeight = bottom - top
        val filledLimit = (effectiveHeight * FILLED_RATIO_LIMIT / 2).roundToInt()
        
        // Scan last column to detect dominant color
        var whitePixels = 0
        var blackPixels = 0
        val lastX = width - 1
        
        var y = top
        while (y < bottom) {
            if (isBlackPixel(pixels, width, lastX, y)) {
                blackPixels++
            } else if (isWhitePixel(pixels, width, lastX, y)) {
                whitePixels++
            }
            y += 2
        }
        
        val detectFunc: (ByteArray, Int, Int, Int) -> Boolean = when {
            whitePixels > filledLimit && blackPixels > filledLimit -> {
                // Mixed fill found, don't crop
                return width
            }
            blackPixels > filledLimit -> ::isWhitePixel
            else -> ::isBlackPixel
        }
        
        // Scan vertical lines in search of filled lines
        for (x in width - 2 downTo 1) {
            var filledCount = 0
            
            y = top
            while (y < bottom) {
                if (detectFunc(pixels, width, x, y)) {
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
