package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

fun interface Cancellation {
    fun cancel()
    companion object { val NONE = Cancellation {} }
}

/** execute always queues, including calls from the scheduler itself. Tasks run serially. */
interface RunScheduler {
    fun nowMs(): Long
    fun execute(action: () -> Unit)
    fun schedule(delayMs: Long, action: () -> Unit): Cancellation
}

/** Owned by one link. Shut it down only after runs have received detach and settled. */
class SerialRunScheduler : RunScheduler, AutoCloseable {
    private val executor = ScheduledThreadPoolExecutor(1) { runnable -> Thread(runnable, "ai-agent-runner").apply { isDaemon = true } }
        .apply { removeOnCancelPolicy = true; setExecuteExistingDelayedTasksAfterShutdownPolicy(false) }
    override fun nowMs() = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
    override fun execute(action: () -> Unit) { executor.execute(action) }
    override fun schedule(delayMs: Long, action: () -> Unit): Cancellation {
        require(delayMs >= 0)
        val future = executor.schedule(action, delayMs, TimeUnit.MILLISECONDS)
        return Cancellation { future.cancel(false) }
    }
    override fun close() { executor.shutdown() }
}
