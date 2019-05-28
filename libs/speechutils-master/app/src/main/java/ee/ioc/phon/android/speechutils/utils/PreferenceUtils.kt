package ee.ioc.phon.android.speechutils.utils

import android.content.SharedPreferences
import android.content.res.Resources

import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.UUID

object PreferenceUtils {

    fun getPrefString(prefs: SharedPreferences, res: Resources, key: Int, defaultValue: Int): String {
        return prefs.getString(res.getString(key), res.getString(defaultValue))
    }

    fun getPrefString(prefs: SharedPreferences, res: Resources, key: Int): String {
        return prefs.getString(res.getString(key), null)
    }

    fun getPrefStringSet(prefs: SharedPreferences, res: Resources, key: Int): Set<String> {
        return prefs.getStringSet(res.getString(key), Collections.emptySet())
    }

    fun getPrefStringSet(prefs: SharedPreferences, res: Resources, key: Int, defaultValue: Int): Set<String> {
        return prefs.getStringSet(res.getString(key), getStringSetFromStringArray(res, defaultValue))
    }

    fun getPrefBoolean(prefs: SharedPreferences, res: Resources, key: Int, defaultValue: Int): Boolean {
        return prefs.getBoolean(res.getString(key), res.getBoolean(defaultValue))
    }

    fun getPrefInt(prefs: SharedPreferences, res: Resources, key: Int, defaultValue: Int): Int {
        return Integer.parseInt(getPrefString(prefs, res, key, defaultValue))
    }

    fun getUniqueId(settings: SharedPreferences): String {
        var id = settings.getString("id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            val editor = settings.edit()
            editor.putString("id", id)
            editor.apply()
        }
        return id
    }

    fun getStringSetFromStringArray(res: Resources, key: Int): Set<String> {
        return HashSet(Arrays.asList(res.getStringArray(key)))
    }

    fun getStringListFromStringArray(res: Resources, key: Int): List<String> {
        return Arrays.asList(res.getStringArray(key))
    }

    fun putPrefString(prefs: SharedPreferences, res: Resources, key: Int, value: String) {
        val editor = prefs.edit()
        editor.putString(res.getString(key), value)
        editor.apply()
    }

    fun putPrefStringSet(prefs: SharedPreferences, res: Resources, key: Int, value: Set<String>) {
        val editor = prefs.edit()
        editor.putStringSet(res.getString(key), value)
        editor.apply()
    }
}