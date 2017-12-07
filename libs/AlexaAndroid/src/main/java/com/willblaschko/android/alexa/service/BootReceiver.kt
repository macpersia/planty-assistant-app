package com.willblaschko.android.alexa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * @author will on 4/17/2016.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        //start our service in the background
        val stickyIntent = Intent(context, DownChannelService::class.java)
        context.startService(stickyIntent)
        Log.i(TAG, "Started down channel service.")
    }

    companion object {

        private val TAG = "BootReceiver"
    }
}
