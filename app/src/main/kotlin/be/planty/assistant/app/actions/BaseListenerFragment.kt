package be.planty.assistant.app.actions

import android.app.Fragment
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.RawRes
import be.planty.android.alexa.AlexaManager
import be.planty.assistant.app.R
import be.planty.android.alexa.callbacks.AsyncCallback
import be.planty.android.alexa.interfaces.AvsResponse
import be.planty.assistant.app.global.Constants.PRODUCT_ID

/**
 * @author will on 5/30/2016.
 */

abstract class BaseListenerFragment : Fragment() {

    protected lateinit var alexaManager: AlexaManager

    protected val requestCallback: AsyncCallback<AvsResponse, Exception?>?
        get() = if (activity != null && activity is AvsListenerInterface) {
            (activity as AvsListenerInterface).requestCallback
        } else null
    protected abstract val title: String
    @get:RawRes
    protected abstract val rawCode: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //get our AlexaManager instance for convenience
        alexaManager = AlexaManager.getInstance(activity, PRODUCT_ID)

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            activity.title = title
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.code_menu, menu)

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.view_code -> {
                val fragment = DisplayCodeFragment.getInstance(title, rawCode)
                loadFragment(fragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    abstract fun startListening()

    interface AvsListenerInterface {
        val requestCallback: AsyncCallback<AvsResponse, Exception?>
    }

    protected fun loadFragment(fragment: Fragment) {
        if (activity != null && activity is ActionsFragment.ActionFragmentInterface) {
            (activity as ActionsFragment.ActionFragmentInterface).loadFragment(fragment, true)
        }
    }
}
