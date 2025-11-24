package com.davemorrissey.labs.subscaleview.decoder

/**
 * Interface for decoder factories.
 * @param <T> The type of decoder to produce.
 */
interface DecoderFactory<T> {
    /**
     * Produce a new instance of the decoder.
     * @return a new instance of the decoder.
     */
    fun make(): T
}
