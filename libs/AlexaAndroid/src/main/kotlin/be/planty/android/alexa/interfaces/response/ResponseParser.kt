package be.planty.android.alexa.interfaces.response

import android.util.Log

import com.google.gson.Gson
import com.google.gson.JsonParseException
import be.planty.android.alexa.data.Directive
import be.planty.android.alexa.interfaces.AvsException
import be.planty.android.alexa.interfaces.AvsItem
import be.planty.android.alexa.interfaces.AvsResponse
import be.planty.android.alexa.interfaces.alerts.AvsDeleteAlertItem
import be.planty.android.alexa.interfaces.alerts.AvsSetAlertItem
import be.planty.android.alexa.interfaces.audioplayer.AvsPlayAudioItem
import be.planty.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem
import be.planty.android.alexa.interfaces.errors.AvsResponseException
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem
import be.planty.android.alexa.interfaces.playbackcontrol.AvsStopItem
import be.planty.android.alexa.interfaces.speaker.AvsAdjustVolumeItem
import be.planty.android.alexa.interfaces.speaker.AvsSetMuteItem
import be.planty.android.alexa.interfaces.speaker.AvsSetVolumeItem
import be.planty.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem
import be.planty.android.alexa.interfaces.speechrecognizer.AvsStopCaptureItem
import be.planty.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem
import be.planty.android.alexa.interfaces.system.AvsSetEndpointItem

import org.apache.commons.fileupload.MultipartStream
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

import okhttp3.Headers
import okhttp3.Response

import okhttp3.internal.Util.UTF_8

/**
 * Static helper class to parse incoming responses from the Alexa server and generate a corresponding
 * [AvsResponse] item with all the directives matched to their audio streams.
 *
 * @author will on 5/21/2016.
 */
object ResponseParser {

    val TAG = "ResponseParser"

    private val PATTERN = Pattern.compile("<(.*?)>")

