package com.willblaschko.android.alexa.interfaces

import java.util.ArrayList

/**
 * Wrapper for the list of [AvsItem] [com.willblaschko.android.alexa.data.Directive]s returned from a post/get sent to the
 * Alexa server. In the future this will contain other metadata associated with the returned response.
 */
class AvsResponse : ArrayList<AvsItem>()
