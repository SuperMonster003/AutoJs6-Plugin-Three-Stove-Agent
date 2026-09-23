# P2.4 context compiler and model client evidence

## Context compiler

Implemented the original ContextCompiler item without changing roadmap stages.
The compiler retains system rules, the full goal and a marked current observation,
packs deterministic summaries before complete recent pairs, and places remaining
budgets last. All costs include JSON escaping, message envelopes and response
schema bytes. The effective ceiling is the minimum of the configured limit,
target context size, grant and the local 3000-token input allowance (0.4 token per
UTF-8 byte). An impossible minimum fails with LIMIT_EXCEEDED before model dispatch.

Local observations omit bounds and pure containers, prefer actionable/text nodes
and retain at most 70 complete rows. Selection runs before the journal's 24 KiB
observation cap, preserving useful nodes beyond a long container prefix. Node
references, snapshot identity and quoted screen text are retained. The compact
tool signatures derive from the same ToolCatalog schemas as runtime validation;
repeated selector schemas appear once. Historical pairs are removed before the
current observation is shortened. Optional memories and fixed context are marked
when trimmed. No model summary call is made.

Validation on 2026-09-23: JVM 183/183, including 19 new context, observation and
target tests. The default local empty-history fixture uses 5025 bytes / 2010
estimated tokens for English and 4839 bytes / 1936 tokens for Chinese, including
the response schema. Input plus admitted output stays within the local 4096-token
window. These are deterministic size estimates, not live model performance.

Public target metadata provides locality and structured-JSON capabilities but no
online wire protocol. Local targets are selected using locality; remote/hybrid
targets keep UNKNOWN protocol and plain decision JSON. Names, package names and
target IDs never imply a protocol. Explicit protocol variants remain available
for a future negotiated extension. No host contract or Provider binding is added.
