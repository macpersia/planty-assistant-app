package be.planty.android.alexa.interfaces.speaker

import be.planty.android.alexa.interfaces.AvsItem

/**
 * Directive to set the device mute state
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsSetMuteItem
/**
 * Create a new AdjustVolume [be.planty.android.alexa.data.Directive]
 * @param mute whether the device should be mute upon parsing the directive.
 */
(token: String, mute: Boolean) : AvsItem(token) {
    var isMute: Boolean = false
        internal set

    init {
        this.isMute = mute
    }
}
