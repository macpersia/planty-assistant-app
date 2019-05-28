package com.github.macpersia.planty_alexa_android.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.github.macpersia.planty_alexa_android.R

import java.io.IOException
import java.io.InputStream

/**
 * @author will on 5/30/2016.
 */

class SendPrerecordedActionFragment : BaseListenerFragment() {

    internal lateinit var button: View

    protected override val title: String
        get() = getString(R.string.fragment_action_send_prerecorded)

    protected override val rawCode: Int
        get() = R.raw.code_prerecorded

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_action_prerecorded, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button = view!!.findViewById(R.id.button)

        button.setOnClickListener { search() }
    }

    private fun search() {
        try {
            val `is` = activity.assets.open("intros/joke.raw")
            val fileBytes = ByteArray(`is`.available())
            `is`.read(fileBytes)
            `is`.close()
            alexaManager.sendAudioRequest(fileBytes, requestCallback)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun startListening() {

    }
}
