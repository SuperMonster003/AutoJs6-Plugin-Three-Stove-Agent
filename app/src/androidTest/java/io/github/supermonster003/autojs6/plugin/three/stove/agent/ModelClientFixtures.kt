package io.github.supermonster003.autojs6.plugin.three.stove.agent

import android.os.Looper
import io.github.supermonster003.autojs6.plugin.three.stove.agent.model.ModelClient
import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Device-test synchronous wait over [ModelClient.generate] (roadmap P11): production only ever passes a callback.
 * It refuses the main thread, where a blocking wait would stall the callback, and relies on the client's own
 * scheduler deadline for the timeout outcome; a call that has not settled after the grace period is cancelled.
 */
internal fun ModelClient.awaitReply(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long, graceMs: Long = 5000): PortResult<ModelReply> {
    check(Looper.myLooper() != Looper.getMainLooper()) { "Model wait requires an independent worker thread" }
    val latch = CountDownLatch(1)
    val result = AtomicReference<PortResult<ModelReply>?>(null)
    val cancellation = generate(input, maximumOutputTokens, timeoutMs) { if (result.compareAndSet(null, it)) latch.countDown() }
    if (!latch.await(timeoutMs + graceMs, TimeUnit.MILLISECONDS)) { cancellation.cancel(); latch.await(graceMs, TimeUnit.MILLISECONDS) }
    return checkNotNull(result.get()) { "The model call did not settle" }
}
