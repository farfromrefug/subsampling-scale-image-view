package com.davemorrissey.labs.subscaleview.decoder

/**
 * Compatibility factory to create decoders from a class object,
 * mimicking the original library behavior for Java compatibility.
 */
class CompatDecoderFactory<T> @JvmOverloads constructor(
    private val clazz: Class<out T>,
    private val bitmapConfig: android.graphics.Bitmap.Config? = null,
    private val cropBorders: Boolean = false,
    private val borderDetectionConfig: BorderDetectionConfig = BorderDetectionConfig.DEFAULT
) : DecoderFactory<T> {

    @Throws(Exception::class)
    override fun make(): T {
        // Try different constructor signatures in order of preference
        return tryConstructorWithConfigCropAndBorderConfig()
            ?: tryConstructorWithConfigAndCropBorders()
            ?: tryConstructorWithConfig()
            ?: tryDefaultConstructor()
            ?: throw NoSuchMethodException("No suitable constructor found for ${clazz.name}")
    }
    
    /**
     * Try to instantiate with constructor(Config, boolean, BorderDetectionConfig).
     */
    private fun tryConstructorWithConfigCropAndBorderConfig(): T? {
        return try {
            clazz.getConstructor(
                android.graphics.Bitmap.Config::class.java,
                Boolean::class.javaPrimitiveType,
                BorderDetectionConfig::class.java
            ).newInstance(bitmapConfig, cropBorders, borderDetectionConfig)
        } catch (e: NoSuchMethodException) {
            null
        }
    }
    
    /**
     * Try to instantiate with constructor(Config, boolean).
     */
    private fun tryConstructorWithConfigAndCropBorders(): T? {
        return try {
            clazz.getConstructor(
                android.graphics.Bitmap.Config::class.java,
                Boolean::class.javaPrimitiveType
            ).newInstance(bitmapConfig, cropBorders)
        } catch (e: NoSuchMethodException) {
            null
        }
    }
    
    /**
     * Try to instantiate with constructor(Config).
     */
    private fun tryConstructorWithConfig(): T? {
        if (bitmapConfig == null) return null
        return try {
            clazz.getConstructor(android.graphics.Bitmap.Config::class.java).newInstance(bitmapConfig)
        } catch (e: NoSuchMethodException) {
            null
        }
    }
    
    /**
     * Try to instantiate with default constructor.
     */
    private fun tryDefaultConstructor(): T? {
        return try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            null
        }
    }
}
