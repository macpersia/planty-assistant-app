package be.planty.android.alexa.data

import android.text.TextUtils

/**
 * A catch-all Directive to classify return responses from the Amazon Alexa v20160207 API
 * Will handle calls to:
 * [Speech Recognizer](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechrecognizer)
 * [Alerts](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/alerts)
 * [Audio Player](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer)
 * [Playback Controller](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/playbackcontroller)
 * [Speaker](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speaker)
 * [Speech Synthesizer](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/speechsynthesizer)
 * [System](https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/system)
 *
 *
 * @author wblaschko on 5/6/16.
 */
class Directive {
    lateinit var header: Header
    lateinit var payload: Payload


    //PLAY BEHAVIORS

    val playBehaviorReplaceAll: Boolean
        get() = TextUtils.equals(payload.playBehavior, PLAY_BEHAVIOR_REPLACE_ALL)
    val playBehaviorEnqueue: Boolean
        get() = TextUtils.equals(payload.playBehavior, PLAY_BEHAVIOR_ENQUEUE)
    val playBehaviorReplaceEnqueued: Boolean
        get() = TextUtils.equals(payload.playBehavior, PLAY_BEHAVIOR_REPLACE_ENQUEUED)

    class Header {
        var namespace: String? = null
            internal set
        var name: String? = null
            internal set
        var messageId: String? = null
            internal set
        var dialogRequestId: String? = null
            internal set
    }

    class Payload {
        lateinit var url: String
            internal set
        lateinit var endpoint: String
            internal set
        lateinit var format: String
            internal set
        lateinit var type: String
            internal set
        lateinit var scheduledTime: String
            internal set
        var playBehavior: String? = null
            internal set
        var audioItem: AudioItem? = null
            internal set
        var volume: Long = 0
            internal set
        var isMute: Boolean = false
            internal set
        var timeoutInMilliseconds: Long = 0
            internal set
        var description: String? = null
            internal set
        lateinit var code: String
            internal set
        var token: String = ""
	    internal set
            get() = if (field.isNotBlank()) field else audioItem?.stream?.token?:""
    }

    class AudioItem {
        var audioItemId: String? = null
            internal set
        var stream: Stream? = null
            internal set
    }

    class Stream {
        //todo progressReport

        var url: String? = null
            internal set
        var streamFormat: String? = null
            internal set
        var offsetInMilliseconds: Long = 0
            internal set
        var expiryTime: String? = null
            internal set
        var token: String? = null
            internal set
        var expectedPreviousToken: String? = null
            internal set
    }

    class DirectiveWrapper {
        var directive: Directive? = null
            internal set
    }

    companion object {

        val TYPE_SPEAK = "Speak"
        val TYPE_PLAY = "Play"
        val TYPE_STOP = "Stop"
        val TYPE_STOP_CAPTURE = "StopCapture"
        val TYPE_SET_ALERT = "SetAlert"
        val TYPE_DELETE_ALERT = "DeleteAlert"
        val TYPE_SET_VOLUME = "SetVolume"
        val TYPE_ADJUST_VOLUME = "AdjustVolume"
        val TYPE_SET_MUTE = "SetMute"
        val TYPE_EXPECT_SPEECH = "ExpectSpeech"
        val TYPE_MEDIA_PLAY = "PlayCommandIssued"
        val TYPE_MEDIA_PAUSE = "PauseCommandIssued"
        val TYPE_MEDIA_NEXT = "NextCommandIssued"
        val TYPE_MEDIA_PREVIOUS = "PreviousCommandIssue"
        val TYPE_EXCEPTION = "Exception"
        val TYPE_SET_ENDPOINT = "SetEndpoint"

        private val PLAY_BEHAVIOR_REPLACE_ALL = "REPLACE_ALL"
        private val PLAY_BEHAVIOR_ENQUEUE = "ENQUEUE"
        private val PLAY_BEHAVIOR_REPLACE_ENQUEUED = "REPLACE_ENQUEUED"
    }
}
