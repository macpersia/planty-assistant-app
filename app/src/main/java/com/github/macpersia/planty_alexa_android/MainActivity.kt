package com.github.macpersia.planty_alexa_android

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.view.View
import android.widget.TextView

import com.github.macpersia.planty_alexa_android.actions.ActionsFragment
import com.github.macpersia.planty_alexa_android.actions.BaseListenerFragment

import com.github.macpersia.planty_alexa_android.R.id.frame

/**
 * Our main launch activity where we can change settings, see about, etc.
 */
class MainActivity : BaseActivity(), ActionsFragment.ActionFragmentInterface, FragmentManager.OnBackStackChangedListener {

    private var statusBar: View? = null
    private var status: TextView? = null
    private var loading: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        //Listen for changes in the back stack
        supportFragmentManager.addOnBackStackChangedListener(this)
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp()

        statusBar = findViewById(R.id.status_bar)
        status = findViewById(R.id.status) as TextView?
        loading = findViewById(R.id.loading)

        val fragment = ActionsFragment()
        loadFragment(fragment, false)
    }

    override fun startListening() {
        val fragment = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT)
        if (fragment != null && fragment.isVisible) {
            // add your code here
            (fragment as? BaseListenerFragment)?.startListening()
        }
    }

    override fun loadFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(frame, fragment, TAG_FRAGMENT)
        if (addToBackStack) {
            transaction.addToBackStack(fragment.javaClass.getSimpleName())
        }
        transaction.commit()
    }

    override fun stateListening() {

        if (status != null) {
            status!!.setText(R.string.status_listening)
            loading!!.visibility = View.GONE
            statusBar!!.animate().alpha(1f)
        }
    }

    override fun stateProcessing() {
        if (status != null) {
            status!!.setText(R.string.status_processing)
            loading!!.visibility = View.VISIBLE
            statusBar!!.animate().alpha(1f)
        }
    }

    override fun stateSpeaking() {
        if (status != null) {
            status!!.setText(R.string.status_speaking)
            loading!!.visibility = View.VISIBLE
            statusBar!!.animate().alpha(1f)
        }
    }

    override fun statePrompting() {
        if (status != null) {
            status!!.text = ""
            loading!!.visibility = View.VISIBLE
            statusBar!!.animate().alpha(1f)
        }
    }

    override fun stateFinished() {
        if (status != null) {
            status!!.text = ""
            loading!!.visibility = View.GONE
            statusBar!!.animate().alpha(0f)
        }
    }

    override fun stateNone() {
        statusBar!!.animate().alpha(0f)
    }

    override fun onBackStackChanged() {
        shouldDisplayHomeUp()
    }

    fun shouldDisplayHomeUp() {
        //Enable Up button only  if there are entries in the back stack
        val canback = supportFragmentManager.backStackEntryCount > 0
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(canback)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        //This method is called when the up button is pressed. Just the pop back stack.
        supportFragmentManager.popBackStack()
        return true
    }

    companion object {
        private val TAG = "MainActivity"
        private val TAG_FRAGMENT = "CurrentFragment"
    }
}
