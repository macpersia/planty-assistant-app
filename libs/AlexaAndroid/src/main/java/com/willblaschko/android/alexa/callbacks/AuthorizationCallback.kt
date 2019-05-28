package com.willblaschko.android.alexa.callbacks

/**
 * A callback to handle three states of Amazon authorization
 */
interface AuthorizationCallback {
    fun onCancel()
    fun onSuccess()
    fun onError(error: Exception)
}
