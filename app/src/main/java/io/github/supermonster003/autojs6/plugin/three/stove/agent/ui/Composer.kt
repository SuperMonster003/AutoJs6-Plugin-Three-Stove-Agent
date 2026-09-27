package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/**
 * The docked composer: goal field (1 to 6 lines), preset chip, full access warning, voice and Start.
 * It sits above the keyboard; the feed scrolls behind it.
 */
internal class Composer(private val kit: Kit, onPreset: () -> Unit, onVoice: () -> Unit, onSend: () -> Unit) {
    private val fieldPair = kit.textField(null, kit.string(R.string.workbench_goal_hint),
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 4096, singleLine = false)
    val goal = fieldPair.second.apply {
        id = R.id.workbench_goal; minLines = 1; maxLines = 6; gravity = Gravity.TOP or Gravity.START
        filters = arrayOf(InputFilter.LengthFilter(4096))
    }
    /** Preset names are user text of any length; the chip may ellipsize and states the full name in its description. */
    val preset: Chip = kit.chip("", "preset-chip", icon = R.drawable.ic_layers) { onPreset() }.apply { id = R.id.workbench_preset; Ui.truncatable(this) }
    val fullAccess: TextView = kit.badge(kit.string(R.string.settings_full_access), Tone.DANGER).apply {
        id = R.id.workbench_full_access; visibility = View.GONE; tag = "full-access-chip"
        contentDescription = kit.string(R.string.settings_full_access_note)
    }
    val voice = kit.iconButton(R.drawable.ic_mic, kit.string(R.string.workbench_voice), "voice", kit.palette.accent) { onVoice() }
        .apply { id = R.id.workbench_voice; visibility = View.GONE }
    /** Round send button; its label is the accessible name "Start task". */
    val send = kit.filledButton("", "send") { onSend() }.apply {
        id = R.id.workbench_send; contentDescription = kit.string(R.string.workbench_send)
        setIconResource(R.drawable.ic_send); iconPadding = 0; iconSize = kit.dp(22)
        iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
        setPaddingRelative(0, 0, 0, 0); minWidth = kit.dp(Ui.TOUCH_TARGET); minimumWidth = kit.dp(Ui.TOUCH_TARGET)
    }
    val error: TextView = kit.text("", Ui.TEXT_SECONDARY, kit.palette.danger).apply {
        id = R.id.workbench_error; visibility = View.GONE; textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
    }
    val view: LinearLayout = LinearLayout(kit.context).apply {
        orientation = LinearLayout.VERTICAL
        tag = "composer"
        background = kit.roundedFill(kit.palette.surface, Ui.RADIUS_SHEET, kit.palette.outline)
        elevation = kit.dp(6).toFloat()
        setPaddingRelative(kit.dp(Ui.SPACE_LG), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_MD), kit.dp(Ui.SPACE_SM))
        // Options row: the preset chip may use the full width, so it never competes with the actions.
        addView(LinearLayout(kit.context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(android.widget.FrameLayout(kit.context).apply {
                addView(preset, android.widget.FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.CENTER_VERTICAL))
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
            addView(fullAccess, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_XS) })
        }, LinearLayout.LayoutParams(-1, -2))
        // Input row: the goal field grows; voice and send keep fixed 48dp targets beside it.
        addView(LinearLayout(kit.context).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
            addView(fieldPair.first, LinearLayout.LayoutParams(0, -2, 1f))
            addView(voice, LinearLayout.LayoutParams(kit.dp(Ui.TOUCH_TARGET), kit.dp(Ui.TOUCH_TARGET)).apply { marginStart = kit.dp(Ui.SPACE_XS); bottomMargin = kit.dp(4) })
            addView(send, LinearLayout.LayoutParams(kit.dp(52), kit.dp(52)).apply { marginStart = kit.dp(Ui.SPACE_XS); bottomMargin = kit.dp(2) })
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_XS) })
        addView(error, LinearLayout.LayoutParams(-1, -2))
    }

    fun showPreset(label: String, available: Boolean) {
        preset.text = label
        preset.contentDescription = kit.string(R.string.workbench_preset) + ", " + label
        preset.setTextColor(if (available) kit.palette.text else kit.palette.danger)
    }
}
