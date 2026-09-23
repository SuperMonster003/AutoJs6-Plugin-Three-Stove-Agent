package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.catalog.*
import java.util.UUID

class RunAdmissionFailure(val error: RunError) : IllegalStateException(error.name)

/** One active run (including user waits), eight pending runs, no terminal-history retention. */
class RunQueue(private val scheduler: RunScheduler, private val catalog: ToolCatalog, private val policy: ToolPolicy,
               private val compiler: RunContextCompiler, private val model: RunModel, private val tools: RunTools,
               private val textForLocale: (String) -> RunnerText) {
    private var active: AgentRunner? = null
    private val pending = ArrayDeque<AgentRunner>()
    private var unavailable: RunError? = null

    fun submit(options: RunOptions, listener: (RunEvent) -> Unit = {}): AgentRunner = submitPrepared(options, policy, null, listener = listener)
    @Synchronized fun submitPrepared(options: RunOptions, runPolicy: ToolPolicy, preparation: RunPreparation?,
                                     onAdmitted: (AgentRunner) -> Unit = {}, listener: (RunEvent) -> Unit = {}): AgentRunner {
        unavailable?.let { throw RunAdmissionFailure(it) }
        if (active != null && pending.size >= RunLimits.QUEUED_RUNS) throw RunAdmissionFailure(RunError.QUEUE_FULL)
        val run = AgentRunner(UUID.randomUUID().toString(), options, scheduler, catalog, runPolicy, compiler, model, tools,
            textForLocale(options.locale), listener, ::settled, preparation)
        onAdmitted(run)
        if (active == null) { active = run; run.start() } else pending.addLast(run)
        return run
    }
    @Synchronized fun runs(): List<AgentRunner> = listOfNotNull(active) + pending.toList()
    @Synchronized private fun settled(run: AgentRunner) {
        if (active === run) active = null else pending.remove(run)
        if (unavailable == null && active == null && pending.isNotEmpty()) {
            active = pending.removeFirst().also { it.start() }
        }
    }
    @Synchronized fun hostUnavailable(error: RunError = RunError.HOST_UNAVAILABLE) {
        require(error.hostLost)
        if (unavailable != null) return
        unavailable = error
        runs().forEach { it.hostUnavailable(error) }
    }
    fun detach() = hostUnavailable(RunError.LINK_DETACHED)
}
