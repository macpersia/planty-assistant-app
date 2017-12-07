package com.github.macpersia.planty_alexa_android.actions

import android.os.Bundle
import android.support.annotation.RawRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.github.macpersia.planty_alexa_android.R

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * @author will on 5/30/2016.
 */

class DisplayCodeFragment : Fragment() {

    internal var title: String? = null
    internal var rawCode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null) {
            return
        }
        title = arguments.getString(ARG_TITLE)
        rawCode = arguments.getInt(ARG_RAW_CODE)
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            activity.title = title
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_display_code, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val text = view!!.findViewById(R.id.code) as TextView

        val builder = StringBuilder()

        val `in` = resources.openRawResource(rawCode)
        try {
            val reader = BufferedReader(InputStreamReader(`in`))
            var line: String? = reader.readLine()
            while (line != null) {
                builder.append(line).append("\r\n")
                line = reader.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        text.text = builder.toString()

    }

    companion object {

        private val ARG_TITLE = "title"
        private val ARG_RAW_CODE = "raw_code"

        fun getInstance(title: String, @RawRes rawCode: Int): DisplayCodeFragment {
            val b = Bundle()
            b.putString(ARG_TITLE, title)
            b.putInt(ARG_RAW_CODE, rawCode)
            val fragment = DisplayCodeFragment()
            fragment.arguments = b
            return fragment
        }
    }
}
