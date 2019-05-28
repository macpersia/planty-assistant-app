package ee.ioc.phon.android.speechutils.editor

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

import java.util.regex.Matcher
import java.util.regex.Pattern

import ee.ioc.phon.android.speechutils.Log

// TODO: return correct boolean
class InputConnectionCommandEditor : CommandEditor {

    private var mPrevText = ""

    private var mGlueCount = 0

    var inputConnection: InputConnection? = null

    /**
     * Writes the text into the text field and forgets the previous entry.
     */
    fun commitFinalResult(text: String): Boolean {
        commitText(text)
        mPrevText = ""
        mGlueCount = 0
        return true
    }

    /**
     * Writes the text into the text field and stores it for future reference.
     */
    fun commitPartialResult(text: String): Boolean {
        commitText(text)
        mPrevText = text
        return true
    }

    @Override
    fun goToPreviousField(): Boolean {
        return inputConnection!!.performEditorAction(EditorInfo.IME_ACTION_PREVIOUS)
    }

    @Override
    fun goToNextField(): Boolean {
        return inputConnection!!.performEditorAction(EditorInfo.IME_ACTION_NEXT)
    }

    @Override
    fun goToCharacterPosition(pos: Int): Boolean {
        return inputConnection!!.setSelection(pos, pos)
    }

    @Override
    fun selectAll(): Boolean {
        return inputConnection!!.performContextMenuAction(android.R.id.selectAll)
    }

    @Override
    fun cut(): Boolean {
        return inputConnection!!.performContextMenuAction(android.R.id.cut)
    }

    @Override
    fun copy(): Boolean {
        return inputConnection!!.performContextMenuAction(android.R.id.copy)
    }

    @Override
    fun paste(): Boolean {
        return inputConnection!!.performContextMenuAction(android.R.id.paste)
    }

    @Override
    fun capitalize(str: String): Boolean {
        return false
    }

    @Override
    fun addSpace(): Boolean {
        inputConnection!!.commitText(" ", 1)
        return true
    }

    @Override
    fun addNewline(): Boolean {
        inputConnection!!.commitText("\n", 1)
        return true
    }

    @Override
    fun reset(): Boolean {
        var success = false
        inputConnection!!.beginBatchEdit()
        val cs = inputConnection!!.getSelectedText(0)
        if (cs != null) {
            val len = cs!!.length()
            inputConnection!!.setSelection(len, len)
            success = true
        }
        inputConnection!!.endBatchEdit()
        return success
    }

    /**
     * Deletes all characters up to the leftmost whitespace from the cursor (including the whitespace).
     * If something is selected then delete the selection.
     * TODO: maybe expensive?
     */
    @Override
    fun deleteLeftWord(): Boolean {
        inputConnection!!.beginBatchEdit()
        // If something is selected then delete the selection and return
        if (inputConnection!!.getSelectedText(0) != null) {
            inputConnection!!.commitText("", 0)
        } else {
            val beforeCursor = inputConnection!!.getTextBeforeCursor(MAX_DELETABLE_CONTEXT, 0)
            if (beforeCursor != null) {
                val beforeCursorLength = beforeCursor!!.length()
                val m = WHITESPACE_AND_TOKEN.matcher(beforeCursor)
                var lastIndex = 0
                while (m.find()) {
                    // If the cursor is immediately left from WHITESPACE_AND_TOKEN, then
                    // delete the WHITESPACE_AND_TOKEN, otherwise delete whatever is in between.
                    lastIndex = if (beforeCursorLength == m.end()) m.start() else m.end()
                }
                if (lastIndex > 0) {
                    inputConnection!!.deleteSurroundingText(beforeCursorLength - lastIndex, 0)
                } else if (beforeCursorLength < MAX_DELETABLE_CONTEXT) {
                    inputConnection!!.deleteSurroundingText(beforeCursorLength, 0)
                }
            }
        }
        inputConnection!!.endBatchEdit()
        return true
    }

    @Override
    fun select(str: String): Boolean {
        var success = false
        inputConnection!!.beginBatchEdit()
        val extractedText = inputConnection!!.getExtractedText(ExtractedTextRequest(), 0)
        val beforeCursor = extractedText.text
        val index = beforeCursor.toString().lastIndexOf(str)
        if (index > 0) {
            inputConnection!!.setSelection(index, index + str.length())
            success = true
        }
        inputConnection!!.endBatchEdit()
        return success
    }

    @Override
    fun delete(str: String): Boolean {
        return replace(str, "")
    }

