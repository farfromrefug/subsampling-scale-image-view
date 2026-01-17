package com.davemorrissey.labs.subscaleview.decoder

/**
 * Configuration for border detection algorithm.
 * These settings control how borders are detected and cropped.
 * 
 * @param maxBorderDetectionDimension Maximum dimension for the sampled bitmap (must be positive)
 * @param threshold Threshold for grayscale detection (must be between 0.0 and 1.0)
 * @param filledRatioLimit Ratio of pixels that must be "filled" to detect content (must be between 0.0 and 1.0)
 * @param cropOnlyWhite When true, only crop white backgrounds (not black)
 * @param maxCropPercentage Maximum percentage of dimension to crop (0.0 to 1.0), null for unlimited
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
    val filledRatioLimit: Float = 0.15f,
    
    /**
     * When true, only detect and crop white backgrounds (not black).
     * This is useful for documents or images where you only want to remove white borders.
     * Default: false (detect both black and white)
     */
    val cropOnlyWhite: Boolean = false,
    
    /**
     * Maximum percentage of width/height that can be cropped for any dimension (0.0 to 1.0).
     * If the detected border exceeds this percentage, no cropping is applied for that dimension.
     * This prevents cropping of cover pages or images with centered content on white backgrounds.
     * Example: 0.3 means at most 30% can be cropped from any edge.
     * Default: null (unlimited cropping)
     */
    val maxCropPercentage: Float? = null
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
        if (maxCropPercentage != null) {
            require(maxCropPercentage in 0.0f..1.0f) {
                "maxCropPercentage must be between 0.0 and 1.0, got: $maxCropPercentage"
            }
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
