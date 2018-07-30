package com.github.macpersia.planty_alexa_android

import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager.getToken
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult
import com.amazon.identity.auth.device.api.authorization.ProfileScope
import com.amazon.identity.auth.device.api.authorization.Scope
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder
import com.amazonaws.services.lambda.model.InvokeRequest
import com.github.macpersia.planty_alexa_android.R.id.frame
import com.github.macpersia.planty_alexa_android.actions.ActionsFragment
import com.github.macpersia.planty_alexa_android.actions.BaseListenerFragment


/**
 * Our main launch activity where we can change settings, see about, etc.
 */
class MainActivity : BaseActivity(), ActionsFragment.ActionFragmentInterface,
                     FragmentManager.OnBackStackChangedListener {

    private var statusBar: View? = null
    private var status: TextView? = null
    private var loading: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        //Listen for changes in the back stack
        fragmentManager.addOnBackStackChangedListener(this)
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp()

        statusBar = findViewById(R.id.status_bar)
        status = findViewById(R.id.status) as TextView?
        loading = findViewById(R.id.loading)

        val fragment = ActionsFragment()
        loadFragment(fragment, false)

        val scopes = arrayOf<Scope>(ProfileScope.profile()/*, ProfileScope.postalCode()*/)
        getToken(this.status?.context, scopes, object : Listener<AuthorizeResult, AuthError> {
            override fun onSuccess(result: AuthorizeResult) {
                val accessToken = result.accessToken
                if (accessToken != null) {
                    sendAccessTokenAsEvent(status?.context!!, accessToken)
                } else {
                    /* The user is not signed in */
                }
            }
            override fun onError(ae: AuthError) {
                /* The user is not signed in */
            }
        })
    }

    override fun startListening() {
        val fragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT)
        if (fragment != null && fragment.isVisible) {
            // add your code here
            (fragment as? BaseListenerFragment)?.startListening()
        }
    }

    override fun loadFragment(fragment: Fragment, addToBackstack: Boolean) {
        val system = Resources.getSystem()
        val transaction = fragmentManager
                .beginTransaction()
                .setCustomAnimations(
//                        android.R.anim.slide_in_left,
//                        android.R.anim.slide_out_right,
//                        android.R.anim.fade_in,
//                        android.R.anim.fade_out
                        android.R.animator.fade_in,
                        android.R.animator.fade_out
                ).replace(frame, fragment, TAG_FRAGMENT)

        if (addToBackstack) {
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
        val canback = fragmentManager.backStackEntryCount > 0
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(canback)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        //This method is called when the up button is pressed. Just the pop back stack.
        fragmentManager.popBackStack()
        return true
    }

    companion object {
        private val TAG = MainActivity.javaClass.simpleName
        private val TAG_FRAGMENT = "CurrentFragment"

        internal fun sendAccessTokenAsEvent(context: Context, accessToken: String?) {
            val event = "ACCESS_TOKEN_RECEIVED: ${accessToken}"
//            Log.i(TAG, ">>>> Sending event to Lambda: ${event}")
//            sendEvenToLambda(event)
//
//            Log.i(TAG, ">>>> Sending event to Alexa...")
//            val instance = AlexaManager.getInstance(context, Constants.PRODUCT_ID)
//            instance.sendEvent(event, object : ImplAsyncCallback<AvsResponse, Exception?>() {
//                override fun success(result: AvsResponse) {
//                    Log.i(TAG, result.toString())
//                }
//                override fun failure(error: Exception?) {
//                    Log.e(TAG, error?.message, error)
//                }
//            })
        }

        internal fun sendEvenToLambda(payload: String?) {
            val region = "us-east-1"
            val functionName = "handlePrototypingRequest"
            try {
                val client = AWSLambdaAsyncClientBuilder.standard()
                        .withRegion(Regions.fromName(region))
                        .build()

                val request = InvokeRequest()
                        .withFunctionName(functionName)
                        .withPayload(payload)

                val result = client.invoke(request)
                Log.i(TAG, ">>>> Lambda result: $functionName: $result")

            } catch (e: Throwable) {
                Log.e(TAG, e.message, e)
            }
        }
    }
}
