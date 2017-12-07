package com.willblaschko.android.alexa.callbacks

/**
 * A generic callback to handle four states of asynchronous operations
 */
interface AsyncCallback<D, E> {
    fun start()
    fun success(result: D)
    fun failure(error: E)
    fun complete()
}
