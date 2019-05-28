package com.willblaschko.android.alexa.interfaces.audioplayer

import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem

import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Directive to play a local, returned audio item
 *
 * See: [AvsSpeakItem]
 *
 * [com.willblaschko.android.alexa.data.Directive] response item type parsed so we can properly
 * deal with the incoming commands from the Alexa server.
 *
 * @author will on 5/21/2016.
 */
class AvsPlayAudioItem @Throws(IOException::class)
constructor(token: String, cid: String, audio: ByteArrayInputStream?) : AvsSpeakItem(token, cid, audio)
