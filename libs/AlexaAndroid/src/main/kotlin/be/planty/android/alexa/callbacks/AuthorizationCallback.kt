package be.planty.android.alexa.callbacks

import com.amazon.identity.auth.device.AuthError

/**
 * A callback to handle three states of Amazon authorization
 */
interface AuthorizationCallback {
    fun onCancel()
    fun onSuccess()
    //fun onError(error: Exception)
    fun onError(error: AuthError?)
}
