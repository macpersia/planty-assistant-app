package com.github.macpersia.planty_alexa_android.actions

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.*
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.github.macpersia.planty_alexa_android.R
import com.github.macpersia.planty_alexa_android.actions.adapter.ActionFragmentAdapter
import java.util.*


/**
 * @author will on 5/30/2016.
 */

class ActionsFragment : BaseListenerFragment() {

    val TAG = ActionsFragment::class.java.simpleName

    override val title: String
        get() = getString(R.string.app_name)

    override val rawCode: Int
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

////            <ImageButton
////            android:id="@+id/login_with_amazon"
////            android:layout_width="wrap_content"
////            android:layout_height="wrap_content"
////            android:background="@color/colorPrimaryDark"
////            android:onClick="authorize"
////            android:src="@drawable/btnlwa_gold_loginwithamazon" />
//            items.add(ActionFragmentAdapter.ActionFragmentItem("Login with Amazon",
//                    android.R.drawable.ic_secure,
//                    View.OnClickListener { it -> authorizeApp()}))

            return items
        }

    private fun authorizeApp() {
        val authReq = AuthorizeRequest.Builder(requestContext)
                .addScopes(ProfileScope.profile()/*, ProfileScope.postalCode()*/)
                .build()
        AuthorizationManager.authorize(authReq)
    }


    override fun startListening() {

    }

    private lateinit var requestContext: RequestContext

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_actions, container, false)
        requestContext = RequestContext.create(this)
        requestContext.registerListener(object : AuthorizeListener() {

            /* Authorization was completed successfully. */
            override fun onSuccess(result: AuthorizeResult) {
                /* Your app is now authorized for the requested scopes */
                fetchUserProfile(requestContext.context)
            }

            /* There was an error during the attempt to authorize the application. */
            override fun onError(ae: AuthError) {
                /* Inform the user of the error */
            }

            /* Authorization was cancelled before it could be completed. */
            override fun onCancel(cancellation: AuthCancellation) {
                /* Reset the UI to a ready-to-login state */
            }
        })
        return view
    }

    override fun onResume() {
        super.onResume()
        requestContext.onResume()
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

    private fun fetchUserProfile(context: Context) {
        User.fetch(context, object : Listener<User, AuthError> {
            /* fetch completed successfully. */
            override fun onSuccess(user: User?) {
                val name = user?.userName
                val email = user?.userEmail
                val account = user?.userId
                //val zipcode = user.userPostalCode

                //runOnUiThread(Runnable { updateProfileData(name, email, account, zipcode) })
                Log.i(TAG, ">>>> User.name: ${user?.userName}")
                Log.i(TAG, ">>>> User.email: ${user?.userEmail}")
                Log.i(TAG, ">>>> User.account: ${user?.userId}")
}
            /* There was an error during the attempt to get the profile. */
            override fun onError(ae: AuthError?) {
                /* Retry or inform the user of the error */
                Log.e(TAG, ae.toString(), ae)
            }
        })
    }}
