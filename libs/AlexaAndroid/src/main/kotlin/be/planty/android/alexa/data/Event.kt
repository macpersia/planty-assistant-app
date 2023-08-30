package be.planty.android.alexa.data

import com.google.gson.Gson

import java.util.ArrayList

import be.planty.android.alexa.utility.Util.uuid

/**
 * A catch-all Event to classify return responses from the Amazon Alexa v20160207 API
 * Will handle calls to:
 * [Speech Recognizer](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechrecognizer)
 * [Alerts](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/alerts)
 * [Audio Player](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer)
 * [Playback Controller](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/playbackcontroller)
 * [Speaker](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speaker)
 * [Speech Synthesizer](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechsynthesizer)
 * [System](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system)
 *
 * @author wblaschko on 5/6/16.
 */
class Event {

    lateinit var header: Header
    lateinit var payload: Payload
    internal var context: List<Event>? = null


    class Header {

        lateinit var namespace: String
        lateinit var name: String
        var messageId: String? = null
            internal set
        lateinit var dialogRequestId: String
    }

    class Payload {
        internal var token: String? = null
        var profile: String? = null
            internal set
        var format: String? = null
            internal set
        internal var muted: Boolean? = null
        internal var volume: Long? = null
        internal var offsetInMilliseconds: Long? = null


    }

    class EventWrapper {
        var event: Event? = null
            internal set
        var context: List<Event> = ArrayList()
            internal set

        fun toJson(): String {
            return Gson().toJson(this) + "\n"
        }
    }

    class Builder {
        internal var event = Event()
        internal var payload = Payload()
        internal var header = Header()
        internal var context: List<Event> = ArrayList()

        init {
            event.payload = payload
            event.header = header
        }

        fun build(): EventWrapper {
            val wrapper = EventWrapper()
            wrapper.event = event

            if (context != null && !context.isEmpty() && !(context.size == 1 && context[0] == null)) {
                wrapper.context = context
            }

            return wrapper
        }

        fun toJson(): String {
            return build().toJson()
        }

        fun setContext(context: List<Event>?): Builder {
            if (context == null) {
                return this
            }
            this.context = context
            return this
        }

        fun setHeaderNamespace(namespace: String): Builder {
            header.namespace = namespace
            return this
        }

        fun setHeaderName(name: String): Builder {
            header.name = name
            return this
        }

        fun setHeaderMessageId(messageId: String): Builder {
            header.messageId = messageId
            return this
        }

        fun setHeaderDialogRequestId(dialogRequestId: String): Builder {
            header.dialogRequestId = dialogRequestId
            return this
        }

        fun setPayloadProfile(profile: String): Builder {
            payload.profile = profile
            return this
        }

        fun setPayloadFormat(format: String): Builder {
            payload.format = format
            return this
        }

        fun setPayloadMuted(muted: Boolean): Builder {
            payload.muted = muted
            return this
        }

        fun setPayloadVolume(volume: Long): Builder {
            payload.volume = volume
            return this
        }

        fun setPayloadToken(token: String): Builder {
            payload.token = token
            return this
        }

        fun setPlayloadOffsetInMilliseconds(offsetInMilliseconds: Long): Builder {
            payload.offsetInMilliseconds = offsetInMilliseconds
            return this
        }
    }

