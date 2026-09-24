package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.os.Bundle
import android.widget.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.service.ControlRequests

/** Read-only recent-task destination. P6.2 adds full retention, history controls and export. */
class RunDetailActivity : HostAppearanceActivity() {
    private lateinit var agent: AgentConnection
    private lateinit var body: LinearLayout
    private var previous = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (20 * resources.displayMetrics.density).toInt()
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(padding, padding, padding, padding) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; fitsSystemWindows = true; layoutDirection = resources.configuration.layoutDirection }
        root.addView(Button(this).apply { setText(R.string.workbench_back); setOnClickListener { finish() } })
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        agent = AgentConnection(this) { render(it.run) }.apply {
            preferRunning = false
            selectedId = runCatching { ControlRequests.runId(jsonObject("runId" to requireNotNull(intent.getStringExtra("runId")).json())) }.getOrNull()
        }
        if (agent.selectedId == null) finish()
        tint(root)
    }
    override fun onStart() { super.onStart(); agent.start() }
    override fun onStop() { agent.stop(); super.onStop() }
    override fun onDestroy() { agent.close(); super.onDestroy() }
    private fun render(row: JsonObject?) {
        if (row.toString() == previous) return
        previous = row.toString(); body.removeAllViews()
        fun label(value: String) { body.addView(TextView(this).apply {
            text = value; setTextIsSelectable(true); setPadding(0, 8, 0, 8)
        }) }
        if (row == null) { label(getString(R.string.workbench_run_unavailable)); return }
        label(row.string("goal").orEmpty())
        label(WorkbenchText.state(this, row)); label(WorkbenchText.budget(this, row))
        label(WorkbenchText.summary(row))
        row.getAsJsonObject("result")?.let { result ->
            for (key in listOf("evidence", "unfinished")) result[key]?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { label(it.asString) }
            result["error"]?.let { label(it.toString()) }
        }
        row.getAsJsonArray("steps")?.forEach { item ->
            val step = item.asJsonObject
            label(getString(R.string.task_running, step.number("index") ?: 0) + " - " + (step.string("tool") ?: step.string("kind").orEmpty()))
            label(step.getAsJsonObject("decision")?.string("reasoning") ?: step.string("observation").orEmpty())
        }
        if (row.flag("truncated") == true) label(getString(R.string.workbench_truncated))
    }
}
