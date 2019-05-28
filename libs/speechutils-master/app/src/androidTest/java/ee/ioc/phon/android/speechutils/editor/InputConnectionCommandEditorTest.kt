package ee.ioc.phon.android.speechutils.editor

import android.content.Context
import android.support.test.runner.AndroidJUnit4
import android.test.suitebuilder.annotation.SmallTest
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.InstrumentationRegistry.getInstrumentation
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Assert.assertNotNull

@RunWith(AndroidJUnit4::class)
@SmallTest
class InputConnectionCommandEditorTest {

    private var mEditor: InputConnectionCommandEditor? = null

    @Before
    fun before() {
        val context = getInstrumentation().getContext()
        val view = EditText(context)
        //view.setText("elas metsas mutionu, keset kuuski noori-vanu");
        val editorInfo = EditorInfo()
        //editorInfo.initialSelStart = 12;
        //editorInfo.initialSelEnd = 19;
        val connection = view.onCreateInputConnection(editorInfo)
        //InputConnection connection = new BaseInputConnection(view, true);
        mEditor = InputConnectionCommandEditor()
        mEditor!!.setInputConnection(connection)
    }

    @Test
    fun test01() {
        assertNotNull(mEditor!!.getInputConnection())
    }

    @Test
    fun test02() {
        assertThat(mEditor!!.commitFinalResult("start12345 67890"), `is`(true))
        assertThat(getTextBeforeCursor(5), `is`("67890"))
        assertThat(mEditor!!.deleteLeftWord(), `is`(true))
        assertThat(getTextBeforeCursor(5), `is`("12345"))
        assertThat(mEditor!!.delete("12345"), `is`(true))
        assertThat(getTextBeforeCursor(5), `is`("Start"))
    }


    // TODO: @Test
    // Can't create handler inside thread that has not called Looper.prepare()
    fun test03() {
        assertThat(mEditor!!.copy(), `is`(true))
        assertThat(mEditor!!.paste(), `is`(true))
        assertThat(mEditor!!.paste(), `is`(true))
    }

    @Test
    fun test04() {
        assertThat(mEditor!!.commitPartialResult("...123"), `is`(true))
        assertThat(mEditor!!.commitPartialResult("...124"), `is`(true))
        assertThat(mEditor!!.commitFinalResult("...1245"), `is`(true))
        assertThat(mEditor!!.goToCharacterPosition(4), `is`(true))
        assertThat(getTextBeforeCursor(10), `is`("...1"))
    }

    @Test
    fun test05() {
        assertThat(mEditor!!.commitFinalResult("a12345 67890_12345"), `is`(true))
        assertThat(mEditor!!.select("12345"), `is`(true))
        assertThat(getTextBeforeCursor(2), `is`("0_"))
        assertThat(mEditor!!.deleteLeftWord(), `is`(true))
        assertThat(getTextBeforeCursor(2), `is`("0_"))
        assertThat(mEditor!!.deleteLeftWord(), `is`(true))
        assertThat(getTextBeforeCursor(2), `is`("45"))
    }

    @Test
    fun test06() {
        assertThat(mEditor!!.commitFinalResult("a12345 67890_12345"), `is`(true))
        assertThat(mEditor!!.replace("12345", "abcdef"), `is`(true))
        assertThat(mEditor!!.addSpace(), `is`(true))
        assertThat(mEditor!!.replace("12345", "ABC"), `is`(true))
        assertThat(getTextBeforeCursor(2), `is`("BC"))
        assertThat(mEditor!!.addNewline(), `is`(true))
        assertThat(mEditor!!.addSpace(), `is`(true))
        assertThat(mEditor!!.goToCharacterPosition(9), `is`(true))
        assertThat(getTextBeforeCursor(2), `is`("67"))
    }

    private fun getTextBeforeCursor(n: Int): String {
        return mEditor!!.getInputConnection().getTextBeforeCursor(n, 0).toString()
    }
}