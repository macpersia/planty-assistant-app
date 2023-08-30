package be.planty.android.alexa.interfaces.playbackcontrol

import be.planty.android.alexa.interfaces.AvsItem

/**
 * [be.planty.android.alexa.data.Directive] to send a play command to any app playing media
 *
 * This directive doesn't seem applicable to mobile applications
 *
 * @author will on 5/31/2016.
 */

class AvsMediaPlayCommandItem(token: String) : AvsItem(token)
