package io.github.supermonster003.autojs6.plugin.ai.agent.runner

import io.github.supermonster003.autojs6.plugin.ai.agent.model.*

/** Structural completion checks, not a claim that natural-language evidence has been proven. */
class DoneRules(private val text: RunnerText) {
    var orderStatusRequired = false
        private set

    fun requireOrderStatus() { orderStatusRequired = true }

    /** A missing order state uses the existing per-step repair allowance. Never invent a state. */
    fun validate(decision: AgentDecision) {
        if (decision is AgentDecision.Done && orderStatusRequired && decision.orderStatus == null) {
            throw DecisionFailure("DECISION_UNPARSABLE", "This order/payment task requires done.orderStatus: none, cart, pending_payment, submitted or paid. Use observed state, never guess; observe or ask if unknown.")
        }
    }

    fun normalize(value: AgentDecision.Done): Checked {
        val missingEvidence = value.status == "completed" && value.evidence.isEmpty()
        val unfinishedCompletion = value.status == "completed" && value.unfinished.isNotEmpty()
        val downgrade = missingEvidence || unfinishedCompletion
        val status = if (downgrade) "partial" else value.status
        val rules = mutableListOf<String>()
        var unfinished = value.unfinished
        if (missingEvidence) {
            rules += "EVIDENCE_MISSING"
            unfinished = (listOf(text.rule("evidence_missing")) + unfinished).distinct().take(8)
        }
        if (unfinishedCompletion) rules += "UNFINISHED_COMPLETION"
        if (status == "partial" && unfinished.isEmpty()) {
            rules += "UNFINISHED_MISSING"
            unfinished = listOf(text.rule("unfinished_missing"))
        }
        return Checked(value.copy(status = status, summary = if (downgrade) text.rule("unverified_summary") else value.summary,
            unfinished = unfinished), rules)
    }

    data class Checked(val decision: AgentDecision.Done, val rules: List<String>)
}
