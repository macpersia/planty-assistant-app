package com.willblaschko.android.alexa.interfaces.speechrecognizer

import com.willblaschko.android.alexa.interfaces.AvsItem

/**
 * [com.willblaschko.android.alexa.data.Directive] to prompt the user for a speech input
 *
 * [com.willblaschko.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
open class AvsExpectSpeechItem @JvmOverloads constructor(token: String, timeoutInMiliseconds: Long = 2000) : AvsItem(token) {
    var timeoutInMiliseconds: Long = 0
        internal set

    init {
        this.timeoutInMiliseconds = timeoutInMiliseconds
    }
}
