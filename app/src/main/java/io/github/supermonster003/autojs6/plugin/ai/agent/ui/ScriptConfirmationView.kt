package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.ui.kit.*

/** A registered script's description and its complete parameters, sorted by name. */
internal object ScriptConfirmationView {
    fun create(context: Context, pending: JsonObject, kit: Kit = Kit.of(context)): View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        pending.string("description")?.takeIf { it.isNotBlank() }?.let { description ->
            addView(kit.text(description, Ui.TEXT_BODY).apply {
                setTextIsSelectable(true); textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPaddingRelative(0, kit.dp(Ui.SPACE_XS), 0, 0)
            })
        }
        addView(kit.parameterTable(ArgumentRows.rows(pending.getAsJsonObject("arguments")?.get("parameters"), sorted = true)),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(Ui.SPACE_SM) })
    }
}
