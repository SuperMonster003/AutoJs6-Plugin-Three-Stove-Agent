package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.scripts.ScriptRoots

/** P3.1's small settings entry. The full settings/workbench remains in P6. */
class ScriptRootsActivity : HostAppearanceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.script_roots_title)
        val settings = ScriptRootSettings(this)
        val page = AgentUi.column(this)
        val layout = AgentUi.card(page)
        layout.addView(TextView(this).apply { setText(R.string.script_roots_instruction) })
        val field = EditText(this).apply {
            id = R.id.script_roots_paths
            contentDescription = getString(R.string.script_roots_title)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 4; maxLines = 12; filters = arrayOf(InputFilter.LengthFilter(8192))
            setText(savedInstanceState?.getString("paths") ?: settings.read().joinToString("\n"))
        }
        layout.addView(field)
        layout.addView(Button(this).apply {
            AgentUi.role(this, "primary")
            setText(R.string.script_roots_save)
            setOnClickListener {
                val roots = runCatching { ScriptRoots.parseLines(field.text.toString()) }.getOrNull()
                if (roots == null) field.error = getString(R.string.script_roots_invalid)
                else { settings.save(roots); finish() }
            }
        })
        layout.addView(Button(this).apply { setText(android.R.string.cancel); setOnClickListener { finish() } })
        setContentView(AgentUi.screen(this, getString(R.string.script_roots_title), page))
        tint(layout)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("paths", findViewById<EditText>(R.id.script_roots_paths).text.toString())
        super.onSaveInstanceState(outState)
    }
}
