package be.planty.android.alexa.interfaces.audioplayer

import be.planty.android.alexa.interfaces.AvsItem

/**
 * Directive to play a remote URL item
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsPlayRemoteItem(token: String, val url: String, startOffset: Long) : AvsItem(token) {
    private val mStreamId: String? = null
    val startOffset: Long

    init {
        this.startOffset = if (startOffset < 0) 0 else startOffset
    }

}
