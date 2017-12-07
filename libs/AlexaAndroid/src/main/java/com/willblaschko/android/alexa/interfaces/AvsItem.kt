package com.willblaschko.android.alexa.interfaces

/**
 * @author wblaschko on 8/13/15.
 */
abstract class AvsItem(token: String) {
    var token: String
        internal set

    init {
        this.token = token
    }
}
