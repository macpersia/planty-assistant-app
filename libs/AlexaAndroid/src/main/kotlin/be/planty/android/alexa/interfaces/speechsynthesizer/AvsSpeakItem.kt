package be.planty.android.alexa.interfaces.speechsynthesizer

import be.planty.android.alexa.interfaces.AvsItem

import org.apache.commons.io.IOUtils

import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Directive to play a local, returned audio item from the Alexa post/get response
 *
 * [be.planty.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
open class AvsSpeakItem(token: String, val cid: String, val audio: ByteArray) : AvsItem(token) {

    @Throws(IOException::class)
    constructor(token: String, cid: String, audio: ByteArrayInputStream?) : this(token, cid, IOUtils.toByteArray(audio)) {
        audio!!.close()
    }
}
