package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.*
import android.view.View
import android.widget.*
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import java.util.Calendar

class HistoryActivity : HostAppearanceActivity() {
    private lateinit var history: HistoryConnection
    private lateinit var list: LinearLayout
    private lateinit var error: TextView
    private lateinit var presets: Spinner
    private lateinit var fromButton: Button
    private lateinit var untilButton: Button
    private lateinit var clear: Button
    private var rows = emptyList<JsonObject>()
    private var presetIds = listOf<String?>(null)
    private var filter = RunHistoryFilter()
    private var visible = false
    private val main = Handler(Looper.getMainLooper())
    private val poll = Runnable { refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filter = RunHistoryFilter(savedInstanceState?.getString("state"), savedInstanceState?.getString("preset"),
            savedInstanceState?.getLong("from", -1)?.takeIf { it >= 0 }, savedInstanceState?.getLong("until", -1)?.takeIf { it >= 0 })
        val body = HistoryViews.column(this).apply { layoutDirection = resources.configuration.layoutDirection }
        setContentView(ScrollView(this).apply { fitsSystemWindows = true; addView(body) })
        HistoryViews.label(body, getString(R.string.history_title), true)
        HistoryViews.button(body, R.string.workbench_back, "back") { finish() }
        HistoryViews.label(body, getString(R.string.history_retention))
        val states = listOf<String?>(null) + RunHistoryCodec.states
        val stateSpinner = Spinner(this).apply { contentDescription = getString(R.string.history_state_filter) }
        HistoryViews.label(body, getString(R.string.history_state_filter)); body.addView(stateSpinner)
        stateSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, states.map {
            if (it == null) getString(R.string.history_all) else WorkbenchText.state(this, jsonObject("state" to it.json()))
        })
        stateSpinner.setSelection(states.indexOf(filter.state).coerceAtLeast(0))
        stateSpinner.onItemSelectedListener = selection { filter = filter.copy(state = states[it]); render() }
        HistoryViews.label(body, getString(R.string.workbench_preset))
        presets = Spinner(this).apply { contentDescription = getString(R.string.workbench_preset) }; body.addView(presets)
        presets.onItemSelectedListener = selection { filter = filter.copy(preset = presetIds.getOrNull(it)); render() }
        fromButton = HistoryViews.button(body, R.string.history_from, "from") { pickDate(true) }
        untilButton = HistoryViews.button(body, R.string.history_until, "until") { pickDate(false) }
        HistoryViews.button(body, R.string.history_reset_dates, "reset-dates") { filter = filter.copy(from = null, until = null); render() }
        clear = HistoryViews.button(body, R.string.history_clear, "clear") {
            AlertDialog.Builder(this).setMessage(R.string.history_clear_confirm).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.history_clear) { _, _ -> history.query("clear") { it.onSuccess { refresh() }.onFailure { showError() } } }.show()
        }
        error = HistoryViews.label(body, "")
        list = HistoryViews.column(this); body.addView(list)
        history = HistoryConnection(this) { refresh() }
        tint(body)
    }
    override fun onStart() { super.onStart(); visible = true; history.start() }
    override fun onStop() { visible = false; main.removeCallbacks(poll); history.stop(); super.onStop() }
    override fun onDestroy() { history.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("state", filter.state); outState.putString("preset", filter.preset)
        filter.from?.let { outState.putLong("from", it) }; filter.until?.let { outState.putLong("until", it) }
        super.onSaveInstanceState(outState)
    }
    private fun refresh() {
        main.removeCallbacks(poll)
        history.query("list") { result ->
            result.onSuccess { data ->
                val next = data.getAsJsonArray("runs").map { it.asJsonObject }
                if (next != rows || list.childCount == 0) {
                    rows = next
                    val ids = listOf(null) + (rows.mapNotNull { it.string("preset") } + listOfNotNull(filter.preset)).distinct().sorted()
                    if (ids != presetIds || presets.adapter == null) {
                        presetIds = ids
                        presets.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ids.map { it ?: getString(R.string.history_all) })
                        presets.setSelection(ids.indexOf(filter.preset).coerceAtLeast(0))
                    }
                    render()
                }
            }.onFailure { showError() }
            if (visible) main.postDelayed(poll, 2000)
        }
    }
    private fun render() {
        if (!::list.isInitialized) return
        fromButton.text = filter.from?.let { getString(R.string.history_label_value, getString(R.string.history_from), HistoryViews.date(this, it)) } ?: getString(R.string.history_from)
        untilButton.text = filter.until?.let { getString(R.string.history_label_value, getString(R.string.history_until), HistoryViews.date(this, it - 1)) } ?: getString(R.string.history_until)
        clear.isEnabled = rows.any { !WorkbenchText.active(it) }
        list.removeAllViews()
        val matching = rows.filter(filter::matches)
        if (matching.isEmpty()) HistoryViews.label(list, getString(R.string.history_empty))
        matching.forEach { row -> list.addView(Button(this).apply {
            text = getString(R.string.history_item, getString(R.string.workbench_recent_item, WorkbenchText.state(this@HistoryActivity, row), row.string("goal").orEmpty()),
                HistoryViews.date(this@HistoryActivity, row.number("startedAt") ?: 0), row.string("preset"))
            setOnClickListener { startActivity(Intent(this@HistoryActivity, RunDetailActivity::class.java).putExtra("runId", row.string("runId"))) }
        }, LinearLayout.LayoutParams(-1, -2)) }
        tint(list)
    }
    private fun pickDate(start: Boolean) {
        val calendar = Calendar.getInstance().apply { (if (start) filter.from else filter.until?.minus(1))?.let { timeInMillis = it } }
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day, 0, 0, 0); calendar.set(Calendar.MILLISECOND, 0)
            if (start) filter = filter.copy(from = calendar.timeInMillis)
            else { calendar.add(Calendar.DAY_OF_MONTH, 1); filter = filter.copy(until = calendar.timeInMillis) }
            render()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun showError() { error.setText(R.string.history_unavailable) }
    private fun selection(action: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action(position)
    }
}
