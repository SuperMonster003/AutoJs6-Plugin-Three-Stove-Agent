package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.ToolPolicy
import io.github.supermonster003.autojs6.plugin.ai.agent.model.*
import org.junit.Assert.*
import org.junit.Test

class PreparedObservationPolicyTest {
    @Test fun preparedAvailabilityControlsDecisionAdmissionBeforeTheFirstModelReply() {
        val initial = ToolPolicy()
        val f = RunnerFixture(initial)
        val compiler = RunContextCompiler { ModelInput(jsonArray(jsonObject("role" to "user".json(), "content" to "Observe".json()))) }
        f.enqueue(RunnerFixture.tool("ocr_screen"), RunnerFixture.done(evidence = listOf("Screen text observed")))
        val run = f.queue.submitPrepared(f.options(), initial, RunPreparation { callback ->
            callback(PortResult.Success(RunComponents(compiler, f.model, f.tools, policy = initial.withOcrAvailability(true))))
            Cancellation.NONE
        })
        f.scheduler.drain()
        assertEquals(RunState.COMPLETED, run.state)
        assertEquals(listOf("ocr_screen"), f.tools.executions.map { it.first.invocation.name })
    }
}
