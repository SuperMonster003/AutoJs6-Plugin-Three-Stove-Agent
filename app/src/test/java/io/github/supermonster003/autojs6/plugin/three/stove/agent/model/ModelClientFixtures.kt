package io.github.supermonster003.autojs6.plugin.three.stove.agent.model

import io.github.supermonster003.autojs6.plugin.three.stove.agent.runner.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Test-side synchronous wait over [ModelClient.generate] (roadmap P11): production only ever passes a callback.
 * When no reply arrives within [timeoutMs] plus a grace period, [onTimeout] runs first (a virtual scheduler test
 * advances time there so the client's own deadline settles the call), otherwise the call is cancelled and the
 * settled result is returned. An interrupted waiting thread cancels the call and keeps its interrupt flag.
 */
internal fun ModelClient.awaitReply(input: ModelInput, maximumOutputTokens: Int, timeoutMs: Long,
                                    graceMs: Long = 2000, onTimeout: (() -> Unit)? = null): PortResult<ModelReply> {
    val latch = CountDownLatch(1)
    val result = AtomicReference<PortResult<ModelReply>?>(null)
    val cancellation = generate(input, maximumOutputTokens, timeoutMs) { if (result.compareAndSet(null, it)) latch.countDown() }
    try {
        if (!latch.await(timeoutMs + graceMs, TimeUnit.MILLISECONDS)) {
            onTimeout?.invoke()
            if (!latch.await(graceMs, TimeUnit.MILLISECONDS)) { cancellation.cancel(); latch.await(graceMs, TimeUnit.MILLISECONDS) }
        }
    } catch (_: InterruptedException) {
        cancellation.cancel(); latch.await(graceMs, TimeUnit.MILLISECONDS)
        Thread.currentThread().interrupt()
    }
    return checkNotNull(result.get()) { "The model call did not settle" }
}
