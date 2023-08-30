package be.planty.android.speechutils

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.Locale

object TtsLocaleMapper {

    private val SIMILAR_LOCALES_ET: List<Locale>

    private val SIMILAR_LOCALES: Map<Locale, List<Locale>>

    init {
        val aListEt = ArrayList<Locale>()
        aListEt.add(Locale("fi-FI"))
        aListEt.add(Locale("es-ES"))
        SIMILAR_LOCALES_ET = Collections.unmodifiableList(aListEt)
    }

    init {
        val aMap = HashMap<Locale, List<Locale>>()
        aMap.put(Locale("et-EE"), SIMILAR_LOCALES_ET)
        SIMILAR_LOCALES = Collections.unmodifiableMap(aMap)
    }

    fun getSimilarLocales(locale: Locale): List<Locale> {
        return SIMILAR_LOCALES[locale] ?: Collections.emptyList()
    }
}