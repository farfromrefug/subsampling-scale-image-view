package com.davemorrissey.labs.subscaleview.decoder

/**
 * Compatibility factory to create decoders from a class object,
 * mimicking the original library behavior for Java compatibility.
 */
class CompatDecoderFactory<T> @JvmOverloads constructor(
    private val clazz: Class<out T>,
    private val bitmapConfig: android.graphics.Bitmap.Config? = null,
    private val cropBorders: Boolean = false
) : DecoderFactory<T> {

    @Throws(Exception::class)
    override fun make(): T {
        return when {
            // Try constructor with both bitmapConfig and cropBorders
            bitmapConfig != null || cropBorders -> {
                try {
                    clazz.getConstructor(
                        android.graphics.Bitmap.Config::class.java,
                        Boolean::class.javaPrimitiveType
                    ).newInstance(bitmapConfig, cropBorders)
                } catch (e: NoSuchMethodException) {
                    // Try constructor with just bitmapConfig
                    try {
                        clazz.getConstructor(android.graphics.Bitmap.Config::class.java).newInstance(bitmapConfig)
                    } catch (e2: NoSuchMethodException) {
                        // Fallback to default constructor
                        clazz.getDeclaredConstructor().newInstance()
                    }
                }
            }
            else -> {
                clazz.getDeclaredConstructor().newInstance()
            }
        }
    }
}