    @Throws(IOException::class, IllegalStateException::class, AvsException::class)
    @JvmOverloads
    fun parseResponse(stream: InputStream, boundary: String, checkBoundary: Boolean = false): AvsResponse {
        val start = System.currentTimeMillis()

        val directives = ArrayList<Directive>()
        val audio = HashMap<String, ByteArrayInputStream>()

        var bytes: ByteArray
        try {
            bytes = IOUtils.toByteArray(stream)
        } catch (exp: IOException) {
            exp.printStackTrace()
            Log.e(TAG, "Error copying bytes[]")
            return AvsResponse()
        }

        var responseString = string(bytes)
        Log.i(TAG, responseString)
        if (checkBoundary) {
            val responseTrim = responseString.trim { it <= ' ' }
            val testBoundary = "--" + boundary
            if (!StringUtils.isEmpty(responseTrim) && StringUtils.endsWith(responseTrim, testBoundary) && !StringUtils.startsWith(responseTrim, testBoundary)) {
                responseString = "--" + boundary + "\r\n" + responseString
                bytes = responseString.toByteArray()
            }
        }

        val mpStream = MultipartStream(ByteArrayInputStream(bytes), boundary.toByteArray(), 100000, null)

        //have to do this otherwise mpStream throws an exception
        if (mpStream.skipPreamble()) {
            Log.i(TAG, "Found initial boundary: true")

            //we have to use the count hack here because otherwise readBoundary() throws an exception
            var count = 0
            while (count < 1 || mpStream.readBoundary()) {
                val headers: String
                try {
                    headers = mpStream.readHeaders()
                } catch (exp: MultipartStream.MalformedStreamException) {
                    break
                }

                val data = ByteArrayOutputStream()
                mpStream.readBodyData(data)
                if (!isJson(headers)) {
                    // get the audio data
                    //convert our multipart into byte data
                    val contentId = getCID(headers)
                    if (contentId != null) {
                        val matcher = PATTERN.matcher(contentId)
                        if (matcher.find()) {
                            val currentId = "cid:" + matcher.group(1)
                            audio.put(currentId, ByteArrayInputStream(data.toByteArray()))
                        }
                    }
                } else {
                    // get the json directive
                    val directive = data.toString(Charset.defaultCharset().displayName())

                    directives.add(getDirective(directive))
                }
                count++
            }

        } else {
            Log.i(TAG, "Response Body: \n" + string(bytes))
            try {
                directives.add(getDirective(responseString))
            } catch (e: JsonParseException) {
                e.printStackTrace()
                throw AvsException("Response from Alexa server malformed. ")
            }

        }

        val response = AvsResponse()

        for (directive in directives) {

            if (directive.playBehaviorReplaceAll) {
                response.add(0, AvsReplaceAllItem(directive.payload.token))
            }
            if (directive.playBehaviorReplaceEnqueued) {
                response.add(AvsReplaceEnqueuedItem(directive.payload.token))
            }

            val item = parseDirective(directive, audio)

            if (item != null) {
                response.add(item)
            }
        }

        Log.i(TAG, "Parsing response took: " + (System.currentTimeMillis() - start) + " size is " + response.size)

        if (response.size == 0) {
            Log.i(TAG, string(bytes))
        }

        return response
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun parseDirective(directive: Directive, audio: HashMap<String, ByteArrayInputStream>? = null): AvsItem? {
        Log.i(TAG, "Parsing directive type: " + directive.header.namespace + ":" + directive.header.name)
        when (directive.header.name) {
            Directive.TYPE_SPEAK -> {
                val cid = directive.payload.url
                return AvsSpeakItem(directive.payload.token, cid, audio!![cid])
            }
            Directive.TYPE_PLAY -> {
                val url = directive.payload.audioItem?.stream?.url
                return if (url!!.contains("cid:")) {
                    AvsPlayAudioItem(directive.payload.token, url, audio!![url])
                } else {
                    AvsPlayRemoteItem(directive.payload.token, url, directive.payload.audioItem?.stream!!.offsetInMilliseconds)
                }
            }
            Directive.TYPE_STOP_CAPTURE -> return AvsStopCaptureItem(directive.payload.token)
            Directive.TYPE_STOP -> return AvsStopItem(directive.payload.token)
            Directive.TYPE_SET_ALERT -> return AvsSetAlertItem(directive.payload.token, directive.payload.type, directive.payload.scheduledTime)
            Directive.TYPE_DELETE_ALERT -> return AvsDeleteAlertItem(directive.payload.token)
            Directive.TYPE_SET_MUTE -> return AvsSetMuteItem(directive.payload.token, directive.payload.isMute)
            Directive.TYPE_SET_VOLUME -> return AvsSetVolumeItem(directive.payload.token, directive.payload.volume)
            Directive.TYPE_ADJUST_VOLUME -> return AvsAdjustVolumeItem(directive.payload.token, directive.payload.volume)
            Directive.TYPE_EXPECT_SPEECH -> return AvsExpectSpeechItem(directive.payload.token, directive.payload.timeoutInMilliseconds)
            Directive.TYPE_MEDIA_PLAY -> return AvsMediaPlayCommandItem(directive.payload.token)
            Directive.TYPE_MEDIA_PAUSE -> return AvsMediaPauseCommandItem(directive.payload.token)
            Directive.TYPE_MEDIA_NEXT -> return AvsMediaNextCommandItem(directive.payload.token)
            Directive.TYPE_MEDIA_PREVIOUS -> return AvsMediaPreviousCommandItem(directive.payload.token)
            Directive.TYPE_SET_ENDPOINT -> return AvsSetEndpointItem(directive.payload.token, directive.payload.endpoint)
            Directive.TYPE_EXCEPTION -> return AvsResponseException(directive)
            else -> {
                Log.e(TAG, "Unknown type found")
                return null
            }
        }
    }

    @Throws(IOException::class)
    fun getBoundary(response: Response): String {
        val headers = response.headers()
        val header = headers.get("content-type")
        var boundary = ""

        if (header != null) {
            val pattern = Pattern.compile("boundary=(.*?);")
            val matcher = pattern.matcher(header)
            if (matcher.find()) {
                boundary = matcher.group(1)
            }
        } else {
            Log.i(TAG, "Body: " + response.body()?.string())
        }
        return boundary
    }


    @Throws(IOException::class)
    private fun string(bytes: ByteArray): String {
        return String(bytes, UTF_8)
    }

    /**
     * Parse our directive using Gson into an object
     * @param directive the string representation of our JSON object
     * @return the reflected directive
     */
    @Throws(AvsException::class, IllegalStateException::class)
    fun getDirective(directive: String): Directive {
        Log.i(TAG, directive)
        val gson = Gson()
        val wrapper = gson.fromJson<Directive.DirectiveWrapper>(directive, Directive.DirectiveWrapper::class.java)
        return if (wrapper.directive == null) {
            gson.fromJson<Directive>(directive, Directive::class.java)
        } else wrapper.directive!!
    }


    /**
     * Get the content id from the return headers from the AVS server
     * @param headers the return headers from the AVS server
     * @return a string form of our content id
     */
    @Throws(IOException::class)
    private fun getCID(headers: String): String? {
        val contentString = "Content-ID:"
        val reader = BufferedReader(StringReader(headers))
        var line: String? = reader.readLine()
        while (line != null) {
            if (line.startsWith(contentString)) {
                return line.substring(contentString.length).trim({ it <= ' ' })
            }
            line = reader.readLine()
        }
        return null
    }

    /**
     * Check if the response is JSON (a validity check)
     * @param headers the return headers from the AVS server
     * @return true if headers state the response is JSON, false otherwise
     */
    private fun isJson(headers: String): Boolean {
        return headers.contains("application/json")
    }
}
/**
 * Get the AvsItem associated with a Alexa API post/get, this will contain a list of [AvsItem] directives,
 * if applicable.
 *
 * Includes hacky work around for PausePrompt items suggested by Eric@Amazon
 * @see [Forum Discussion](https://forums.developer.amazon.com/questions/28021/response-about-the-shopping-list.html)
 *
 *
 * @param stream the input stream as a result of our  OkHttp post/get calls
 * @param boundary the boundary we're using to separate the multiparts
 * @return the parsed AvsResponse
 * @throws IOException
 */
