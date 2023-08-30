package be.planty.android.alexa.interfaces.speaker

import be.planty.android.alexa.interfaces.AvsItem

/**
 * Directive to set the device volume
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsSetVolumeItem
/**
 * Create a new AdjustVolume [be.planty.android.alexa.data.Directive]
 * @param volume the requested volume, 0-100 scale (requested as 1-10 by the user)
 */
(token: String, volume: Long) : AvsItem(token) {
    /**
     * Get the [be.planty.android.alexa.data.Directive]'s requested volume
     * @return the requested volume, 0-100 scale (requested as 1-10 by the user), this
     * value needs to be adjusted to the local device's min/max
     */
    var volume: Long = 0
        internal set

    init {
        this.volume = volume
    }
}
