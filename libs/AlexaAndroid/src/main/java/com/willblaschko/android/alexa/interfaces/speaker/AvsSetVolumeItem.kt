package com.willblaschko.android.alexa.interfaces.speaker

import com.willblaschko.android.alexa.interfaces.AvsItem

/**
 * Directive to set the device volume
 *
 * [com.willblaschko.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsSetVolumeItem
/**
 * Create a new AdjustVolume [com.willblaschko.android.alexa.data.Directive]
 * @param volume the requested volume, 0-100 scale (requested as 1-10 by the user)
 */
(token: String, volume: Long) : AvsItem(token) {
    /**
     * Get the [com.willblaschko.android.alexa.data.Directive]'s requested volume
     * @return the requested volume, 0-100 scale (requested as 1-10 by the user), this
     * value needs to be adjusted to the local device's min/max
     */
    var volume: Long = 0
        internal set

    init {
        this.volume = volume
    }
}
