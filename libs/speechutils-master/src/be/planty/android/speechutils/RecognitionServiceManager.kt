package be.planty.android.speechutils

import android.annotation.TargetApi
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.util.Pair

import java.util.ArrayList
import java.util.HashSet
import java.util.Locale

class RecognitionServiceManager {
    private var mInitiallySelectedCombos: Set<String> = HashSet()
    private var mCombosExcluded: Set<String> = HashSet()

    interface Listener {
        fun onComplete(combos: List<String>, selectedCombos: Set<String>)
    }


    fun setCombosExcluded(set: Set<String>) {
        mCombosExcluded = set
    }

    fun setInitiallySelectedCombos(set: Set<String>) {
        mInitiallySelectedCombos = set
    }

    /**
     * @return list of currently installed RecognitionService component names flattened to short strings
     */
    fun getServices(pm: PackageManager): List<String> {
        val services = ArrayList<String>()
        val flags = 0
        //int flags = PackageManager.GET_META_DATA;
        val infos = pm.queryIntentServices(
                Intent(RecognitionService.SERVICE_INTERFACE), flags)

        for (ri in infos) {
            val si = ri.serviceInfo
            if (si == null) {
                Log.i("serviceInfo == null")
                continue
            }
            val pkg = si.packageName
            val cls = si.name
            // TODO: process si.metaData
            val component = ComponentName(pkg, cls).flattenToShortString()
            if (!mCombosExcluded.contains(component)) {
                services.add(component)
            }
        }
        return services
    }

    /**
     * Collect together the languages supported by the given services and call back once done.
     */
    fun populateCombos(activity: Activity, listener: Listener) {
        val services = getServices(activity.packageManager)
        populateCombos(activity, services, listener)
    }

    fun populateCombos(activity: Activity, services: List<String>, listener: Listener) {
        populateCombos(activity, services, 0, listener, ArrayList(), HashSet())
    }

    fun populateCombos(activity: Activity, service: String, listener: Listener) {
        val services = ArrayList<String>()
        services.add(service)
        populateCombos(activity, services, listener)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private fun populateCombos(activity: Activity, services: List<String>, counter: Int, listener: Listener,
                               combos: MutableList<String>, selectedCombos: MutableSet<String>) {

        if (services.size == counter) {
            listener.onComplete(combos, selectedCombos)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
        // TODO: this seems to be only for activities that implement ACTION_WEB_SEARCH
        //Intent intent = RecognizerIntent.getVoiceDetailsIntent(this);

        val service = services[counter]
        val serviceComponent = ComponentName.unflattenFromString(service)
        if (serviceComponent != null) {
            intent.`package` = serviceComponent.packageName
            // TODO: ideally we would like to query the component, because the package might
            // contain services (= components) with different capabilities.
            //intent.setComponent(serviceComponent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            // This is needed to include newly installed apps or stopped apps
            // as receivers of the broadcast.
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }

        activity.sendOrderedBroadcast(intent, null, object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {

                // Service that does not report which languages it supports
                if (resultCode != Activity.RESULT_OK) {
                    Log.i(combos.size.toString() + ") NO LANG: " + service)
                    combos.add(service)
                    populateCombos(activity, services, counter + 1, listener, combos, selectedCombos)
                    return
                }

                val results = getResultExtras(true)

                // Supported languages
                val prefLang = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)
                var allLangs = results.getCharSequenceArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)

                Log.i("Supported langs: $prefLang: $allLangs")
                if (allLangs == null) {
                    allLangs = ArrayList()
                }
                // We add the preferred language to the list of supported languages, if not already there.
                if (prefLang != null && !allLangs.contains(prefLang)) {
                    allLangs.add(prefLang)
                }

                for (lang in allLangs) {
                    val combo = service + SEPARATOR + lang
                    if (!mCombosExcluded.contains(combo)) {
                        Log.i(combos.size.toString() + ") " + combo)
                        combos.add(combo)
                        if (mInitiallySelectedCombos.contains(combo)) {
                            selectedCombos.add(combo)
                        }
                    }
                }

                populateCombos(activity, services, counter + 1, listener, combos, selectedCombos)
            }
        }, null, Activity.RESULT_OK, null, null)
    }

    companion object {
        private val SEPARATOR = ";"

        /**
         * @return true iff a RecognitionService with the given component name is installed
         */
        fun isRecognitionServiceInstalled(pm: PackageManager, componentName: ComponentName): Boolean {
            val services = pm.queryIntentServices(
                    Intent(RecognitionService.SERVICE_INTERFACE), 0)
            for (ri in services) {
                val si = ri.serviceInfo
                if (si == null) {
                    Log.i("serviceInfo == null")
                    continue
                }
                if (componentName == ComponentName(si.packageName, si.name)) {
                    return true
                }
            }
            return false
        }

        /**
         * On LOLLIPOP we use a builtin to parse the locale string, and return
         * the name of the locale in the language of the current locale. In pre-LOLLIPOP we just return
         * the formal name (e.g. "et-ee"), because the Locale-constructor is not able to parse it.
         *
         * @param localeAsStr Formal name of the locale, e.g. "et-ee"
         * @return The name of the locale in the language of the current locale
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun makeLangLabel(localeAsStr: String): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Locale.forLanguageTag(localeAsStr).displayName
            } else localeAsStr
        }

        fun getServiceAndLang(str: String): Array<String> {
            return TextUtils.split(str, SEPARATOR)
        }

        /**
         * @param str string like `ee.ioc.phon.android.speak/.HttpRecognitionService;et-ee`
         * @return ComponentName in the input string
         */
        fun getComponentName(str: String): ComponentName? {
            val splits = getServiceAndLang(str)
            return ComponentName.unflattenFromString(splits[0])
        }

        fun getServiceLabel(context: Context, service: String): String {
            val recognizerComponentName = ComponentName.unflattenFromString(service)
            return getServiceLabel(context, recognizerComponentName)
        }

        fun getServiceLabel(context: Context, recognizerComponentName: ComponentName?): String {
            var recognizer = "[?]"
            val pm = context.packageManager
            if (recognizerComponentName != null) {
                try {
                    val si = pm.getServiceInfo(recognizerComponentName, 0)
                    recognizer = si.loadLabel(pm).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    // ignored
                }

            }
            return recognizer
        }

        fun getLabel(context: Context, comboAsString: String): Pair<String, String> {
            var recognizer = "[?]"
            var language = "[?]"
            val splits = TextUtils.split(comboAsString, SEPARATOR)
            if (splits.size > 0) {
                val pm = context.packageManager
                val recognizerComponentName = ComponentName.unflattenFromString(splits[0])
                if (recognizerComponentName != null) {
                    try {
                        val si = pm.getServiceInfo(recognizerComponentName, 0)
                        recognizer = si.loadLabel(pm).toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        // ignored
                    }

                }
            }
            if (splits.size > 1) {
                language = makeLangLabel(splits[1])
            }
            return Pair(recognizer, language)
        }
    }
}