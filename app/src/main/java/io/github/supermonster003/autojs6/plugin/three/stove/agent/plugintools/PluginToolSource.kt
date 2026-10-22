package io.github.supermonster003.autojs6.plugin.three.stove.agent.plugintools

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.*
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import org.autojs.plugin.host.capability.api.HostCapabilityContract as H
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** The host capability broker as plugin tools use it: a failed call also hands over the host's error object (D88). */
internal fun interface PluginToolBridge {
    fun call(request: BridgeCall, callback: (PortResult<JsonElement>, JsonObject?) -> Unit): Cancellation
}

internal class PluginToolOutput(val name: String, val directory: Boolean, val mimeType: String)

/** What Stove keeps of one tool from the host's plugin_tools.list (D85 / D86). */
internal class PluginToolEntry(
    val name: String, val tool: String, val shortName: String, val provider: String, val timeoutMs: Long,
    val outputs: List<PluginToolOutput>, val imageParameters: Set<String>, val pathParameters: Set<String>, val takesOutputPaths: Boolean,
)

/**
 * Self-described AutoJs6 plugin tools for one task (roadmap P18, D83 - D90). The host lists the tools of enabled and
 * admitted plugins once while the task is prepared; a failed, refused or late listing only leaves the task without them.
 */
internal class PluginToolSource(private val bridge: PluginToolBridge, private val scheduler: RunScheduler,
                                private val clock: () -> Long = System::currentTimeMillis, private val zone: TimeZone = TimeZone.getDefault()) {
    fun prepare(base: ToolCatalog, policy: ToolPolicy, maximumTimeoutMs: Long, callback: (PluginToolSnapshot?) -> Unit): Cancellation {
        val done = AtomicBoolean()
        val guard = AtomicReference(Cancellation.NONE)
        val call = AtomicReference(Cancellation.NONE)
        fun finish(value: PluginToolSnapshot?) {
            if (!done.compareAndSet(false, true)) return
            guard.get().cancel(); call.get().cancel(); callback(value)
        }
        guard.set(scheduler.schedule(LIST_TIMEOUT_MS) { finish(null) })
        val request = BridgeCall(MODULE, "list", JsonArray(), listOf(TOKEN), minOf(LIST_TIMEOUT_MS - 500, maximumTimeoutMs))
        val handle = bridge.call(request) { value, _ ->
            finish((value as? PortResult.Success)?.let { runCatching { snapshot(it.value, base, policy, maximumTimeoutMs) }.getOrNull() })
        }
        call.set(handle)
        if (done.get()) handle.cancel()
        return Cancellation { if (done.compareAndSet(false, true)) { guard.get().cancel(); call.get().cancel() } }
    }

    /** Converts a listing; null when no tool is admitted. Pure apart from building the snapshot's adapter. */
    fun snapshot(listed: JsonElement, base: ToolCatalog, policy: ToolPolicy, maximumTimeoutMs: Long): PluginToolSnapshot? {
        val tools = listed.asJsonObject.getAsJsonArray("tools").mapNotNull { it.takeIf(JsonElement::isJsonObject)?.asJsonObject }
            .sortedBy { it.string("name").orEmpty() }
        val visible = policy.withExternalTools(base.tools.filter { it.external != null }).withOcrAvailability(true).withVisionAvailability(true)
        val room = minOf(MAX_TOOLS, 96 - base.tools.size, 64 - base.tools.count(visible::isEnabled))
        val specs = mutableListOf<ToolSpec>()
        val entries = linkedMapOf<String, PluginToolEntry>()
        var schemaBytes = 0
        for (tool in tools) {
            if (specs.size >= room) break
            val (entry, spec) = convert(tool) ?: continue
            if (base[spec.name] != null || spec.name in entries || !policy.withExternalTools(listOf(spec)).isEnabled(spec)) continue
            schemaBytes += spec.inputSchema.toString().utf8Size()
            if (schemaBytes > MAX_SCHEMA_BYTES) break
            specs += spec; entries[spec.name] = entry
        }
        if (specs.isEmpty()) return null
        return PluginToolSnapshot(base.withExternal(specs), entries, bridge, scheduler, clock, zone, maximumTimeoutMs)
    }

    companion object {
        const val MODULE = "plugin_tools"
        const val TOKEN = "plugin_tools"
        const val LIST_TIMEOUT_MS = 5_000L
        const val MAX_TOOLS = 32
        const val MAX_SCHEMA_BYTES = 64 * 1024
        const val MAX_TOOL_SCHEMA_BYTES = 16 * 1024
        const val MAX_DESCRIPTION_BYTES = 1024
        const val OUTPUT_PATHS = "outputPaths"
        const val OUTPUT_DIRECTORY = "plugin-outputs"
        private const val SPECIAL_TYPE = "x-autojs6-type"
        private const val DIRECTORY_FILES = "x-autojs6-files"
        private val NAME = Regex("[a-z][a-z0-9_]{1,63}")
        private val RISKS = setOf("readonly", "write", "destructive", "network")
        private const val IMAGE_NOTE = "Accepts \"screen\" (a screenshot taken now) or an image path relative to the workspace."
        private const val FILE_NOTE = "A file path relative to the workspace."
        private const val DIRECTORY_NOTE = "A directory path relative to the workspace; AutoJs6 hands its files to the plugin."

        /** The run asks for a plugin group and the link's grant covers listing and invoking. */
        fun admitted(groups: Collection<String>, methods: Set<String>, permissions: Set<String>): Boolean =
            (ToolGroup.PLUGIN.id in groups || ToolGroup.PLUGIN_WRITE.id in groups) &&
                "$MODULE.list" in methods && "$MODULE.invoke" in methods && TOKEN in permissions

        /** One listed tool as a catalog row, or null when Stove cannot offer it to a model (D86). */
        fun convert(tool: JsonObject): Pair<PluginToolEntry, ToolSpec>? = runCatching {
            val name = checkNotNull(tool.string("name")).also { require(NAME.matches(it)) }
            val shortName = checkNotNull(tool.string("shortName")).also { require(it.isNotBlank()) }
            val toolName = checkNotNull(tool.string("tool")).also { require(it.isNotBlank()) }
            val provider = checkNotNull(tool.string("provider")).also { require(RiskRules.isPackage(it)) }
            val risk = checkNotNull(tool.string("risk")).also { require(it in RISKS) }
            val timeoutMs = checkNotNull(tool.number("timeoutMs")).also { require(it >= 1) }
            val input = checkNotNull(tool["inputSchema"]?.takeIf { it.isJsonObject }?.asJsonObject)
            require(input.string("type") == "object" && input.toString().utf8Size() <= MAX_TOOL_SCHEMA_BYTES)
            fun names(key: String) = tool[key]?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString }.orEmpty().toSet()
            val outputs = tool["outputs"]?.takeIf { it.isJsonArray }?.asJsonArray.orEmpty().map { value ->
                val output = value.asJsonObject
                val kind = checkNotNull(output.string("kind")).also { require(it == "file" || it == "directory") }
                PluginToolOutput(checkNotNull(output.string("name")).also { require(it.isNotBlank()) }, kind == "directory", output.string("mimeType").orEmpty())
            }
            require(outputs.map { it.name }.distinct().size == outputs.size)
            val schema = input.deepCopy()
            val properties = schema["properties"]?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject().also { schema.add("properties", it) }
            val images = names("imageParameters").toMutableSet()
            val fileParameters = names("fileParameters")
            val directoryParameters = names("directoryParameters")
            val paths = (fileParameters + directoryParameters).toMutableSet()
            properties.entrySet().forEach { (key, value) ->
                if (!value.isJsonObject) return@forEach
                val property = value.asJsonObject
                val special = property.remove(SPECIAL_TYPE)?.asString
                val files = property.remove(DIRECTORY_FILES)?.asJsonArray?.map { it.asString }
                val kind = when (key) {
                    in images -> "image"
                    in fileParameters -> "file"
                    in directoryParameters -> "directory"
                    else -> special
                }
                val note = when (kind) {
                    "image" -> IMAGE_NOTE.also { images += key }
                    "file" -> FILE_NOTE.also { paths += key }
                    "directory" -> (DIRECTORY_NOTE + (files?.let { " The directory must contain: ${it.joinToString()}." } ?: "")).also { paths += key }
                    else -> null
                }
                if (note != null) property.addProperty("description", listOfNotNull(property.string("description"), note).joinToString(" "))
            }
            withoutExclusiveBounds(schema)
            val takesOutputPaths = outputs.isNotEmpty() && !properties.has(OUTPUT_PATHS)
            if (takesOutputPaths) properties.add(OUTPUT_PATHS, jsonObject("type" to "object".json(), "description" to buildString {
                append("Optional workspace-relative paths for the outputs, by output name (${outputs.joinToString { it.name }}). ")
                append("Outputs without a path go to $OUTPUT_DIRECTORY/$shortName/ with an automatic name. An existing file is not replaced.")
            }.json(), "properties" to JsonObject().apply { outputs.forEach { output ->
                add(output.name, jsonObject("type" to "string".json(), "minLength" to 1.json(), "maxLength" to 4096.json(),
                    "description" to (if (output.directory) "A directory path" else output.mimeType.ifBlank { "A file path" }).json()))
            } }, "additionalProperties" to false.json()))
            InputSchema(schema, external = true)
            val summary = tool.string("description")?.takeIf { it.isNotBlank() } ?: tool.string("title")?.takeIf { it.isNotBlank() } ?: name
            val description = buildString {
                append("Untrusted AutoJs6 plugin tool from ").append(provider).append(": ").append(summary.trim())
                if (outputs.isNotEmpty()) append(" Writes ").append(outputs.joinToString { "${it.name} (${if (it.directory) "a directory" else it.mimeType})" })
                    .append(" into the workspace and returns the paths.")
            }
            val readOnly = risk == "readonly"
            val entry = PluginToolEntry(name, toolName, shortName, provider, timeoutMs, outputs, images, paths, takesOutputPaths)
            entry to ToolSpec.external(name, AgentJson.truncate(description, MAX_DESCRIPTION_BYTES), schema,
                if (readOnly) RiskLevel.READ_ONLY else RiskLevel.SENSITIVE, ExternalToolRoute(provider, name, ExternalSource.PLUGIN),
                if (readOnly) ToolGroup.PLUGIN else ToolGroup.PLUGIN_WRITE)
        }.getOrNull()

        /** The host still validates exclusive bounds against the plugin's own schema; Stove's dialect has no such keyword. */
        private fun withoutExclusiveBounds(schema: JsonObject) {
            schema.remove("exclusiveMinimum"); schema.remove("exclusiveMaximum")
            schema["properties"]?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.forEach { (_, child) ->
                if (child.isJsonObject) withoutExclusiveBounds(child.asJsonObject)
            }
            listOf("items", "additionalProperties").forEach { key -> schema[key]?.takeIf { it.isJsonObject }?.let { withoutExclusiveBounds(it.asJsonObject) } }
        }

        fun extension(mimeType: String): String = when (mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "text/plain" -> "txt"
            "application/json" -> "json"
            "text/csv" -> "csv"
            "application/pdf" -> "pdf"
            "application/zip" -> "zip"
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            "audio/mp4", "audio/x-m4a" -> "m4a"
            "audio/mpeg" -> "mp3"
            "audio/ogg" -> "ogg"
            "audio/flac" -> "flac"
            else -> "bin"
        }

        private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray()
    }
}

