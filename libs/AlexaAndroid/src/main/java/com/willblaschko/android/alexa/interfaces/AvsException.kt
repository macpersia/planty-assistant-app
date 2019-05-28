package com.willblaschko.android.alexa.interfaces

/**
 * Custom exception type to wrap exceptions thrown by the Alexa server.
 */
class AvsException : Exception {

    constructor() {}

    constructor(message: String) : super(message) {}

    constructor(cause: Throwable) : super(cause) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}


}
