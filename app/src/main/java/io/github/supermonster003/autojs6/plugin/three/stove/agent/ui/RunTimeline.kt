package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/**
 * Task steps as a keyed timeline: each step index owns one row, updated only when that step
 * changes, so frequent polling never rebuilds the list or moves the reader's scroll position.
 * Labels come from the bundled catalog and fixed strings; model text is shown as plain text.
 */
internal class RunTimeline(
    private val kit: Kit,
    /** False where the result summary is already shown next to the timeline (the home feed). */
    private val showDoneSummary: Boolean = true,
    /** Optional per-step details (task details screen), rebuilt only when that step changes. */
    private val details: ((JsonObject, LinearLayout) -> Unit)? = null,
    val view: LinearLayout = LinearLayout(kit.context).apply { orientation = LinearLayout.VERTICAL },
) {
    private class Row(val view: LinearLayout, val icon: ImageView, val disc: FrameLayout, val title: TextView, val detail: TextView,
                      val badge: TextView, val extra: LinearLayout, var signature: String = "")
    private val rows = linkedMapOf<Long, Row>()

    fun reset() { rows.clear(); view.removeAllViews() }

    fun render(steps: JsonArray?) {
        val items = steps?.mapNotNull { it.takeIf { element -> element.isJsonObject }?.asJsonObject }.orEmpty()
        val indexes = items.mapNotNull { it.number("index") }.toSet()
        // A bounded window drops the oldest steps; remove their rows instead of rebuilding.
        rows.keys.filter { it !in indexes }.forEach { index -> rows.remove(index)?.let { view.removeView(it.view) } }
        items.forEach { step ->
            val index = step.number("index") ?: return@forEach
            val row = rows.getOrPut(index) { create(index).also { view.addView(it.view, LinearLayout.LayoutParams(-1, -2)) } }
            val signature = step.toString()
            if (row.signature != signature) { row.signature = signature; bind(row, step) }
        }
        view.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun create(index: Long): Row {
        val icon = ImageView(kit.context).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        val disc = FrameLayout(kit.context).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(icon, FrameLayout.LayoutParams(kit.dp(16), kit.dp(16), Gravity.CENTER))
        }
        val title = kit.text("", Ui.TEXT_BODY, medium = true).apply { textAlignment = View.TEXT_ALIGNMENT_VIEW_START }
        val detail = kit.text("", Ui.TEXT_SECONDARY, kit.palette.muted).apply {
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setTextIsSelectable(true); setPaddingRelative(0, kit.dp(2), 0, 0)
        }
        val badge = kit.badge("")
        val extra = LinearLayout(kit.context).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val column = LinearLayout(kit.context).apply {
            orientation = LinearLayout.VERTICAL
            addView(title); addView(detail)
            addView(badge, LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
            addView(extra, LinearLayout.LayoutParams(-1, -2))
        }
        val row = LinearLayout(kit.context).apply {
            orientation = LinearLayout.HORIZONTAL
            tag = "step-$index"
            setPaddingRelative(0, kit.dp(Ui.SPACE_SM), 0, kit.dp(Ui.SPACE_SM))
            addView(disc, LinearLayout.LayoutParams(kit.dp(28), kit.dp(28)).apply { marginEnd = kit.dp(Ui.SPACE_MD) })
            addView(column, LinearLayout.LayoutParams(0, -2, 1f))
        }
        return Row(row, icon, disc, title, detail, badge, extra)
    }

    private fun bind(row: Row, step: JsonObject) {
        val context = kit.context
        val decision = step.getAsJsonObject("decision")
        val error = step.string("error")
        val confirmation = step.string("confirmation")
        val (title, icon, tone) = when (step.string("kind")) {
            "ask" -> Triple(context.getString(R.string.step_ask), R.drawable.ic_help, Tone.ACCENT)
            "done" -> Triple(context.getString(R.string.step_done), R.drawable.ic_check, Tone.SUCCESS)
            "repair" -> Triple(context.getString(R.string.step_repair), R.drawable.ic_restart, Tone.WARNING)
            "error" -> Triple(context.getString(R.string.step_error), R.drawable.ic_block, Tone.DANGER)
            else -> Triple(step.string("tool")?.let { ToolPresentation.label(context, it) } ?: context.getString(R.string.step_error),
                if (error != null) R.drawable.ic_warning else if (confirmation == "denied") R.drawable.ic_block else R.drawable.ic_tune,
                if (error != null) Tone.DANGER else if (confirmation == "denied") Tone.WARNING else Tone.NEUTRAL)
        }
        val (fill, foreground) = kit.toneColors(tone)
        row.disc.background = kit.roundedFill(fill, Ui.RADIUS_PILL)
        row.icon.setImageDrawable(kit.tintedDrawable(icon, foreground))
        row.title.text = title
        val detail = decision?.getAsJsonObject("ask")?.string("question")
            ?: decision?.getAsJsonObject("done")?.string("summary")?.takeIf { showDoneSummary }
            ?: decision?.string("reasoning")
        row.detail.text = detail.orEmpty()
        row.detail.visibility = if (detail.isNullOrBlank()) View.GONE else View.VISIBLE
        val badge = when {
            error != null -> (decision?.string("failure")?.let { "$error ($it)" } ?: error) to Tone.DANGER
            confirmation == "allowed" -> context.getString(R.string.history_allowed) to Tone.SUCCESS
            confirmation == "denied" -> context.getString(R.string.history_denied) to Tone.WARNING
            else -> null
        }
        row.badge.visibility = if (badge == null) View.GONE else View.VISIBLE
        badge?.let { (text, badgeTone) ->
            val (badgeFill, badgeText) = kit.toneColors(badgeTone)
            row.badge.text = text; row.badge.setTextColor(badgeText); row.badge.background = kit.roundedFill(badgeFill, Ui.RADIUS_PILL)
        }
        details?.let { build ->
            row.extra.removeAllViews(); build(step, row.extra)
            row.extra.visibility = if (row.extra.childCount == 0) View.GONE else View.VISIBLE
        }
    }
}