/** The plugin tools frozen for one task; [wrap] routes their calls and passes every other tool to the delegate. */
internal class PluginToolSnapshot(val catalog: ToolCatalog, val entries: Map<String, PluginToolEntry>, private val bridge: PluginToolBridge,
                                  private val scheduler: RunScheduler, private val clock: () -> Long, zone: TimeZone, private val maximumTimeoutMs: Long) {
    private val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).apply { timeZone = zone }
    private var lastStamp = ""
    private var sequence = 0

    private class Inspected(val owner: PluginToolSnapshot, val entry: PluginToolEntry, val validated: JsonObject, val arguments: JsonObject,
                            val outputPaths: Map<String, String>, val permissions: List<String>)

    fun wrap(delegate: RunTools): RunTools = object : RunTools {
        override fun prepare(invocation: ToolInvocation, timeoutMs: Long, callback: (PortResult<PreparedTool>) -> Unit): Cancellation {
            val entry = entries[invocation.name] ?: return delegate.prepare(invocation, timeoutMs, callback)
            val plan = invocation.plan as? ToolPlan.External
            val inspected = if (plan == null || plan.serverId != entry.provider || plan.toolName != entry.name) null
                else runCatching { inspect(entry, checkNotNull(catalog[entry.name]).validator.validate(plan.arguments).asJsonObject) }.getOrNull()
            if (inspected == null) callback(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID))
            else callback(PortResult.Success(PreparedTool(invocation, ToolMetadata(pluginTimeoutMs = stepTimeout(entry)), inspected)))
            return Cancellation.NONE
        }
        override fun execute(prepared: PreparedTool, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
            val entry = entries[prepared.invocation.name] ?: return delegate.execute(prepared, timeoutMs, callback)
            val inspected = prepared.opaqueContext as? Inspected
            val plan = prepared.invocation.plan as? ToolPlan.External
            if (inspected == null || inspected.owner !== this@PluginToolSnapshot || inspected.entry !== entry || plan == null ||
                plan.serverId != entry.provider || plan.toolName != entry.name || plan.arguments != inspected.validated) {
                callback(PortResult.Failure(RunError.TOOL_ARGUMENTS_INVALID)); return Cancellation.NONE
            }
            return invoke(inspected, timeoutMs, callback)
        }
    }

    /** The step limit D87 gives a plugin tool: its own limit plus 2 s for the host, within the grant and the contract. */
    fun stepTimeout(entry: PluginToolEntry): Long = minOf(entry.timeoutMs + 2_000, maximumTimeoutMs, RunLimits.TOOL_TIMEOUT_MS).coerceAtLeast(1)

    private fun inspect(entry: PluginToolEntry, validated: JsonObject): Inspected {
        val arguments = validated.deepCopy()
        val requested = if (entry.takesOutputPaths) arguments.remove(PluginToolSource.OUTPUT_PATHS)?.asJsonObject else null
        val outputPaths = requested?.entrySet()?.associate { (name, value) ->
            require(entry.outputs.any { it.name == name })
            name to value.asString.also(WorkspacePath::requireValid)
        }.orEmpty()
        var screen = false
        var files = false
        arguments.entrySet().forEach { (key, value) ->
            if (key !in entry.imageParameters && key !in entry.pathParameters) return@forEach
            if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return@forEach
            if (key in entry.imageParameters && value.asString == "screen") screen = true
            else { WorkspacePath.requireValid(value.asString); files = true }
        }
        val permissions = buildList {
            add(PluginToolSource.TOKEN)
            if (files || entry.outputs.isNotEmpty()) add("files")
            if (screen) add("screen_capture")
            if (entry.outputs.isNotEmpty()) add("files.write")
        }
        return Inspected(this, entry, validated, arguments, outputPaths, permissions)
    }

    /** Default output paths of D88, unique within this task even for calls in the same millisecond. */
    private fun outputs(inspected: Inspected): Map<String, String> {
        val entry = inspected.entry
        if (entry.outputs.isEmpty()) return emptyMap()
        val suffix = synchronized(this) {
            val now = stamp.format(Date(clock()))
            sequence = if (now == lastStamp) sequence + 1 else 0
            lastStamp = now
            now + if (sequence > 0) "-$sequence" else ""
        }
        return entry.outputs.associate { output ->
            val base = "${PluginToolSource.OUTPUT_DIRECTORY}/${entry.shortName}/${entry.tool}-$suffix${if (entry.outputs.size > 1) "-${output.name}" else ""}"
            output.name to (inspected.outputPaths[output.name] ?: if (output.directory) base else "$base.${PluginToolSource.extension(output.mimeType)}")
        }
    }

    private fun invoke(inspected: Inspected, timeoutMs: Long, callback: (PortResult<ToolReply>) -> Unit): Cancellation {
        val entry = inspected.entry
        // D87: the adapter answers before the runner's step limit, so a slow plugin is one failed call, not a failed task.
        if (timeoutMs < MIN_STEP_MS) { callback(PortResult.Success(failure(entry, "timeout", "TIMEOUT", "Not enough time is left in this task for the tool."))); return Cancellation.NONE }
        val invocationId = "stove-${UUID.randomUUID()}"
        val bridgeTimeout = timeoutMs - 500
        val options = jsonObject("invocationId" to invocationId.json(), "timeoutMs" to (bridgeTimeout - 1_000).json(), "overwrite" to false.json())
        val paths = outputs(inspected)
        if (paths.isNotEmpty()) options.add("outputs", JsonObject().apply { paths.forEach { (name, path) -> addProperty(name, path) } })
        val request = BridgeCall(PluginToolSource.MODULE, "invoke", jsonArray(entry.name.json(), inspected.arguments.deepCopy(), options),
            inspected.permissions, bridgeTimeout)
        val done = AtomicBoolean()
        val guard = AtomicReference(Cancellation.NONE)
        val call = AtomicReference(Cancellation.NONE)
        fun cancelRemote() {
            runCatching { bridge.call(BridgeCall(PluginToolSource.MODULE, "cancel", jsonArray(invocationId.json()), listOf(PluginToolSource.TOKEN), CANCEL_TIMEOUT_MS)) { _, _ -> } }
        }
        fun finish(value: PortResult<ToolReply>) {
            if (!done.compareAndSet(false, true)) return
            guard.get().cancel(); callback(value)
        }
        guard.set(scheduler.schedule(timeoutMs - 250) {
            if (done.get()) return@schedule
            call.get().cancel(); cancelRemote()
            finish(PortResult.Success(failure(entry, "timeout", "TIMEOUT", "The tool did not finish within $timeoutMs ms and was asked to stop.")))
        })
        val handle = bridge.call(request) { value, error ->
            finish(when (value) {
                is PortResult.Success -> PortResult.Success(runCatching { success(entry, value.value) }
                    .getOrElse { failure(entry, "providerFailed", null, "AutoJs6 returned a result Stove cannot read.") })
                is PortResult.Failure -> when {
                    error == null -> value
                    error.string("category") == H.ERROR_PROCESS_DEAD -> PortResult.Failure(RunError.HOST_UNAVAILABLE)
                    else -> PortResult.Success(hostFailure(entry, error))
                }
            })
        }
        call.set(handle)
        if (done.get()) handle.cancel()
        return Cancellation {
            if (done.compareAndSet(false, true)) { guard.get().cancel(); call.get().cancel(); cancelRemote() }
        }
    }

    private fun success(entry: PluginToolEntry, value: JsonElement): ToolReply {
        val result = value.asJsonObject
        var truncated = false
        val observation = jsonObject("untrusted" to true.json(), "source" to "plugin".json(), "tool" to entry.name.json(), "provider" to entry.provider.json())
        result["result"]?.let { raw ->
            val compacted = ObservationCompactor.compact(raw, RESULT_BYTES, false)
            if (compacted != raw) truncated = true
            observation.add("result", compacted)
        }
        result.string("text")?.let { text ->
            val bounded = AgentJson.truncate(text, TEXT_BYTES)
            if (bounded != text) truncated = true
            observation.addProperty("text", bounded)
        }
        result["outputs"]?.takeIf { it.isJsonArray }?.asJsonArray?.let { outputs ->
            observation.add("outputs", JsonArray().apply {
                outputs.take(MAX_LISTED_OUTPUTS).filter { it.isJsonObject }.forEach { output ->
                    add(JsonObject().apply { OUTPUT_FIELDS.forEach { key ->
                        output.asJsonObject[key]?.takeIf { it.isJsonPrimitive }?.let { add(key, it) }
                    } })
                }
            })
        }
        result["missingOutputs"]?.takeIf { it.isJsonArray }?.asJsonArray?.let { missing ->
            observation.add("missingOutputs", JsonArray().apply { missing.take(MAX_LISTED_OUTPUTS).filter { it.isJsonPrimitive }.forEach(::add) })
        }
        result["durationMs"]?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.let { observation.add("durationMs", it) }
        observation.addProperty("truncated", truncated)
        return ToolReply(observation)
    }

    private fun hostFailure(entry: PluginToolEntry, error: JsonObject): ToolReply {
        val message = error.string("message").orEmpty()
        val match = CODE.find(message)
        return failure(entry, error.string("category") ?: "providerFailed", match?.groupValues?.get(1), match?.groupValues?.get(2) ?: message)
    }

    private fun failure(entry: PluginToolEntry, category: String, code: String?, message: String) = ToolReply(jsonObject(
        "untrusted" to true.json(), "source" to "plugin".json(), "tool" to entry.name.json(), "error" to category.json()).apply {
        code?.let { addProperty("code", it) }
        addProperty("message", AgentJson.truncate(message.trim(), MESSAGE_BYTES))
    }, error = RunError.TOOL_FAILED)

    companion object {
        const val RESULT_BYTES = 8 * 1024
        const val TEXT_BYTES = 12 * 1024
        const val MESSAGE_BYTES = 512
        const val MIN_STEP_MS = 2_000L
        const val CANCEL_TIMEOUT_MS = 5_000L
        private const val MAX_LISTED_OUTPUTS = 16
        private val OUTPUT_FIELDS = listOf("name", "path", "bytes", "mimeType", "kind", "files", "conflictCount")
        private val CODE = Regex("failed with ([A-Z][A-Z0-9_]{0,63}): ?(.*)", RegexOption.DOT_MATCHES_ALL)
    }
}