    @Override
    fun replace(str1: String, str2: String): Boolean {
        var success = false
        inputConnection!!.beginBatchEdit()
        val extractedText = inputConnection!!.getExtractedText(ExtractedTextRequest(), 0)
        if (extractedText != null) {
            val beforeCursor = extractedText!!.text
            //CharSequence beforeCursor = mInputConnection.getTextBeforeCursor(MAX_SELECTABLE_CONTEXT, 0);
            Log.i("replace: " + beforeCursor)
            val index = beforeCursor.toString().lastIndexOf(str1)
            Log.i("replace: " + index)
            if (index > 0) {
                inputConnection!!.setSelection(index, index)
                inputConnection!!.deleteSurroundingText(0, str1.length())
                if (!str2.isEmpty()) {
                    inputConnection!!.commitText(str2, 0)
                }
                success = true
            }
            inputConnection!!.endBatchEdit()
        }
        return success
    }

    @Override
    fun go(): Boolean {
        // Does not work on Google Searchbar
        // mInputConnection.performEditorAction(EditorInfo.IME_ACTION_DONE);

        // Works in Google Searchbar, GF Translator, but NOT in the Firefox search widget
        //mInputConnection.performEditorAction(EditorInfo.IME_ACTION_GO);

        inputConnection!!.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
        return true
    }

    /**
     * Updates the text field, modifying only the parts that have changed.
     */
    private fun commitText(text: String) {
        var text = text
        inputConnection!!.beginBatchEdit()
        // Calculate the length of the text that has changed
        val commonPrefix = greatestCommonPrefix(mPrevText, text)
        val commonPrefixLength = commonPrefix.length()
        val prevLength = mPrevText.length()
        var deletableLength = prevLength - commonPrefixLength

        // Delete the glue symbol if present
        if (text.isEmpty()) {
            deletableLength += mGlueCount
        }

        if (deletableLength > 0) {
            inputConnection!!.deleteSurroundingText(deletableLength, 0)
        }

        if (text.isEmpty() || commonPrefixLength == text.length()) {
            return
        }

        // We look at the left context of the cursor
        // to decide which glue symbol to use and whether to capitalize the text.
        var leftContext = inputConnection!!.getTextBeforeCursor(MAX_DELETABLE_CONTEXT, 0)
        // In some error situation, null is returned
        if (leftContext == null) {
            leftContext = ""
        }
        var glue = ""
        if (commonPrefixLength == 0) {
            glue = getGlue(text, leftContext)
        } else {
            text = text.substring(commonPrefixLength)
        }

        if (" ".equals(glue)) {
            mGlueCount = 1
        }

        text = capitalizeIfNeeded(text, leftContext)
        inputConnection!!.commitText(glue + text, 1)
        inputConnection!!.endBatchEdit()
    }

    companion object {

        // Maximum number of characters that left-swipe is willing to delete
        private val MAX_DELETABLE_CONTEXT = 100
        // Token optionally preceded by whitespace
        private val WHITESPACE_AND_TOKEN = Pattern.compile("\\s*\\w+")

        /**
         * Capitalize if required by left context
         */
        private fun capitalizeIfNeeded(text: String, leftContext: CharSequence): String {
            // Capitalize if required by left context
            val leftContextTrimmed = leftContext.toString().trim()
            if (leftContextTrimmed.length() === 0 || Constants.CHARACTERS_EOS.contains(leftContextTrimmed.charAt(leftContextTrimmed.length() - 1))) {
                // Since the text can start with whitespace (newline),
                // we capitalize the first non-whitespace character.
                var firstNonWhitespaceIndex = -1
                for (i in 0 until text.length()) {
                    if (!Constants.CHARACTERS_WS.contains(text.charAt(i))) {
                        firstNonWhitespaceIndex = i
                        break
                    }
                }
                if (firstNonWhitespaceIndex > -1) {
                    var newText = text.substring(0, firstNonWhitespaceIndex) + Character.toUpperCase(text.charAt(firstNonWhitespaceIndex))
                    if (firstNonWhitespaceIndex < text.length() - 1) {
                        newText += text.substring(firstNonWhitespaceIndex + 1)
                    }
                    return newText
                }
            }
            return text
        }

        private fun getGlue(text: String, leftContext: CharSequence): String {
            val firstChar = text.charAt(0)

            return if (leftContext.length() === 0
                    || Constants.CHARACTERS_WS.contains(firstChar)
                    || Constants.CHARACTERS_PUNCT.contains(firstChar)
                    || Constants.CHARACTERS_WS.contains(leftContext.charAt(leftContext.length() - 1))) {
                ""
            } else " "
        }

        private fun greatestCommonPrefix(a: String, b: String): String {
            val minLength = Math.min(a.length(), b.length())
            for (i in 0 until minLength) {
                if (a.charAt(i) !== b.charAt(i)) {
                    return a.substring(0, i)
                }
            }
            return a.substring(0, minLength)
        }
    }
}
