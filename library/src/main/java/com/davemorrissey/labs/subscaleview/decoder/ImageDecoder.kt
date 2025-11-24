package com.davemorrissey.labs.subscaleview.decoder

import android.content.Context
import android.graphics.Bitmap
import com.davemorrissey.labs.subscaleview.provider.InputProvider

/**
 * Interface for full image decoding classes.
 */
interface ImageDecoder {
    /**
     * Initialise the decoder. When possible, perform initial setup work once in this method.
     * The full image bitmap must be returned.
     *
     * @param context  Application context. A reference may be held, but must be cleared on recycle.
     * @param provider Provider of the image.
     * @return The decoded bitmap.
     * @throws Exception if initialisation fails.
     */
    fun init(context: Context, provider: InputProvider): Bitmap
}
