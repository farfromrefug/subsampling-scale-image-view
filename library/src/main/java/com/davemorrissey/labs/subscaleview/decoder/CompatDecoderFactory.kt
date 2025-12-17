package com.davemorrissey.labs.subscaleview.decoder

/**
 * Compatibility factory to create decoders from a class object,
 * mimicking the original library behavior for Java compatibility.
 */
class CompatDecoderFactory<T> @JvmOverloads constructor(
    private val clazz: Class<out T>,
    private val bitmapConfig: android.graphics.Bitmap.Config? = null
) : DecoderFactory<T> {

    @Throws(Exception::class)
    override fun make(): T {
        return if (bitmapConfig == null) {
            clazz.getDeclaredConstructor().newInstance()
        } else {
            try {
                clazz.getConstructor(android.graphics.Bitmap.Config::class.java).newInstance(bitmapConfig)
            } catch (e: NoSuchMethodException) {
                // Fallback to default constructor if config constructor not found
                clazz.getDeclaredConstructor().newInstance()
            }
        }
    }
}
