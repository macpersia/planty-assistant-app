package be.planty.android.alexa.callbacks

import com.amazon.identity.auth.device.AuthError

/**
 * Implemented version of [AuthorizationCallback]
 */
class ImplAuthorizationCallback : AuthorizationCallback {

    override fun onCancel() {

    }

    override fun onSuccess() {

    }

    //override fun onError(error: Exception) {
    override fun onError(error: AuthError?) {
    }
}
