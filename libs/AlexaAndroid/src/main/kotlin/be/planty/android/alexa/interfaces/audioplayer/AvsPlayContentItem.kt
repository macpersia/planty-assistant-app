package be.planty.android.alexa.interfaces.audioplayer

import android.net.Uri

import be.planty.android.alexa.interfaces.AvsItem

/**
 * Directive to play a local content item, this is not generated from the Alexa servers, this is for local
 * use only.
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsPlayContentItem
/**
 * Create a new local play item
 * @param uri the local URI
 */
(token: String, val uri: Uri) : AvsItem(token)
