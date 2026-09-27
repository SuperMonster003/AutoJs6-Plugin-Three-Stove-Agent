package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.ui.kit.*

/** Dialog raised by the failure alert setting: the stopped task's goal and cause, with View and Close. */
class FailureAlertActivity : HostAppearanceActivity() {
    override val dialogTheme = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        title = getString(R.string.app_name)
        val runId = intent.getStringExtra(EXTRA_RUN_ID)?.takeIf { it.length in 1..128 }
        val heading = intent.getStringExtra(EXTRA_TITLE).orEmpty().take(200)
        val summary = intent.getStringExtra(EXTRA_SUMMARY).orEmpty().take(1000)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; layoutDirection = resources.configuration.layoutDirection; tag = "failure-alert"
            setPaddingRelative(kit.dp(Ui.SPACE_XXL), kit.dp(Ui.SPACE_XL), kit.dp(Ui.SPACE_XXL), kit.dp(Ui.SPACE_MD))
        }
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply {
                setImageDrawable(kit.tintedDrawable(R.drawable.ic_warning, palette.danger)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(kit.dp(Ui.ICON_SIZE), kit.dp(Ui.ICON_SIZE)).apply { marginEnd = kit.dp(Ui.SPACE_MD) })
            addView(kit.text(heading, Ui.TEXT_TITLE, medium = true).apply {
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
            }, LinearLayout.LayoutParams(0, -2, 1f))
        })
        content.addView(kit.text(summary, Ui.TEXT_BODY).apply { setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START; tag = "failure-summary" },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_LG) })
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END or Gravity.CENTER_VERTICAL
            addView(kit.textButton(getString(R.string.alert_close), "alert-close") { finish() },
                LinearLayout.LayoutParams(-2, -2).apply { marginEnd = kit.dp(Ui.SPACE_SM) })
            addView(kit.filledButton(getString(R.string.task_view), "alert-view") {
                runId?.let { startActivity(Intent(this@FailureAlertActivity, RunDetailActivity::class.java).putExtra("runId", it)) }
                finish()
            })
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_LG) })
        setContentView(androidx.core.widget.NestedScrollView(this).apply { addView(content) })
    }

    companion object {
        const val EXTRA_RUN_ID = "runId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SUMMARY = "summary"
        fun intent(context: Context, runId: String, title: String, summary: String): Intent =
            Intent(context, FailureAlertActivity::class.java).putExtra(EXTRA_RUN_ID, runId).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_SUMMARY, summary)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
