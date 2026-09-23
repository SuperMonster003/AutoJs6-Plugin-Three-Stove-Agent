package io.github.supermonster003.autojs6.plugin.ai.agent.scripts

import com.google.gson.*
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

internal object ScriptFixtures {
    fun entry(id: String = "clean-downloads", description: String = "Remove old installers from Downloads") = jsonObject(
        "id" to id.json(), "path" to "/storage/emulated/0/autojs/$id.js".json(), "kind" to "file".json(),
        "description" to description.json(), "parameters" to AgentJson.parse("""{"type":"object","properties":{"days":{"type":"integer","default":30,"minimum":1}},"required":[],"additionalProperties":false}"""),
        "risk" to "normal".json(), "confirm" to "before-run".json(), "timeoutMs" to 60000.json(),
        "updatedAt" to 1000.json(), "examples" to jsonArray("Clean old installers".json(), "Free download space".json(), "Third example".json()),
        "tags" to jsonArray("cleanup".json(), "downloads".json()))
    fun snapshot(vararg values: JsonObject) = ScriptCatalogSnapshot.parse(jsonArray(*values))
}
