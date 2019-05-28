package ee.ioc.phon.android.speechutils.editor

import java.util.Arrays
import java.util.HashSet

object Constants {

    val CHARACTERS_WS: Set<Character> = HashSet(Arrays.asList(arrayOf<Character>(' ', '\n', '\t')))

    // Symbols that should not be preceded by space in a written text.
    val CHARACTERS_PUNCT: Set<Character> = HashSet(Arrays.asList(arrayOf<Character>(',', ':', ';', '.', '!', '?')))

    // Symbols after which the next word should be capitalized.
    // We include ) because ;-) often finishes a sentence.
    val CHARACTERS_EOS: Set<Character> = HashSet(Arrays.asList(arrayOf<Character>('.', '!', '?', ')')))
}