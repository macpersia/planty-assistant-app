package be.planty.android.alexa.requestbody

import okhttp3.MediaType
import okhttp3.RequestBody

/**
 * An implemented class that automatically fills in the required MediaType for the [RequestBody] that is sent
 * in the [be.planty.android.alexa.interfaces.SendEvent] class.
 *
 * @author will on 5/28/2016.
 */
abstract class DataRequestBody : RequestBody() {
    override fun contentType(): MediaType? {
        return MediaType.parse("application/octet-stream")
    }
}
