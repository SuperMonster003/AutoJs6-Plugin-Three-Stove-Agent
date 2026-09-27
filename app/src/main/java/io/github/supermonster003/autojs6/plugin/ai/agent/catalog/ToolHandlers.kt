package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.autojs.plugin.ai.agent.api.AiAgentContract

data class BridgeCall(val module: String, val method: String, val args: JsonArray, val permissions: List<String>, val timeoutMs: Long = AiAgentContract.DEFAULT_TOOL_TIMEOUT_MS) {
    fun envelope(id: String): JsonObject = jsonObject("id" to id.json(), "module" to module.json(), "method" to method.json(),
        "args" to args.deepCopy(), "permissions" to JsonArray().apply { permissions.forEach { add(it) } }, "timeoutMs" to timeoutMs.json())
}

/** P2.1 prepares admitted calls; P2.3/P3/P4 supply execution, confirmation and observation flows. */
sealed interface ToolPlan {
    data class Call(val request: BridgeCall, val resultLimit: Int? = null) : ToolPlan
    data class Poll(val request: BridgeCall, val state: String, val deadlineMs: Long, val intervalMs: Long = 200) : ToolPlan
    data class Repeat(val request: BridgeCall, val times: Int) : ToolPlan
    data class AppendText(val target: JsonObject, val text: String) : ToolPlan
    data class RegisteredScript(val manifest: BridgeCall, val execution: BridgeCall) : ToolPlan
    data class DynamicScript(val source: String, val timeoutMs: Long) : ToolPlan {
        override fun toString() = "DynamicScript(sourceBytes=${source.toByteArray(Charsets.UTF_8).size}, timeoutMs=$timeoutMs)"
    }
    data class Local(val name: String, val arguments: JsonObject) : ToolPlan
    data class External(val serverId: String, val toolName: String, val arguments: JsonObject) : ToolPlan
}

