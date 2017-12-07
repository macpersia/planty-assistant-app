package com.willblaschko.android.alexa.interfaces.speechrecognizer

/**
 * Directive to prompt the user for a speech input
 *
 * See: [AvsExpectSpeechItem]
 *
 * [com.willblaschko.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */

@Deprecated("Check for {@link AvsExpectSpeechItem} instead\n" +
        " \n" +
        "  ")
class AvsListenItem @JvmOverloads constructor(token: String, timeoutInMiliseconds: Long = 2000) : AvsExpectSpeechItem(token, timeoutInMiliseconds)
