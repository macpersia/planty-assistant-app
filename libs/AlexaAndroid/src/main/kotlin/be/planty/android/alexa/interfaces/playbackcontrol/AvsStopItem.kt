package be.planty.android.alexa.interfaces.playbackcontrol

import be.planty.android.alexa.interfaces.AvsItem

/**
 * Directive to stop device playback
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsStopItem(token: String) : AvsItem(token)