class ToolHandlers(private val catalog: ToolCatalog) {
    fun prepare(name: String, arguments: JsonObject, policy: ToolPolicy): ToolPlan {
        val spec = policy.requireEnabled(catalog, name)
        if (arguments.toString().toByteArray(Charsets.UTF_8).size > 64 * 1024) invalid("Tool arguments exceed the byte limit.")
        val args = try { spec.validator.validate(arguments).asJsonObject } catch (_: IllegalArgumentException) {
            throw ToolFailure("TOOL_ARGUMENTS_INVALID", "Arguments must match the tool input schema.")
        }
        spec.external?.let { return ToolPlan.External(it.serverId, it.toolName, args) }
        fun str(key: String) = checkNotNull(args.string(key))
        fun num(key: String) = checkNotNull(args.number(key))
        if (spec.group == ToolGroup.FILES) WorkspacePath.requireValid(str("path"))
        fun call(method: String, vararg values: JsonElement, timeout: Long = AiAgentContract.DEFAULT_TOOL_TIMEOUT_MS): BridgeCall {
            require(method in spec.bridgeMapping) { "Undeclared bridge method" }
            return bridge(method, jsonArray(*values), timeout)
        }
        fun target(): JsonObject {
            val ref = args.string("nodeRef")
            val selector = args.getAsJsonObject("selector")
            if ((ref != null) == (selector != null)) invalid("Choose exactly one nodeRef or selector.")
            if (args.has("snapshotId") && ref == null) invalid("snapshotId is only valid with nodeRef. Omit snapshotId when using selector.")
            if (selector != null) { validateSelector(selector); return selector }
            if (!checkNotNull(ref).matches(Regex("#n[1-9][0-9]*"))) invalid("nodeRef must exactly match an observed reference such as #n12, including the leading #.")
            return jsonObject("nodeRef" to ref.json()).apply { args["snapshotId"]?.let { add("snapshotId", it) } }
        }
        return when (name) {
            ToolNames.UI_DUMP -> ToolPlan.Call(call("accessibility.dump", args.apply { addProperty("format", "compact") }))
            ToolNames.UI_FIND -> { val selector = args.getAsJsonObject("selector"); validateSelector(selector); ToolPlan.Call(call("accessibility.findAll", selector), num("limit").toInt()) }
            ToolNames.UI_WAIT_FOR -> { val selector = args.getAsJsonObject("selector"); validateSelector(selector); ToolPlan.Poll(call("accessibility.findOne", selector, timeout = minOf(num("timeoutMs"), 5000)), str("state"), num("timeoutMs")) }
            ToolNames.APP_CURRENT -> ToolPlan.Call(call("app.currentWindow"))
            ToolNames.SCREEN_STATE -> ToolPlan.Call(call("device.isScreenOn"))
            ToolNames.SCREEN_CAPTURE -> ToolPlan.Call(call("accessibility.screenshot", jsonObject("format" to "png".json())))
            ToolNames.DEVICE_INFO -> ToolPlan.Call(call("device.info"))
            ToolNames.CONSOLE_TAIL -> ToolPlan.Call(call("console.tail", jsonObject("lines" to num("lines").json())))
            ToolNames.OCR_SCREEN -> {
                args.getAsJsonArray("region")?.let { if (it[2].asLong <= 0 || it[3].asLong <= 0) invalid("Region width and height must be positive.") }
                ToolPlan.Call(call("accessibility.readScreenText", args))
            }
            ToolNames.UI_CLICK, ToolNames.UI_LONG_CLICK -> ToolPlan.Call(call(if (name == ToolNames.UI_CLICK) "accessibility.click" else "accessibility.longClick", target()))
            ToolNames.UI_SET_TEXT -> {
                val target = target()
                if (args.flag("append") == true) ToolPlan.AppendText(target, str("text"))
                else ToolPlan.Call(call("accessibility.setText", target, str("text").json()))
            }
            ToolNames.UI_SCROLL -> ToolPlan.Repeat(call(if (str("direction") == "forward") "accessibility.scrollForward" else "accessibility.scrollBackward", target()), num("times").toInt())
            ToolNames.UI_PRESS_KEY -> ToolPlan.Call(call(when (str("key")) {
                "back" -> "accessibility.back"; "home" -> "accessibility.home"; "recents" -> "accessibility.recentApps"
                "notifications" -> "keys.notifications"; else -> "keys.quickSettings"
            }))
            ToolNames.APP_LAUNCH -> {
                if (args.has("packageName") == args.has("appName")) invalid("Choose packageName or appName.")
                if (args.has("packageName")) ToolPlan.Call(call("app.launchPackage", str("packageName").json()))
                else ToolPlan.Call(call("app.launchApp", str("appName").json()))
            }
            ToolNames.CLIPBOARD_GET -> ToolPlan.Call(call("clipboard.getText"))
            ToolNames.CLIPBOARD_SET -> ToolPlan.Call(call("clipboard.setText", str("text").json()))
            ToolNames.UI_CLICK_XY -> ToolPlan.Call(call("accessibility.swipe", num("x").json(), num("y").json(), num("x").json(), num("y").json(), 100.json()))
            ToolNames.UI_SWIPE -> ToolPlan.Call(call("accessibility.swipe", num("x1").json(), num("y1").json(), num("x2").json(), num("y2").json(), num("durationMs").json(), timeout = num("durationMs") + 5000))
            ToolNames.UI_GESTURE -> ToolPlan.Call(call("accessibility.gesture", num("durationMs").json(), args["points"], timeout = num("durationMs") + 5000))
            ToolNames.SCRIPT_CATALOG -> ToolPlan.Call(call("agent.listScripts", args))
            ToolNames.SCRIPT_RUN -> {
                if (args["parameters"].toString().toByteArray(Charsets.UTF_8).size > 16 * 1024) invalid("Script parameters exceed the byte limit.")
                ToolPlan.RegisteredScript(call("agent.readManifest", str("id").json()), call("agent.execRegistered", str("id").json(), args["parameters"], jsonObject("captureConsole" to true.json()), timeout = AiAgentContract.MAX_TOOL_TIMEOUT_MS))
            }
            ToolNames.SCRIPT_RUN_SOURCE -> {
                try { io.github.supermonster003.autojs6.plugin.ai.agent.scripts.DynamicScriptSource.validate(str("source")) }
                catch (_: IllegalArgumentException) { invalid("Use nonempty JavaScript, no NUL, at most 8192 UTF-8 bytes including JSON escaping.") }
                ToolPlan.DynamicScript(str("source"), num("timeoutMs"))
            }
            ToolNames.SCRIPT_STOP -> ToolPlan.Call(call("engines.stop", num("executionId").json()))
            ToolNames.FILES_LIST -> ToolPlan.Call(call("files.list", str("path").json()))
            ToolNames.FILES_STAT -> ToolPlan.Call(call("files.stat", str("path").json()))
            ToolNames.FILES_READ -> ToolPlan.Call(call("files.read", str("path").json(), jsonObject("maxBytes" to num("maxBytes").json())))
            ToolNames.FILES_WRITE -> ToolPlan.Call(call("files.write", str("path").json(), str("content").json(), jsonObject("overwrite" to checkNotNull(args.flag("overwrite")).json())))
            ToolNames.SHELL_EXEC -> ToolPlan.Call(call("shell.exec", str("cmd").json(), jsonObject("root" to false.json(), "timeoutMs" to num("timeoutMs").json()), timeout = num("timeoutMs")))
            ToolNames.MEMORY_GET, ToolNames.MEMORY_PROPOSE, ToolNames.REPORT_PROGRESS -> ToolPlan.Local(name, args)
            else -> throw ToolFailure("TOOL_UNKNOWN", "No handler for this tool.")
        }
    }

