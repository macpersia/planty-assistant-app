package ee.ioc.phon.android.speechutils.editor

import java.util.HashMap

import ee.ioc.phon.android.speechutils.Log

/**
 * utterance = "go to position 1"
 * pattern = ("goTo", "go to position (\d+)")
 * command = goToCharacterPosition(group(1).toInt())
 *
 *
 * TODO: add a way to specify the order and content of the arguments
 */
class CommandEditorManager(private val mCommandEditor: CommandEditor) {
    private val mEditorCommands = HashMap()

    interface EditorCommand {
        fun execute(args: Array<String>): Boolean
    }

    init {
        init()
    }


    operator fun get(id: String): EditorCommand? {
        return mEditorCommands.get(id)
    }

    fun execute(commandId: String, args: Array<String>): Boolean {
        val editorCommand = get(commandId) ?: return false
        return editorCommand.execute(args)
    }

    private fun init() {

        mEditorCommands.put("goToPreviousField", object : EditorCommand {

            @Override
            override fun execute(vararg args: String): Boolean {
                return mCommandEditor.goToPreviousField()
            }
        })

        mEditorCommands.put("goToNextField", object : EditorCommand {

            @Override
            override fun execute(vararg args: String): Boolean {
                return mCommandEditor.goToNextField()
            }
        })

        mEditorCommands.put("goToCharacterPosition", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                var pos = 0
                if (args.size > 0) {
                    try {
                        pos = Integer.parseInt(args[0])
                    } catch (e: NumberFormatException) {
                    }

                }
                return mCommandEditor.goToCharacterPosition(pos)
            }
        })

        mEditorCommands.put("select", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return if (args.size != 1) {
                    false
                } else mCommandEditor.select(args[0])
            }
        })

        mEditorCommands.put("delete", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return if (args.size != 1) {
                    false
                } else mCommandEditor.delete(args[0])
            }
        })

        mEditorCommands.put("replace", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return if (args.size != 2) {
                    false
                } else mCommandEditor.replace(args[0], args[1])
            }
        })

        mEditorCommands.put("addSpace", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return mCommandEditor.addSpace()
            }
        })

        mEditorCommands.put("addNewline", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return mCommandEditor.addNewline()
            }
        })

        mEditorCommands.put("selectAll", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return mCommandEditor.selectAll()
            }
        })

        mEditorCommands.put("cut", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return mCommandEditor.cut()
            }
        })

        mEditorCommands.put("copy", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return mCommandEditor.copy()
            }
        })

        mEditorCommands.put("paste", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return mCommandEditor.paste()
            }
        })

        mEditorCommands.put("go", object : EditorCommand {

            @Override
            override fun execute(args: Array<String>): Boolean {
                return mCommandEditor.go()
            }
        })
    }

}