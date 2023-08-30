package be.planty.android.alexa.interfaces.speechrecognizer

import be.planty.android.alexa.data.Event
import be.planty.android.alexa.interfaces.SendEvent

import okhttp3.MultipartBody
import okhttp3.RequestBody

/**
 * Abstract class to extend [SendEvent] to automatically add the RequestBody with the correct type
 * and name, as well as the SpeechRecognizer [Event]
 *
 * @author will on 5/21/2016.
 */
abstract class SpeechSendEvent : SendEvent() {

    override val event: String
        get() = Event.speechRecognizerEvent

    protected abstract val requestBody: RequestBody

    override fun addFormDataParts(builder: MultipartBody.Builder) {
        builder.addFormDataPart("audio", "speech.wav", requestBody)
    }
}