    private fun validateSelector(selector: JsonObject) {
        if (selector.size() == 0 || selector.entrySet().all { it.value.isJsonPrimitive && it.value.asJsonPrimitive.isString && it.value.asString.isBlank() }) invalid("Selector must contain a condition.")
        for (key in listOf("textMatches", "descMatches", "idMatches", "classNameMatches")) {
            selector.string(key)?.let { try { Regex(it) } catch (_: IllegalArgumentException) { invalid("Invalid selector pattern.") } }
        }
        for (key in listOf("boundsInside", "boundsContains")) selector.getAsJsonObject(key)?.let {
            if (it.number("left")!! > it.number("right")!! || it.number("top")!! > it.number("bottom")!!) invalid("Inverted bounds.")
        }
    }

    private fun invalid(hint: String): Nothing = throw ToolFailure("TOOL_ARGUMENTS_INVALID", hint)

    companion object {
        fun bridge(method: String, args: JsonArray, timeoutMs: Long = AiAgentContract.DEFAULT_TOOL_TIMEOUT_MS): BridgeCall {
            val (module, operation) = method.split('.', limit = 2)
            val permissions = when (module) {
                "accessibility" -> when (operation) {
                    "swipe", "gesture" -> listOf("accessibility", "accessibility.gesture")
                    "readScreenText" -> listOf("accessibility", "screen_capture", "ocr")
                    "screenshot" -> listOf("accessibility", "screen_capture")
                    else -> listOf("accessibility")
                }
                "agent" -> if (operation == "execRegistered") listOf("agent", "agent.exec", "engines", "engines.exec") else listOf("agent")
                "app" -> if (operation == "currentWindow") listOf("app.query", "accessibility") else listOf("app.launch")
                "files" -> if (operation == "write") listOf("files", "files.write") else listOf("files")
                "shell" -> listOf("shell")
                "engines" -> if (operation == "execScript") listOf("engines", "engines.exec", "agent", "agent.exec") else listOf("engines")
                else -> listOf(module)
            }
            return BridgeCall(module, operation, args.deepCopy(), permissions, timeoutMs)
        }
    }
}
