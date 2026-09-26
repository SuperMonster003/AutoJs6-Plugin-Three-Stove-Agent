package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.view.Gravity
import android.view.View
import android.widget.*
import io.github.supermonster003.autojs6.plugin.ai.agent.R

internal object WorkbenchLayout {
    fun create(activity: LauncherActivity): LinearLayout {
        val body = AgentUi.column(activity)
        AgentUi.text(body, activity.getString(R.string.ui_home_title), 30, true)
        AgentUi.text(body, activity.getString(R.string.ui_home_subtitle), 15).setTextColor(AgentUi.palette(activity).muted)
        val connection = AgentUi.column(activity, 4).apply {
            setPaddingRelative(AgentUi.dp(activity, 4), 0, AgentUi.dp(activity, 4), AgentUi.dp(activity, 10))
            body.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        AgentUi.text(connection, "", 13).apply { id = R.id.launcher_host_status; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        AgentUi.action(connection, R.string.launcher_connect, "connect") {}.id = R.id.launcher_connect
        AgentUi.action(connection, R.string.launcher_open_host, "host") {}.id = R.id.launcher_open_host

        val composer = AgentUi.card(body)
        AgentUi.row(composer, activity.getString(R.string.ui_choose_model), activity.getString(R.string.ui_model_inherit), "model") {}.id = R.id.workbench_model
        AgentUi.text(composer, activity.getString(R.string.workbench_goal_label), 16, true).labelFor = R.id.workbench_goal
        composer.addView(EditText(activity).apply {
            id = R.id.workbench_goal; setHint(R.string.workbench_goal_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            gravity = Gravity.TOP or Gravity.START; minLines = 3; maxLines = 10
        }, LinearLayout.LayoutParams(-1, -2))
        val options = AgentUi.disclosure(composer, R.string.ui_task_options)
        AgentUi.text(options, activity.getString(R.string.workbench_preset), 14).labelFor = R.id.workbench_preset
        options.addView(Spinner(activity).apply { id = R.id.workbench_preset; contentDescription = activity.getString(R.string.workbench_preset) }, LinearLayout.LayoutParams(-1, -2))
        AgentUi.text(options, activity.getString(R.string.ui_preset_optional), 13).setTextColor(AgentUi.palette(activity).muted)
        val actions = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        composer.addView(actions, LinearLayout.LayoutParams(-1, -2))
        AgentUi.action(actions, R.string.workbench_voice, "voice") {}.apply {
            id = R.id.workbench_voice; visibility = View.GONE; text = ""; contentDescription = activity.getString(R.string.workbench_voice)
            val mic = activity.getDrawable(R.drawable.ic_mic)!!.mutate().apply {
                setTint(AgentUi.palette(activity).accent); setBounds(0, 0, AgentUi.dp(activity, 24), AgentUi.dp(activity, 24))
            }
            setCompoundDrawablesRelative(mic, null, null, null)
            layoutParams = LinearLayout.LayoutParams(AgentUi.dp(activity, 56), -2).apply { marginEnd = AgentUi.dp(activity, 10) }
        }
        AgentUi.action(actions, R.string.workbench_send, "send", true) {}.apply {
            id = R.id.workbench_send; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        AgentUi.text(composer, "", 14).apply {
            id = R.id.workbench_error; visibility = View.GONE; setTextColor(AgentUi.palette(activity).danger)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
        }
        val current = AgentUi.card(body).apply { id = R.id.workbench_current; visibility = View.GONE }
        AgentUi.section(current, R.string.workbench_current)
        AgentUi.text(current, "", 19, true).apply { id = R.id.workbench_current_goal; setTextIsSelectable(true) }
        AgentUi.text(current, "", 14, true).apply { id = R.id.workbench_state; setTextColor(AgentUi.palette(activity).accent); accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        AgentUi.text(current, "", 15).apply { id = R.id.workbench_step; setTextIsSelectable(true) }
        current.addView(ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply { id = R.id.workbench_progress }, LinearLayout.LayoutParams(-1, AgentUi.dp(activity, 8)))
        AgentUi.text(current, "", 12).apply { id = R.id.workbench_budget; setTextColor(AgentUi.palette(activity).muted) }
        current.addView(AgentUi.column(activity, 0).apply { id = R.id.workbench_pending })
        AgentUi.action(current, R.string.task_stop, "stop") {}.apply { id = R.id.workbench_stop; AgentUi.role(this, "danger") }
        AgentUi.action(current, R.string.workbench_details, "details") {}.id = R.id.workbench_details
        AgentUi.section(body, R.string.workbench_recent)
        AgentUi.card(body).id = R.id.workbench_recent
        AgentUi.action(body, R.string.history_title, "history") {}.id = R.id.workbench_history
        return AgentUi.screen(activity, activity.getString(R.string.app_name), body, onBack = null).apply {
            (getChildAt(0) as LinearLayout).addView(AgentUi.icon(activity, R.drawable.ic_history, R.string.history_title, "home-history") {
                activity.startActivity(android.content.Intent(activity, HistoryActivity::class.java))
            })
            (getChildAt(0) as LinearLayout).addView(AgentUi.icon(activity, R.drawable.ic_more, R.string.ui_more, "more") {}.apply { id = R.id.workbench_more })
        }
    }
}
