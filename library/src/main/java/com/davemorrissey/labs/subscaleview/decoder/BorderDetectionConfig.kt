package com.davemorrissey.labs.subscaleview.decoder

/**
 * Configuration for border detection algorithm.
 * These settings control how borders are detected and cropped.
 * 
 * @param maxBorderDetectionDimension Maximum dimension for the sampled bitmap (must be positive)
 * @param threshold Threshold for grayscale detection (must be between 0.0 and 1.0)
 * @param filledRatioLimit Ratio of pixels that must be "filled" to detect content (must be between 0.0 and 1.0)
 * @throws IllegalArgumentException if any parameter is outside valid range
 */
data class BorderDetectionConfig(
    /**
     * Maximum dimension (width or height) for the sampled bitmap used in border detection.
     * Higher values provide more accurate detection but use more memory.
     * Default: 500
     */
    val maxBorderDetectionDimension: Int = 500,
    
    /**
     * Threshold for grayscale detection (0.0 to 1.0).
     * Pixels darker than (255 * threshold) are considered black.
     * Pixels lighter than (255 - 255 * threshold) are considered white.
     * Default: 0.95
     */
    val threshold: Double = 0.95,
    
    /**
     * Ratio of pixels that must be "filled" (non-border) to detect content (0.0 to 1.0).
     * Lower values detect borders more aggressively.
     * Default: 0.15 (15%)
     */
    val filledRatioLimit: Float = 0.15f
) {
    init {
        require(maxBorderDetectionDimension > 0) {
            "maxBorderDetectionDimension must be positive, got: $maxBorderDetectionDimension"
        }
        require(threshold in 0.0..1.0) {
            "threshold must be between 0.0 and 1.0, got: $threshold"
        }
        require(filledRatioLimit in 0.0f..1.0f) {
            "filledRatioLimit must be between 0.0 and 1.0, got: $filledRatioLimit"
        }
    }
    
    companion object {
        /**
         * Default configuration values.
         */
        @JvmField
        var DEFAULT: BorderDetectionConfig = BorderDetectionConfig()
    }
}
