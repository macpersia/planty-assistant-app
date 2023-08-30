package be.planty.android.alexa.interfaces.speechrecognizer

import be.planty.android.alexa.interfaces.AvsItem

/**
 * [be.planty.android.alexa.data.Directive] to prompt the user for a speech input
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
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
