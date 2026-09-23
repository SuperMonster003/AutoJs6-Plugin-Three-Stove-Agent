package io.github.supermonster003.autojs6.plugin.ai.agent.catalog

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

data class BridgeCall(val module: String, val method: String, val args: JsonArray, val permissions: List<String>, val timeoutMs: Long = 30_000) {
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
    data class Local(val name: String, val arguments: JsonObject) : ToolPlan
}

class ToolHandlers(private val catalog: ToolCatalog) {
    fun prepare(name: String, arguments: JsonObject, policy: ToolPolicy): ToolPlan {
        val spec = policy.requireEnabled(catalog, name)
        if (arguments.toString().toByteArray(Charsets.UTF_8).size > 64 * 1024) invalid("Tool arguments exceed the byte limit.")
        val args = try { spec.validator.validate(arguments).asJsonObject } catch (_: IllegalArgumentException) {
            throw ToolFailure("TOOL_ARGUMENTS_INVALID", "Arguments must match the tool input schema.")
        }
        fun str(key: String) = checkNotNull(args.string(key))
        fun num(key: String) = checkNotNull(args.number(key))
        fun call(method: String, vararg values: JsonElement, timeout: Long = 30_000): BridgeCall {
            require(method in spec.bridgeMapping) { "Undeclared bridge method" }
            return bridge(method, jsonArray(*values), timeout)
        }
        fun target(): JsonObject {
            val ref = args.string("nodeRef")
            val selector = args.getAsJsonObject("selector")
            if ((ref != null) == (selector != null) || (args.has("snapshotId") && ref == null)) invalid("Choose exactly one nodeRef or selector.")
            if (selector != null) { validateSelector(selector); return selector }
            if (!checkNotNull(ref).matches(Regex("#n[1-9][0-9]*"))) invalid("Invalid node reference.")
            return jsonObject("nodeRef" to ref.json()).apply { args["snapshotId"]?.let { add("snapshotId", it) } }
        }
        return when (name) {
            "ui_dump" -> ToolPlan.Call(call("accessibility.dump", args.apply { addProperty("format", "compact") }))
            "ui_find" -> { val selector = args.getAsJsonObject("selector"); validateSelector(selector); ToolPlan.Call(call("accessibility.findAll", selector), num("limit").toInt()) }
            "ui_wait_for" -> { val selector = args.getAsJsonObject("selector"); validateSelector(selector); ToolPlan.Poll(call("accessibility.findOne", selector, timeout = minOf(num("timeoutMs"), 5000)), str("state"), num("timeoutMs")) }
            "app_current" -> ToolPlan.Call(call("app.currentWindow"))
            "screen_state" -> ToolPlan.Call(call("device.isScreenOn"))
            "device_info" -> ToolPlan.Call(call("device.info"))
            "console_tail" -> ToolPlan.Call(call("console.tail", jsonObject("lines" to num("lines").json())))
            "ocr_screen" -> {
                args.getAsJsonArray("region")?.let { if (it[2].asLong <= 0 || it[3].asLong <= 0) invalid("Region width and height must be positive.") }
                ToolPlan.Call(call("accessibility.readScreenText", args))
            }
            "ui_click", "ui_long_click" -> ToolPlan.Call(call(spec.bridgeMapping.single(), target()))
            "ui_set_text" -> {
                val target = target()
                if (args.flag("append") == true) ToolPlan.AppendText(target, str("text"))
                else ToolPlan.Call(call("accessibility.setText", target, str("text").json()))
            }
            "ui_scroll" -> ToolPlan.Repeat(call(if (str("direction") == "forward") "accessibility.scrollForward" else "accessibility.scrollBackward", target()), num("times").toInt())
            "ui_press_key" -> ToolPlan.Call(call(when (str("key")) {
                "back" -> "accessibility.back"; "home" -> "accessibility.home"; "recents" -> "accessibility.recentApps"
                "notifications" -> "keys.notifications"; else -> "keys.quickSettings"
            }))
            "app_launch" -> {
                if (args.has("packageName") == args.has("appName")) invalid("Choose packageName or appName.")
                if (args.has("packageName")) ToolPlan.Call(call("app.launchPackage", str("packageName").json()))
                else ToolPlan.Call(call("app.launchApp", str("appName").json()))
            }
            "clipboard_get" -> ToolPlan.Call(call("clipboard.getText"))
            "clipboard_set" -> ToolPlan.Call(call("clipboard.setText", str("text").json()))
            "ui_click_xy" -> ToolPlan.Call(call("accessibility.swipe", num("x").json(), num("y").json(), num("x").json(), num("y").json(), 100.json()))
            "ui_swipe" -> ToolPlan.Call(call("accessibility.swipe", num("x1").json(), num("y1").json(), num("x2").json(), num("y2").json(), num("durationMs").json(), timeout = num("durationMs") + 5000))
            "ui_gesture" -> ToolPlan.Call(call("accessibility.gesture", num("durationMs").json(), args["points"], timeout = num("durationMs") + 5000))
            "script_catalog" -> ToolPlan.Call(call("agent.listScripts", args))
            "script_run" -> {
                if (args["parameters"].toString().toByteArray(Charsets.UTF_8).size > 16 * 1024) invalid("Script parameters exceed the byte limit.")
                ToolPlan.RegisteredScript(call("agent.readManifest", str("id").json()), call("agent.execRegistered", str("id").json(), args["parameters"], jsonObject("captureConsole" to true.json()), timeout = 300_000))
            }
            "script_stop" -> ToolPlan.Call(call("engines.stop", num("executionId").json()))
            "files_list" -> ToolPlan.Call(call("files.list", str("path").json()))
            "files_stat" -> ToolPlan.Call(call("files.stat", str("path").json()))
            "files_read" -> ToolPlan.Call(call("files.read", str("path").json(), jsonObject("maxBytes" to num("maxBytes").json())))
            "files_write" -> ToolPlan.Call(call("files.write", str("path").json(), str("content").json(), jsonObject("overwrite" to checkNotNull(args.flag("overwrite")).json())))
            "shell_exec" -> ToolPlan.Call(call("shell.exec", str("cmd").json(), jsonObject("root" to false.json(), "timeoutMs" to num("timeoutMs").json()), timeout = num("timeoutMs")))
            "memory_get", "memory_propose", "report_progress" -> ToolPlan.Local(name, args)
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
        fun bridge(method: String, args: JsonArray, timeoutMs: Long = 30_000): BridgeCall {
            val (module, operation) = method.split('.', limit = 2)
            val permissions = when (module) {
                "accessibility" -> when (operation) {
                    "swipe", "gesture" -> listOf("accessibility", "accessibility.gesture")
                    "readScreenText" -> listOf("accessibility", "screen_capture", "ocr")
                    else -> listOf("accessibility")
                }
                "agent" -> if (operation == "execRegistered") listOf("agent", "agent.exec", "engines", "engines.exec") else listOf("agent")
                "app" -> if (operation == "currentWindow") listOf("app.query", "accessibility") else listOf("app.launch")
                "files" -> if (operation == "write") listOf("files", "files.write") else listOf("files")
                "shell" -> listOf("shell")
                else -> listOf(module)
            }
            return BridgeCall(module, operation, args.deepCopy(), permissions, timeoutMs)
        }
    }
}
