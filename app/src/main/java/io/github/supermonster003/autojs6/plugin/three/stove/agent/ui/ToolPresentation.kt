package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.content.Context
import io.github.supermonster003.autojs6.plugin.three.stove.agent.R
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolCatalog
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.ToolGroup

/**
 * Human labels for tools: the localized group from the bundled catalog plus the stable tool name.
 * Only the packaged catalog is read, never model or host text.
 */
internal object ToolPresentation {
    val groupLabels = mapOf(ToolGroup.OBSERVE to R.string.presets_group_observe, ToolGroup.ACT to R.string.presets_group_act,
        ToolGroup.GESTURE to R.string.presets_group_gesture, ToolGroup.OCR to R.string.presets_group_ocr, ToolGroup.SCRIPT to R.string.presets_group_script,
        ToolGroup.SCRIPT_DYNAMIC to R.string.presets_group_script_dynamic, ToolGroup.MCP to R.string.presets_group_mcp,
        ToolGroup.FILES to R.string.presets_group_files, ToolGroup.SHELL to R.string.presets_group_shell, ToolGroup.MEMORY to R.string.presets_group_memory,
        ToolGroup.USER to R.string.presets_group_user)

    @Volatile private var catalog: ToolCatalog? = null

    private fun catalog(context: Context): ToolCatalog? = catalog ?: runCatching {
        ToolCatalog.fromAssets { path -> context.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) } }
    }.getOrNull()?.also { catalog = it }

    fun group(context: Context, tool: String): ToolGroup? =
        catalog(context)?.get(tool)?.group ?: ToolGroup.MCP.takeIf { tool.startsWith("mcp_") }

    fun groupLabel(context: Context, group: ToolGroup): String = context.getString(groupLabels.getValue(group))

    /** "Group · tool_name", or the bare name for a tool the bundled catalog does not know. */
    fun label(context: Context, tool: String): String = group(context, tool)?.let { "${groupLabel(context, it)} · $tool" } ?: tool
}
