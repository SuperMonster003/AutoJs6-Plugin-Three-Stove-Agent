package io.github.supermonster003.autojs6.plugin.ai.agent.ui

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import java.text.DateFormat
import java.util.Date

/** Locale-aware dates and readable JSON for task history, memory and diagnostics. */
internal object Formats {
    fun date(context: Context, time: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
        context.resources.configuration.locales[0]).format(Date(time))
    fun day(context: Context, time: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM, context.resources.configuration.locales[0]).format(Date(time))
    fun pretty(value: JsonElement?): String = value?.let { GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(it) }.orEmpty()
}
