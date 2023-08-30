package be.planty.android.alexa.callbacks

import com.amazon.identity.auth.device.AuthError

/**
 * Implemented version of [AsyncCallback] generic
 */
open class ImplAsyncCallback<D, E> : AsyncCallback<D, E> {
    override fun start() {

    }

    override fun success(result: D) {

    }

    override fun failure(error: AuthError?) {

    }

    override fun complete() {

    }
}
