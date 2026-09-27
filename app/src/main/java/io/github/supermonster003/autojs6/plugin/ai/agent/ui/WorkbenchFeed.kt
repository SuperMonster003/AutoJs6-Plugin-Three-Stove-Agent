package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.R
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*

/** What the feed asks its screen to do; the feed itself never starts a task. */
internal interface FeedActions {
    fun openDetail(runId: String)
    fun stop(runId: String)
    /** Fill the composer with [goal] and [preset]; with [chooseModel] also open the model switcher. Never starts. */
    fun prefill(goal: String, preset: String?, chooseModel: Boolean = false)
}

/**
 * The home feed: a welcome state with examples, recent tasks, and the current task card at the
 * bottom next to the composer. Rendering is incremental: text is replaced only when it changes,
 * steps are keyed by index and the recent list is rebuilt only when its content changes.
 */
internal class WorkbenchFeed(private val kit: Kit, private val actions: FeedActions) {
    private val context = kit.context
    val view: LinearLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val welcome = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; tag = "welcome"
        setPaddingRelative(0, kit.dp(Ui.SPACE_XXL), 0, kit.dp(Ui.SPACE_LG))
    }
    private val recentHeader = kit.sectionHeader(kit.string(R.string.workbench_recent)).apply { setPaddingRelative(kit.dp(Ui.SPACE_XS), kit.dp(Ui.SPACE_LG), 0, kit.dp(Ui.SPACE_SM)) }
    val recent: LinearLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; id = R.id.workbench_recent }

    val current: LinearLayout = kit.card().apply { id = R.id.workbench_current; visibility = View.GONE; tag = "current" }
    val goal: TextView = kit.text("", Ui.TEXT_ITEM, kit.palette.text).apply {
        id = R.id.workbench_current_goal; setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        background = kit.roundedFill(kit.palette.accentTone, Ui.RADIUS_BUBBLE)
        setPaddingRelative(kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_MD))
    }
    val state: TextView = kit.text("", Ui.TEXT_BODY, kit.palette.accent, medium = true).apply {
        id = R.id.workbench_state; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private val meta = kit.text("", Ui.TEXT_SECONDARY, kit.palette.muted).apply { tag = "current-meta"; textAlignment = View.TEXT_ALIGNMENT_VIEW_START }
    val access: TextView = kit.badge(kit.string(R.string.settings_full_access), Tone.DANGER).apply { id = R.id.workbench_current_access; visibility = View.GONE }
    val progress = LinearProgressIndicator(context).apply {
        id = R.id.workbench_progress; trackCornerRadius = kit.dp(4); trackThickness = kit.dp(6)
        setIndicatorColor(kit.palette.accent); trackColor = kit.palette.surfaceVariant
    }
    val budget: TextView = kit.text("", Ui.TEXT_CAPTION, kit.palette.muted).apply { id = R.id.workbench_budget; textAlignment = View.TEXT_ALIGNMENT_VIEW_START }
    private val resultTitle = kit.text(kit.string(R.string.history_result), Ui.TEXT_SECTION, kit.palette.accent, medium = true).apply {
        visibility = View.GONE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }
    /** Latest progress while running; the result summary once finished. */
    val step: TextView = kit.text("", Ui.TEXT_BODY).apply { id = R.id.workbench_step; setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START }
    val timeline = RunTimeline(kit, showDoneSummary = false)
    val pending: LinearLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; id = R.id.workbench_pending }
    private val accessibilityBanner = Banner(kit).apply {
        view.id = R.id.workbench_accessibility
        show(kit.string(R.string.accessibility_start_failed), Tone.WARNING, R.drawable.ic_warning)
        actions.addView(kit.textButton(kit.string(R.string.accessibility_open_settings), "accessibility-settings") {
            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        hide()
    }
    val stopButton: MaterialButton = kit.outlinedButton(kit.string(R.string.task_stop), "stop", danger = true) {
        runId?.let(actions::stop)
    }.apply { id = R.id.workbench_stop }
    val details: MaterialButton = kit.textButton(kit.string(R.string.workbench_details), "details") { runId?.let(actions::openDetail) }
        .apply { id = R.id.workbench_details }
    private val runAgain = kit.tonalButton(kit.string(R.string.workbench_run_again), "rerun") {
        row?.let { actions.prefill(it.string("goal").orEmpty(), it.string("preset")) }
    }
    private val retryModel = kit.textButton(kit.string(R.string.workbench_retry_model), "retry-model") {
        row?.let { actions.prefill(it.string("goal").orEmpty(), it.string("preset"), chooseModel = true) }
    }
    private var row: JsonObject? = null
    private var runId: String? = null
    private var recentKey = ""
    private var welcomeShown: Boolean? = null

    init {
        buildWelcome()
        view.addView(welcome, LinearLayout.LayoutParams(-1, -2))
        view.addView(recentHeader)
        view.addView(recent, LinearLayout.LayoutParams(-1, -2))
        with(current) {
            addView(goal, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END; marginStart = kit.dp(Ui.SPACE_XXXL) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(state)
                addView(access, LinearLayout.LayoutParams(-2, -2).apply { marginStart = kit.dp(Ui.SPACE_SM) })
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_LG) })
            addView(meta, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(2) })
            addView(progress, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
            addView(budget, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            addView(timeline.view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
            addView(resultTitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
            addView(step, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            addView(pending, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
            addView(accessibilityBanner.view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_MD) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                addView(stopButton, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
                addView(runAgain, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
                addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_LG) })
            addView(retryModel, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END })
        }
        view.addView(current, kit.cardParams(topDp = Ui.SPACE_LG, bottomDp = Ui.SPACE_LG))
    }

    private fun buildWelcome() {
        welcome.addView(ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(kit.dp(64), kit.dp(64)))
        welcome.addView(kit.text(kit.string(R.string.ui_home_title), Ui.TEXT_DISPLAY, medium = true).apply {
            gravity = Gravity.CENTER; setPaddingRelative(0, kit.dp(Ui.SPACE_LG), 0, kit.dp(Ui.SPACE_XS))
            if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
        }, LinearLayout.LayoutParams(-1, -2))
        welcome.addView(kit.text(kit.string(R.string.ui_home_subtitle), Ui.TEXT_BODY, kit.palette.muted).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, -2))
        val examples = ChipGroup(context).apply { chipSpacingHorizontal = kit.dp(Ui.SPACE_SM); tag = "examples" }
        listOf(R.string.workbench_example_version, R.string.workbench_example_wifi, R.string.workbench_example_notifications).forEachIndexed { index, text ->
            examples.addView(kit.chip(kit.string(text), "example-$index") { actions.prefill(kit.string(text), null) })
        }
        welcome.addView(examples, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = kit.dp(Ui.SPACE_XL) })
    }

    /** Renders one poll; returns true when the current task or its request changed and should be revealed. */
    fun render(snapshot: WorkbenchSnapshot) {
        val value = snapshot.run
        row = value
        runId = value?.string("runId")
        val showWelcome = value == null
        if (welcomeShown != showWelcome) { welcomeShown = showWelcome; welcome.visibility = if (showWelcome) View.VISIBLE else View.GONE }
        renderRecent(snapshot.runs.filter { it.string("runId") != runId }.take(5))
        current.visibility = if (value == null) View.GONE else View.VISIBLE
        if (value == null) { timeline.reset(); return }
        val active = WorkbenchText.active(value)
        set(goal, value.string("goal").orEmpty())
        set(state, WorkbenchText.state(context, value))
        set(meta, listOfNotNull(value.getAsJsonObject("model")?.string("name"),
            value.string("preset")?.let { if (it == "default") kit.string(R.string.workbench_default_preset) else it }).joinToString(" · "))
        meta.visibility = if (meta.text.isEmpty()) View.GONE else View.VISIBLE
        access.visibility = if (value.flag("fullAccess") == true) View.VISIBLE else View.GONE
        val maximum = value.getAsJsonObject("budget")?.number("maxSteps")?.toInt() ?: 40
        if (progress.max != maximum) progress.max = maximum
        val stepCount = (value.number("step") ?: 0).toInt()
        if (progress.progress != stepCount) progress.setProgressCompat(stepCount, true)
        progress.contentDescription = WorkbenchText.budget(context, value)
        progress.visibility = if (active) View.VISIBLE else View.GONE
        set(budget, WorkbenchText.budget(context, value))
        timeline.render(value.getAsJsonArray("steps"))
        val summary = WorkbenchText.summary(value)
        set(step, if (!active) summary else value.string("progress") ?: value.getAsJsonArray("steps")?.lastOrNull()?.asJsonObject?.let {
            it.getAsJsonObject("decision")?.string("reasoning") ?: it.string("tool")
        }.orEmpty())
        step.visibility = if (step.text.isEmpty()) View.GONE else View.VISIBLE
        resultTitle.visibility = if (!active && summary.isNotEmpty()) View.VISIBLE else View.GONE
        if (WorkbenchText.accessibilityBlocked(value)) accessibilityBanner.view.visibility = View.VISIBLE else accessibilityBanner.hide()
        stopButton.visibility = if (active) View.VISIBLE else View.GONE
        runAgain.visibility = if (active) View.GONE else View.VISIBLE
        retryModel.visibility = if (active) View.GONE else View.VISIBLE
    }

    private fun renderRecent(runs: List<JsonObject>) {
        val key = runs.joinToString("|") { listOf(it.string("runId"), it.string("state"), it.string("goal"), it.getAsJsonObject("model")?.string("name")).toString() }
        if (key == recentKey) return
        recentKey = key
        recent.removeAllViews()
        recentHeader.visibility = if (runs.isEmpty()) View.GONE else View.VISIBLE
        runs.forEach { item ->
            val id = item.string("runId") ?: return@forEach
            val (tone, icon) = when (item.string("state")) {
                "completed" -> Tone.SUCCESS to R.drawable.ic_check
                "failed", "blocked" -> Tone.DANGER to R.drawable.ic_warning
                "partial", "cancelled" -> Tone.WARNING to R.drawable.ic_block
                else -> Tone.ACCENT to R.drawable.ic_timer
            }
            val summary = listOfNotNull(WorkbenchText.state(context, item), HistoryViews.date(context, item.number("startedAt") ?: 0),
                item.getAsJsonObject("model")?.string("name")).joinToString(" · ")
            val entry = kit.settingRow(item.string("goal").orEmpty(), summary, icon, "recent-$id") { actions.openDetail(id) }
            (entry.view.getChildAt(0) as? ImageView)?.setImageDrawable(kit.tintedDrawable(icon, kit.toneColors(tone).second))
            entry.view.setPaddingRelative(kit.dp(Ui.SPACE_XS), entry.view.paddingTop, kit.dp(Ui.SPACE_XS), entry.view.paddingBottom)
            recent.addView(entry.view, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun set(view: TextView, text: CharSequence) { if (view.text.toString() != text.toString()) view.text = text }
}
