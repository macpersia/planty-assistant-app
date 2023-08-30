package be.planty.android.alexa.interfaces.system

import be.planty.android.alexa.interfaces.AvsItem

/**
 * Created by will on 4/8/2017.
 */

class AvsSetEndpointItem(token: String, endpoint: String) : AvsItem(token) {
    var endpoint: String
        internal set

    init {
        this.endpoint = endpoint
    }
}
