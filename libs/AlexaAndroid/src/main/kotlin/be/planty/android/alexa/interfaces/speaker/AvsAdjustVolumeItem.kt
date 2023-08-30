package be.planty.android.alexa.interfaces.speaker

import be.planty.android.alexa.interfaces.AvsItem

/**
 * Directive to adjust the device volume
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsAdjustVolumeItem
/**
 * Create a new AdjustVolume [be.planty.android.alexa.data.Directive]
 * @param adjustment the direction and amount of adjustment (1, -1).
 */
(token: String, val adjustment: Long) : AvsItem(token)