    companion object {

        val speechRecognizerEvent: String
            get() {
                val builder = Builder()
                builder.setHeaderNamespace("SpeechRecognizer")
                        .setHeaderName("Recognize")
                        .setHeaderMessageId(uuid)
                        .setHeaderDialogRequestId("dialogRequest-321")
                        .setPayloadFormat("AUDIO_L16_RATE_16000_CHANNELS_1")
                        .setPayloadProfile("NEAR_FIELD")
                return builder.toJson()
            }

        fun getVolumeChangedEvent(volume: Long, isMute: Boolean): String {
            val builder = Builder()
            builder.setHeaderNamespace("Speaker")
                    .setHeaderName("VolumeChanged")
                    .setHeaderMessageId(uuid)
                    .setPayloadVolume(volume)
                    .setPayloadMuted(isMute)
            return builder.toJson()
        }

        fun getMuteEvent(isMute: Boolean): String {
            val builder = Builder()
            builder.setHeaderNamespace("Speaker")
                    .setHeaderName("VolumeChanged")
                    .setHeaderMessageId(uuid)
                    .setPayloadMuted(isMute)
            return builder.toJson()
        }

        val expectSpeechTimedOutEvent: String
            get() {
                val builder = Builder()
                builder.setHeaderNamespace("SpeechRecognizer")
                        .setHeaderName("ExpectSpeechTimedOut")
                        .setHeaderMessageId(uuid)
                return builder.toJson()
            }

        fun getSpeechNearlyFinishedEvent(token: String, offsetInMilliseconds: Long): String {
            val builder = Builder()
            builder.setHeaderNamespace("SpeechSynthesizer")
                    .setHeaderName("PlaybackNearlyFinished")
                    .setHeaderMessageId(uuid)
                    .setPayloadToken(token)
                    .setPlayloadOffsetInMilliseconds(offsetInMilliseconds)
            return builder.toJson()
        }

        fun getPlaybackNearlyFinishedEvent(token: String, offsetInMilliseconds: Long): String {
            val builder = Builder()
            builder.setHeaderNamespace("AudioPlayer")
                    .setHeaderName("PlaybackNearlyFinished")
                    .setHeaderMessageId(uuid)
                    .setPayloadToken(token)
                    .setPlayloadOffsetInMilliseconds(offsetInMilliseconds)
            return builder.toJson()
        }

        val playbackControllerPlayCommandIssued: String
            get() {
                val builder = Builder()
                builder.setHeaderNamespace("PlaybackController")
                        .setHeaderName("PlayCommandIssued")
                        .setHeaderMessageId(uuid)
                return builder.toJson()
            }

        val playbackControllerPauseCommandIssued: String
            get() {
                val builder = Builder()
                builder.setHeaderNamespace("PlaybackController")
                        .setHeaderName("PauseCommandIssued")
                        .setHeaderMessageId(uuid)
                return builder.toJson()
            }

        val playbackControllerNextCommandIssued: String
            get() {
                val builder = Builder()
                builder.setHeaderNamespace("PlaybackController")
                        .setHeaderName("NextCommandIssued")
                        .setHeaderMessageId(uuid)
                return builder.toJson()
            }

        val playbackControllerPreviousCommandIssued: String
            get() {
                val builder = Builder()
                builder.setHeaderNamespace("PlaybackController")
                        .setHeaderName("PreviousCommandIssued")
                        .setHeaderMessageId(uuid)
                return builder.toJson()
            }

        fun getSetAlertSucceededEvent(token: String): String {
            return getAlertEvent(token, "SetAlertSucceeded")
        }

        fun getSetAlertFailedEvent(token: String): String {
            return getAlertEvent(token, "SetAlertFailed")
        }

        fun getDeleteAlertSucceededEvent(token: String): String {
            return getAlertEvent(token, "DeleteAlertSucceeded")
        }

        fun getDeleteAlertFailedEvent(token: String): String {
            return getAlertEvent(token, "DeleteAlertFailed")
        }

        fun getAlertStartedEvent(token: String): String {
            return getAlertEvent(token, "AlertStarted")
        }

        fun getAlertStoppedEvent(token: String): String {
            return getAlertEvent(token, "AlertStopped")
        }

        fun getAlertEnteredForegroundEvent(token: String): String {
            return getAlertEvent(token, "AlertEnteredForeground")
        }

        fun getAlertEnteredBackgroundEvent(token: String): String {
            return getAlertEvent(token, "AlertEnteredBackground")
        }

        private fun getAlertEvent(token: String, type: String): String {
            val builder = Builder()
            builder.setHeaderNamespace("Alerts")
                    .setHeaderName(type)
                    .setHeaderMessageId(uuid)
                    .setPayloadToken(token)
            return builder.toJson()
        }

        fun getSpeechStartedEvent(token: String): String {
            val builder = Builder()
            builder.setHeaderNamespace("SpeechSynthesizer")
                    .setHeaderName("SpeechStarted")
                    .setHeaderMessageId(uuid)
                    .setPayloadToken(token)
            return builder.toJson()
        }

        fun getSpeechFinishedEvent(token: String): String {
            val builder = Builder()
            builder.setHeaderNamespace("SpeechSynthesizer")
                    .setHeaderName("SpeechFinished")
                    .setHeaderMessageId(uuid)
                    .setPayloadToken(token)
            return builder.toJson()
        }


        fun getPlaybackStartedEvent(token: String, offset: Long): String {
            val builder = Builder()
            builder.setHeaderNamespace("AudioPlayer")
                    .setHeaderName("PlaybackStarted")
                    .setPlayloadOffsetInMilliseconds(offset)
                    .setHeaderMessageId(uuid)
                    .setPayloadToken(token)
            return builder.toJson()
        }

        fun getPlaybackFinishedEvent(token: String): String {
            val builder = Builder()
            builder.setHeaderNamespace("AudioPlayer")
                    .setHeaderName("PlaybackFinished")
                    .setHeaderMessageId(uuid)
                    .setPayloadToken(token)
            return builder.toJson()
        }


        val synchronizeStateEvent: String
            get() {
                val builder = Builder()
                builder.setHeaderNamespace("System")
                        .setHeaderName("SynchronizeState")
                        .setHeaderMessageId(uuid)
                return builder.toJson()
            }
    }


}


