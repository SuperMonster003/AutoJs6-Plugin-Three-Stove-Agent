package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*

/** Extra script directories, one absolute path per line. The host validates and applies them on the next connection. */
class ScriptRootsActivity : HostAppearanceActivity() {
    private lateinit var settings: ScriptRootSettings
    private lateinit var saved: String
    /** The last opened dialog, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null; private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = ScriptRootSettings(this)
        saved = settings.read().joinToString("\n")
        val scaffold = buildScaffold(getString(R.string.script_roots_title), contentPadding = ContentPadding.SCREEN)
        kit.caption(scaffold.content, getString(R.string.script_roots_instruction))
        val field = kit.formField(scaffold.content, getString(R.string.script_roots_title), savedInstanceState?.getString("paths") ?: saved, "script-roots",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, 8192, multiline = true).apply {
            id = R.id.script_roots_paths; textDirection = View.TEXT_DIRECTION_LTR; minLines = 4
        }
        val bar = kit.actionBar()
        bar.addView(kit.textButton(getString(android.R.string.cancel)) { navigateBack() })
        bar.addView(kit.filledButton(getString(R.string.script_roots_save)) {
            val roots = runCatching { ScriptRoots.parseLines(field.text.toString()) }.getOrNull()
            val layout = generateSequence(field.parent) { it.parent }.filterIsInstance<TextInputLayout>().firstOrNull()
            if (roots == null) layout?.error = getString(R.string.script_roots_invalid)
            else { settings.save(roots); finish() }
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = kit.dp(Ui.SPACE_SM) })
        scaffold.root.addView(bar, LinearLayout.LayoutParams(-1, -2))
        setContentView(scaffold.root)
    }
    override fun onStop() { prompt?.dismiss(); prompt = null; super.onStop() }
    override fun navigateBack() {
        val current = findViewById<android.widget.EditText>(R.id.script_roots_paths).text.toString()
        if (current == saved) finish() else prompt = kit.unsavedChanges { finish() }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("paths", findViewById<android.widget.EditText>(R.id.script_roots_paths).text.toString())
        super.onSaveInstanceState(outState)
    }
}
