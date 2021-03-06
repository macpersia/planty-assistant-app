package com.github.macpersia.planty_alexa_android.actions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.macpersia.planty_alexa_android.BuildConfig
import com.github.macpersia.planty_alexa_android.R
import com.willblaschko.android.alexa.requestbody.DataRequestBody
import com.willblaschko.android.recorderview.RecorderView
import ee.ioc.phon.android.speechutils.RawAudioRecorder
import okio.BufferedSink
import java.io.IOException


/**
 * @author will on 5/30/2016.
 */

class SendAudioActionFragment : BaseListenerFragment() {
    private var recorder: RawAudioRecorder? = null
    private var recorderView: RecorderView? = null

    private val requestBody = object : DataRequestBody() {
        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink?) {
            while (recorder != null && !recorder!!.isPausing) {
                if (recorder != null) {
                    val rmsdb = recorder!!.rmsdb
                    if (recorderView != null) {
                        recorderView!!.post { recorderView!!.setRmsdbLevel(rmsdb) }
                    }
                    if (sink != null && recorder != null) {
                        sink.write(recorder!!.consumeRecording())
                    }
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "Received audio")
                        Log.i(TAG, "RMSDB: " + rmsdb)
                    }
                }

                try {
                    Thread.sleep(25)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
            stopListening()
        }

    }

    protected override val title: String
        get() = getString(R.string.fragment_action_send_audio)

    protected override val rawCode: Int
        get() = R.raw.code_audio

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_action_audio, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recorderView = view!!.findViewById(R.id.recorder) as RecorderView
        recorderView!!.setOnClickListener {
            if (recorder == null) {
                startListening()
            } else {
                stopListening()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    activity.fragmentManager.beginTransaction().remove(this).commit()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        //tear down our recorder on stop
        if (recorder != null) {
            recorder!!.stop()
            recorder!!.release()
            recorder = null
        }
    }

    override fun startListening() {
        if (recorder == null) {
            recorder = RawAudioRecorder(sampleRate = AUDIO_RATE)
        }
        recorder!!.start()
        alexaManager.sendAudioRequest(requestBody, requestCallback)
    }

    private fun stopListening() {
        if (recorder != null) {
            recorder!!.stop()
            recorder!!.release()
            recorder = null
        }
    }

    companion object {

        private val TAG = "SendAudioActionFragment"

        private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
        private val AUDIO_RATE = 16000
    }


}
