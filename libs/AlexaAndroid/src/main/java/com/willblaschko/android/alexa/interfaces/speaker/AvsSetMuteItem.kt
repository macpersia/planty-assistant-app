package com.willblaschko.android.alexa.interfaces.speaker

import com.willblaschko.android.alexa.interfaces.AvsItem

/**
 * Directive to set the device mute state
 *
 * [com.willblaschko.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsSetMuteItem
/**
 * Create a new AdjustVolume [com.willblaschko.android.alexa.data.Directive]
 * @param mute whether the device should be mute upon parsing the directive.
 */
(token: String, mute: Boolean) : AvsItem(token) {
    var isMute: Boolean = false
        internal set

    init {
        this.isMute = mute
    }
}
