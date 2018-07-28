package com.willblaschko.android.alexa.interfaces.errors

import com.willblaschko.android.alexa.data.Directive
import com.willblaschko.android.alexa.interfaces.AvsItem

/**
 * Created by will on 6/26/2016.
 */

class AvsResponseException(directive: Directive) : AvsItem(directive.payload.token!!) {
    var directive: Directive
        internal set

    init {
        this.directive = directive
    }
}
