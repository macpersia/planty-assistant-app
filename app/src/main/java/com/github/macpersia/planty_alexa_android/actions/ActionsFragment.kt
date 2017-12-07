package com.github.macpersia.planty_alexa_android.actions

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.github.macpersia.planty_alexa_android.R
import com.github.macpersia.planty_alexa_android.actions.adapter.ActionFragmentAdapter

import java.util.ArrayList

/**
 * @author will on 5/30/2016.
 */

class ActionsFragment : BaseListenerFragment() {

    protected override val title: String
        get() = getString(R.string.app_name)

    protected override val rawCode: Int
        get() = R.raw.code_base

    private val items: List<ActionFragmentAdapter.ActionFragmentItem>
        get() {
            val items = ArrayList<ActionFragmentAdapter.ActionFragmentItem>()

            items.add(ActionFragmentAdapter.ActionFragmentItem(getString(R.string.fragment_action_send_audio),
                    R.drawable.ic_stat_microphone,
                    View.OnClickListener { loadFragment(SendAudioActionFragment()) }))

            items.add(ActionFragmentAdapter.ActionFragmentItem(getString(R.string.fragment_action_send_prerecorded),
                    android.R.drawable.ic_menu_compass,
                    View.OnClickListener { loadFragment(SendPrerecordedActionFragment()) }))
            items.add(ActionFragmentAdapter.ActionFragmentItem(getString(R.string.fragment_action_send_text),
                    android.R.drawable.ic_menu_edit,
                    View.OnClickListener { loadFragment(SendTextActionFragment()) }))

            return items
        }


    override fun startListening() {

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_actions, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view!!.findViewById(R.id.recycler) as RecyclerView
        val adapter = ActionFragmentAdapter(items)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    }


    interface ActionFragmentInterface {
        fun loadFragment(fragment: Fragment, addToBackstack: Boolean)
    }
}
