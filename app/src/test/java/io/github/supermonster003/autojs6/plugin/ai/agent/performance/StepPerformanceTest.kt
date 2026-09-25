package io.github.supermonster003.autojs6.plugin.ai.agent.performance

import com.google.gson.JsonObject
import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import io.github.supermonster003.autojs6.plugin.ai.agent.core.CoreFixtures as F
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import io.github.supermonster003.autojs6.plugin.ai.agent.runner.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.math.ceil

/** Opt-in wall-clock baseline, separate from ordinary correctness CI. No model or network calls. */
class StepPerformanceTest {
    @Test fun compileParseAndValidateRepresentativeSteps() {
        assumeTrue("Set AUTOJS_AGENT_PERFORMANCE=true", System.getenv("AUTOJS_AGENT_PERFORMANCE") == "true")
        val catalog = F.catalog()
        val policy = ToolPolicy.fromAssets(F::asset)
        val prompts = PromptCatalog(F::asset, catalog)
        val validator = DecisionValidator(catalog)
        for (local in listOf(false, true)) for (language in listOf("en", "zh")) for (records in listOf(0, 32)) {
            val target = ModelTarget("fixture", "fixture:performance", if (local) ModelLocality.ON_DEVICE else ModelLocality.REMOTE,
                if (local) ModelProtocol.LOCAL else ModelProtocol.OPENAI, true, 128 * 1024)
            val format = DecisionSchema(catalog).generate(target.protocol, policy)
            val compiler = ContextCompiler(prompts, catalog, policy, target, format)
            val label = if (language == "zh") "项目" else "Item"
            val tree = "window: pkg=fixture\n" + (1..200).joinToString("\n") { "#n$it TextView \"$label $it\" c=(10,20)" }
            val observation = compiler.observe("ui_dump", jsonObject("nodeCount" to 200.json(), "text" to tree.json()))
            val history = (1..records).map { index -> jsonObject("index" to index.json(), "kind" to "tool".json(), "tool" to "ui_dump".json(),
                "decision" to jsonObject("kind" to "tool".json(), "tool" to "ui_dump".json(), "arguments" to JsonObject()),
                "observation" to observation.json(), "confirmation" to "auto".json()) }
            val context = RunContext(if (language == "zh") "读取当前界面中的项目" else "Read the items on the current screen", history, observation, null,
                jsonObject("steps" to 40.json(), "modelCalls" to 50.json(), "tokens" to 90000.json(), "durationMs" to 600000.json()), format, language)
            val arguments = jsonObject("maxNodes" to 200.json())
            val decision = jsonObject("kind" to "tool".json(), "tool" to "ui_dump".json(),
                "arguments" to if (format.argumentsEncoding == ArgumentsEncoding.JSON_STRING) arguments.toString().json() else arguments).toString()
            fun step(): Long {
                val start = System.nanoTime()
                val input = compiler.compile(context)
                val parsed = DecisionParser.parse(decision, format.degraded)
                val validated = validator.validate(parsed, policy, format)
                val elapsed = System.nanoTime() - start
                assertTrue(input.inputBytes <= compiler.maximumBytes)
                assertEquals("ui_dump", (validated as AgentDecision.Tool).name)
                consumed = input.inputBytes
                return elapsed
            }
            val cold = step()
            repeat(100) { step() }
            val samples = LongArray(300) { step() }.sorted()
            fun percentile(p: Double) = samples[(ceil(samples.size * p).toInt() - 1).coerceAtLeast(0)] / 1e6
            val report = jsonObject("local" to local.json(), "language" to language.json(), "history" to records.json(),
                "warmup" to 100.json(), "samples" to samples.size.json(), "coldMs" to (cold / 1e6).json(),
                "meanMs" to (samples.average() / 1e6).json(), "p50Ms" to percentile(.5).json(), "p95Ms" to percentile(.95).json(),
                "p99Ms" to percentile(.99).json(), "maxMs" to (samples.last() / 1e6).json(), "inputBytes" to consumed.json())
            println("P7_STEP $report")
            assertTrue("Warm p95 exceeds the 20 ms baseline: $report", percentile(.95) < 20)
        }
    }
    companion object { @Volatile private var consumed = 0 }
}
