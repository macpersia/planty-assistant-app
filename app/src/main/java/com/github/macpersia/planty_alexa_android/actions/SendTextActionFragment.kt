package com.github.macpersia.planty_alexa_android.actions

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText

import com.github.macpersia.planty_alexa_android.R

/**
 * @author will on 5/30/2016.
 */

class SendTextActionFragment : BaseListenerFragment() {

    internal lateinit var search: EditText
    internal lateinit var button: View

    protected override val title: String
        get() = getString(R.string.fragment_action_send_text)

    protected override val rawCode: Int
        get() = R.raw.code_text

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_action_type, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        search = view!!.findViewById(R.id.search) as EditText
        button = view.findViewById(R.id.button)

        search.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            // If the event is a key-down event on the "enter" button
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // Perform action on key press
                search()
                return@OnKeyListener true
            }
            false
        })

        button.setOnClickListener { search() }
    }

    private fun search() {
        val text = search.text.toString()
        alexaManager.sendTextRequest(text, requestCallback)
    }

    override fun startListening() {
        search.setText("")
        search.requestFocus()
    }
}
