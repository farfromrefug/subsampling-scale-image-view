package com.davemorrissey.labs.subscaleview.decoder

/**
 * Configuration for border detection algorithm.
 * These settings control how borders are detected and cropped.
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
    companion object {
        /**
         * Default configuration values.
         */
        @JvmStatic
        val DEFAULT = BorderDetectionConfig()
    }
}
