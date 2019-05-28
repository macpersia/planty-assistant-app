package ee.ioc.phon.android.speechutils.editor

/**
 * TODO: work in progress
 */
interface CommandEditor {

    fun commitFinalResult(str: String): Boolean

    fun commitPartialResult(str: String): Boolean

    // Moving between fields

    // Go to the previous field
    fun goToPreviousField(): Boolean

    // Go to the next field
    fun goToNextField(): Boolean

    // Moving around in the string

    // Go to the character at the given position
    fun goToCharacterPosition(pos: Int): Boolean

    fun select(str: String): Boolean

    // Reset selection
    fun reset(): Boolean

    // Context menu actions
    fun selectAll(): Boolean

    fun cut(): Boolean

    fun copy(): Boolean

    fun paste(): Boolean

    // Editing

    fun capitalize(str: String): Boolean

    fun addSpace(): Boolean

    fun addNewline(): Boolean

    fun deleteLeftWord(): Boolean

    fun delete(str: String): Boolean

    fun replace(str1: String, str2: String): Boolean

    /**
     * Performs the Search-action, e.g. to launch search on a searchbar.
     */
    fun go(): Boolean
}