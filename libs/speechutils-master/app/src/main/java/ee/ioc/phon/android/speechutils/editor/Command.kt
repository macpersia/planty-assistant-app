package ee.ioc.phon.android.speechutils.editor

import android.text.TextUtils
import android.util.Pair

import java.util.regex.Matcher
import java.util.regex.Pattern

class Command {
    val pattern: Pattern
    val replacement: String
    val id: String
    val args: Array<String>
    private val mArgsAsStr: String

    /**
     * @param pattern     regular expression with capturing groups
     * @param replacement replacement string for the matched substrings, typically empty in case of commands
     * @param id          name of the command to execute, null if missing
     * @param args        arguments of the command
     */
    constructor(pattern: Pattern, replacement: String, id: String, args: Array<String>?) {
        this.pattern = pattern
        this.replacement = replacement
        this.id = id
        if (args == null) {
            this.args = arrayOfNulls(0)
        } else {
            this.args = args
        }
        mArgsAsStr = TextUtils.join(SEPARATOR, this.args)
    }

    constructor(pattern: String, replacement: String, id: String, args: Array<String>) : this(Pattern.compile(pattern), replacement, id, args) {}

    private fun matcher(str: CharSequence): Matcher {
        return pattern.matcher(str)
    }

    fun match(str: CharSequence): Pair<String, Array<String>>? {
        val m = matcher(str)
        if (m.matches()) {
            val newStr = m.replaceAll(replacement)
            val argsEvaluated = TextUtils.split(m.replaceAll(mArgsAsStr), SEPARATOR)
            return Pair(newStr, argsEvaluated)
        }
        return null
    }

    fun toString(): String {
        return pattern + "/" + replacement + "/" + id + "(" + args + ")"
    }

    companion object {
        private val SEPARATOR = "___"
    }
}