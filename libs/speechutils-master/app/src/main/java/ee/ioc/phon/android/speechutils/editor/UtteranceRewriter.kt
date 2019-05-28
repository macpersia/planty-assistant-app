package ee.ioc.phon.android.speechutils.editor

import android.content.ContentResolver
import android.net.Uri
import android.text.TextUtils
import android.util.Pair

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import ee.ioc.phon.android.speechutils.Log

class UtteranceRewriter {

    private var mCommands: List<Command>? = null

    internal class Triple(val mId: String, val mStr: String, val mArgs: Array<String>)

    constructor() {
        mCommands = ArrayList()
    }

    constructor(commands: List<Command>?) {
        assert(commands != null)
        mCommands = commands
    }

    constructor(str: String) : this(loadRewrites(str)) {}

    @Throws(IOException::class)
    constructor(contentResolver: ContentResolver, uri: Uri) : this(loadRewrites(contentResolver, uri)) {
    }

    fun size(): Int {
        return mCommands!!.size()
    }

    /**
     * Rewrites and returns the given string,
     * and the first matching command.
     */
    fun rewrite(str: String): Triple {
        var str = str
        for (command in mCommands!!) {
            Log.i("editor: rewrite with command: $str: $command")
            val pair = command.match(str)
            if (pair != null) {
                str = pair!!.first
                val commandId = command.getId()
                if (commandId != null) {
                    val args = pair!!.second
                    Log.i("editor: rewrite: success: " + str + ": " + commandId + "(" + TextUtils.join(",", args) + ")")
                    return Triple(commandId, str, args)
                }
            }
        }
        return Triple(null, str, null)
    }

    /**
     * Rewrites and returns the given results.
     * TODO: improve this
     */
    fun rewrite(results: List<String>): Pair<???, List<String>> {
        var commandId: String? = null
        var args: Array<String>? = null
        val rewrittenResults = ArrayList()
        for (result in results) {
            val triple = rewrite(result)
            rewrittenResults.add(triple.mStr)
            commandId = triple.mId
            args = triple.mArgs
        }
        return Pair(Pair(commandId, args), rewrittenResults)
    }

    /**
     * Serializes the rewrites as tab-separated-values.
     */
    fun toTsv(): String {
        val stringBuilder = StringBuilder()
        for (command in mCommands!!) {
            stringBuilder.append(escape(command.getPattern().toString()))
            stringBuilder.append('\t')
            stringBuilder.append(escape(command.getReplacement()))
            if (command.getId() != null) {
                stringBuilder.append('\t')
                stringBuilder.append(escape(command.getId()))
            }
            for (arg in command.getArgs()) {
                stringBuilder.append('\t')
                stringBuilder.append(escape(arg))
            }
            stringBuilder.append('\n')
        }
        return stringBuilder.toString()
    }

    fun toStringArray(): Array<String> {
        val array = arrayOfNulls<String>(mCommands!!.size())
        var i = 0
        for (command in mCommands!!) {
            array[i] = (pp(command.getPattern().toString()).toInt()
                    + '\n'
                    + pp(command.getReplacement()).toInt())
            if (command.getId() != null) {
                array[i] += '\n' + command.getId()
            }
            for (arg in command.getArgs()) {
                array[i] += '\n' + arg
            }
            i++
        }
        return array
    }

    companion object {

        private val PATTERN_TRAILING_TABS = Pattern.compile("\t*$")


        /**
         * Loads the rewrites from tab-separated values.
         */
        private fun loadRewrites(str: String?): List<Command> {
            assert(str != null)
            val commands = ArrayList()
            for (line in str!!.split("\n")) {
                addLine(commands, line)
            }
            return commands
        }


        /**
         * Loads the rewrites from an URI using a ContentResolver.
         */
        @Throws(IOException::class)
        private fun loadRewrites(contentResolver: ContentResolver, uri: Uri): List<Command> {
            val inputStream = contentResolver.openInputStream(uri)
            val commands = ArrayList()
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String
                while ((line = reader.readLine()) != null) {
                    addLine(commands, line)
                }
                inputStream!!.close()
            }
            return commands
        }

        private fun addLine(commands: List<Command>, line: String): Boolean {
            // TODO: removing trailing tabs means that rewrite cannot delete a string
            val splits = PATTERN_TRAILING_TABS.matcher(line).replaceAll("").split("\t")
            if (splits.size > 1) {
                try {
                    commands.add(getCommand(splits))
                    return true
                } catch (e: PatternSyntaxException) {
                    // TODO: collect and expose buggy entries
                }

            }
            return false
        }

        private fun getCommand(splits: Array<String>): Command {
            var commandId: String? = null
            var args: Array<String>? = null
            val numOfArgs = splits.size - 3

            if (numOfArgs >= 0) {
                commandId = unescape(splits[2])
            }

            if (numOfArgs > 0) {
                args = arrayOfNulls(numOfArgs)
                for (i in 0 until numOfArgs) {
                    args[i] = unescape(splits[i + 3])
                }
            }

            return Command(unescape(splits[0]), unescape(splits[1]), commandId, args)
        }

        /**
         * Maps newlines and tabs to literals of the form "\n" and "\t".
         */
        private fun escape(str: String): String {
            return str.replace("\n", "\\n").replace("\t", "\\t")
        }

        /**
         * Maps literals of the form "\n" and "\t" to newlines and tabs.
         */
        private fun unescape(str: String): String {
            return str.replace("\\n", "\n").replace("\\t", "\t")
        }

        private fun pp(str: String): String {
            return escape(str).replace(" ", "·")
        }
    }
}