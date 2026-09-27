package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Intent
import android.os.*
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair as AndroidPair
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.store.*
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*
import java.util.Calendar
import java.util.TimeZone

/** Searchable task history: text search, status chips, preset and date range filters, and run cards. */
class HistoryActivity : HostAppearanceActivity() {
    private lateinit var history: HistoryConnection
    private lateinit var scaffold: Scaffold
    private lateinit var list: LinearLayout
    private lateinit var message: TextView
    private lateinit var presetChip: Chip
    private lateinit var dateChip: Chip
    private val stateChips = linkedMapOf<String?, Chip>()
    /** The last opened dialog, exposed for instrumentation. */
    internal var prompt: AlertDialog? = null; private set
    private var rows = emptyList<JsonObject>()
    private var filter = RunHistoryFilter()
    private var visible = false
    private var loaded = false
    private val main = Handler(Looper.getMainLooper())
    private val poll = Runnable { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filter = RunHistoryFilter(savedInstanceState?.getString("state"), savedInstanceState?.getString("preset"),
            savedInstanceState?.getLong("from", -1)?.takeIf { it >= 0 }, savedInstanceState?.getLong("until", -1)?.takeIf { it >= 0 },
            savedInstanceState?.getString("query"))
        scaffold = buildScaffold(getString(R.string.history_title), contentPadding = ContentPadding(Ui.SPACE_LG, Ui.SPACE_SM, Ui.SECTION_GAP))
        val page = scaffold.content
        val (searchLayout, search) = kit.textField(filter.query, getString(R.string.history_search), InputType.TYPE_CLASS_TEXT, 256, tag = "history-search")
        searchLayout.startIconDrawable = kit.tintedDrawable(R.drawable.ic_search, palette.muted)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) { filter = filter.copy(query = s?.toString()); render() }
        })
        page.addView(searchLayout, LinearLayout.LayoutParams(-1, -2))
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        page.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; addView(chips)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
        val states = listOf<Pair<String?, String>>(null to getString(R.string.history_all), RunHistoryFilter.ACTIVE to getString(R.string.run_running)) +
            listOf("completed", "partial", "failed", "cancelled", "blocked").map { it to WorkbenchText.state(this, jsonObject("state" to it.json())) }
        states.forEach { (state, label) ->
            stateChips[state] = kit.chip(label, "state-${state ?: "all"}", checkable = true, checked = filter.state == state) {
                filter = filter.copy(state = state); render()
            }.also { chips.addView(it, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_SM) }) }
        }
        presetChip = kit.chip("", "history-preset", icon = R.drawable.ic_layers) { choosePreset() }
            .also { chips.addView(it, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_SM) }) }
        dateChip = kit.chip("", "history-dates", icon = R.drawable.ic_timer) { chooseDates() }.apply {
            setOnCloseIconClickListener { filter = filter.copy(from = null, until = null); render() }
            closeIconContentDescription = getString(R.string.history_reset_dates)
        }.also { chips.addView(it) }
        page.addView(kit.text(getString(R.string.history_retention), Ui.TEXT_CAPTION, palette.muted).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
        message = kit.text("", Ui.TEXT_BODY, palette.danger).apply { visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        page.addView(message, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "history-list" }
        page.addView(list, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
        setContentView(scaffold.root)
        history = HistoryConnection(this) { refresh() }
        render()
    }
    override fun onStart() { super.onStart(); visible = true; history.start() }
    override fun onStop() { visible = false; main.removeCallbacks(poll); history.stop(); prompt?.dismiss(); prompt = null; super.onStop() }
    override fun onDestroy() { history.close(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("state", filter.state); outState.putString("preset", filter.preset); outState.putString("query", filter.query)
        filter.from?.let { outState.putLong("from", it) }; filter.until?.let { outState.putLong("until", it) }
        super.onSaveInstanceState(outState)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, R.id.history_clear, 0, R.string.history_clear).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.history_clear)?.isEnabled = rows.any { !WorkbenchText.active(it) }
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.history_clear) return super.onOptionsItemSelected(item)
        clearFinished(); return true
    }
    internal fun clearFinished() {
        prompt = kit.confirmDialog(getString(R.string.history_clear), getString(R.string.history_clear_confirm), getString(R.string.history_clear), destructive = true) {
            history.query("clear") { it.onSuccess { refresh() }.onFailure { showError() } }
        }
    }

    private fun refresh() {
        main.removeCallbacks(poll)
        history.query("list") { result ->
            result.onSuccess { data ->
                val next = data.getAsJsonArray("runs").map { it.asJsonObject }
                if (next != rows || !loaded) { rows = next; loaded = true; message.visibility = View.GONE; render(); invalidateOptionsMenu() }
            }.onFailure { showError() }
            if (visible) main.postDelayed(poll, 2000)
        }
    }
    private fun presetLabel(name: String) = if (name == "default") getString(R.string.workbench_default_preset) else name
    private fun choosePreset() {
        val names = listOf<String?>(null) + (rows.mapNotNull { it.string("preset") } + listOfNotNull(filter.preset)).distinct().sorted()
        prompt = kit.singleChoiceDialog(getString(R.string.workbench_preset), names.map { it?.let(::presetLabel) ?: getString(R.string.history_all_presets) },
            names.indexOf(filter.preset)) { index -> filter = filter.copy(preset = names[index]); render() }
    }
    private fun chooseDates() {
        // MaterialDatePicker works in UTC days; convert them to local midnights for the filter.
        fun toUtc(local: Long) = Calendar.getInstance().apply { timeInMillis = local }.let { day ->
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH)) }.timeInMillis
        }
        fun toLocal(utc: Long, nextDay: Boolean) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }.let { day ->
            Calendar.getInstance().apply {
                clear(); set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH)); if (nextDay) add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
        }
        val builder = MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.history_dates)
        val start = filter.from; val end = filter.until
        if (start != null && end != null) builder.setSelection(AndroidPair(toUtc(start), toUtc(end - 1)))
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { range ->
            val from = range.first ?: return@addOnPositiveButtonClickListener
            val until = range.second ?: from
            filter = filter.copy(from = toLocal(from, false), until = toLocal(until, true)); render()
        }
        picker.show(supportFragmentManager, "history-dates")
    }

    private fun render() {
        if (!::list.isInitialized) return
        stateChips.forEach { (state, chip) -> chip.isChecked = filter.state == state }
        presetChip.text = filter.preset?.let(::presetLabel) ?: getString(R.string.history_all_presets)
        presetChip.isChecked = filter.preset != null
        val from = filter.from; val until = filter.until
        dateChip.text = if (from != null && until != null) "${HistoryViews.day(this, from)} - ${HistoryViews.day(this, until - 1)}" else getString(R.string.history_dates)
        dateChip.isCloseIconVisible = from != null
        list.removeAllViews()
        val matching = rows.filter(filter::matches)
        if (loaded && matching.isEmpty()) list.addView(kit.emptyState(getString(R.string.history_empty), null, R.drawable.ic_history))
        matching.forEach { row -> list.addView(runCard(row), kit.cardParams(bottomDp = Ui.SPACE_SM)) }
    }
    private fun runCard(row: JsonObject): View {
        val id = row.string("runId").orEmpty()
        val (tone, _) = WorkbenchText.tone(row)
        return kit.card(interactive = true).apply {
            tag = "history-$id"
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(kit.badge(WorkbenchText.state(context, row), tone))
                if (row.flag("fullAccess") == true) addView(kit.badge(getString(R.string.settings_full_access), Tone.DANGER),
                    LinearLayout.LayoutParams(-2, -2).apply { marginStart = kit.dp(Ui.SPACE_XS) })
                addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
                addView(kit.text(HistoryViews.date(context, row.number("startedAt") ?: 0), Ui.TEXT_CAPTION, palette.muted))
            })
            addView(kit.text(row.string("goal").orEmpty(), Ui.TEXT_ITEM, medium = true).apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_SM), 0, 0)
            })
            val meta = listOfNotNull(row.string("preset")?.let(::presetLabel), row.getAsJsonObject("model")?.string("name")).joinToString(" · ")
            if (meta.isNotEmpty()) addView(kit.text(meta, Ui.TEXT_SECONDARY, palette.muted).apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_XS), 0, 0)
            })
            contentDescription = listOf(row.string("goal").orEmpty(), WorkbenchText.state(context, row), meta).filter { it.isNotEmpty() }.joinToString(", ")
            setOnClickListener { startActivity(Intent(this@HistoryActivity, RunDetailActivity::class.java).putExtra("runId", id)) }
        }
    }
    private fun showError() { message.setText(R.string.history_unavailable); message.visibility = View.VISIBLE }
}
