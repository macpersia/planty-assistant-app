package com.willblaschko.android.alexa.interfaces.playbackcontrol

import com.willblaschko.android.alexa.interfaces.AvsItem

/**
 * [com.willblaschko.android.alexa.data.Directive] to send a play command to any app playing media
 *
 * This directive doesn't seem applicable to mobile applications
 *
 * @author will on 5/31/2016.
 */

class AvsMediaPlayCommandItem(token: String) : AvsItem(token)
