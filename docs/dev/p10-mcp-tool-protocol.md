# P10 MCP tool source

This document describes the AI Agent 1.2.0 development implementation. Validation
and remaining acceptance work are recorded separately in
[the P10 evidence](p10-mcp-evidence-2026-09-26.md). It does not introduce another
roadmap phase or the public API of the proposed MCP Client plugin.

## Ownership and configuration

As checked on 2026-09-26, the local MCP Server roadmap appendix E reserves an
Android MCP Client plugin, but no such implementation or host capability proxy
exists. The PC AutoJs6-MCP-Bridge is a stdio-to-HTTP bridge, not that plugin.
`McpToolSource` is an internal, replaceable source for Agent tools. If a compatible
MCP Client capability proxy becomes available, prefer adapting this source to it
instead of maintaining two independent clients.

The `mcp` group starts disabled. The user configures servers, discovers tools,
selects permitted tools and chooses a risk level for each server. Servers also
start disabled and their initial risk is `SENSITIVE`. Settings, preset and run
group restrictions continue to intersect. `tools: ['mcp', 'user']` does not
configure a server, supply credentials, enable a globally disabled group or
approve an action. The current host recognizes `mcp` in public JS options and
defines the additive `TOOL_FAILED` error; the existing V1 AIDL order is unchanged.

| Limit | Value |
|---|---|
| Server profiles | 8 |
| Selected MCP tools across saved profiles | 32 |
| Combined per-run catalog | 64, including 32 built-in tools |
| Profile identifier | Lowercase ASCII, up to 12 characters |
| Profile name / endpoint / token | 80 / 2048 / 4096 UTF-8 bytes |
| Encrypted configuration plaintext | 128 KiB |
| Discovery | 8 pages, 128 tools, 1 MiB total |
| Individual / combined selected schemas | 16 KiB / 128 KiB |

The token input is write-only. Empty input retains an existing token; explicit
clearing removes it. Changing the endpoint removes the old token unless the user
provides a new token. The UI receives only `hasBearerToken`, uses a secure window,
and excludes the token from saved state and autofill. The entire atomic profile
file is encrypted with Android Keystore AES-GCM in the app's no-backup directory.
Authentication or corruption errors do not fall back to plaintext storage.
Configuration writes use revision checks and are rejected during active tasks.
Destroying the private settings service cancels its discovery operations and
closes its owned worker pool. Already accepted repository writes finish under
their maintenance guard even if the settings endpoint has closed.

## Wire protocol

The supported transport is Streamable HTTP for protocol revisions `2025-11-25`,
`2025-06-18` and `2025-03-26`. This is intentionally a stated compatibility range,
not a claim to support every MCP revision. The local MCP Server at the audited
commit `88c2573` uses Kotlin SDK 0.15.0 and supports these revisions. Legacy
HTTP+SSE (`/sse` plus a separate message endpoint), stdio, OAuth, the 2026 protocol
family, prompts, resources and server-initiated sampling are outside this source.

The client POSTs `initialize`, records the accepted protocol and optional
`Mcp-Session-Id`, then sends `notifications/initialized`. Subsequent requests
include the negotiated `MCP-Protocol-Version`, session identifier and configured
Bearer token. It accepts JSON responses and SSE on each POST, ignores bounded
progress/logging notifications, and matches the terminal JSON-RPC response ID.
Notifications accept a successful empty HTTP response. Session deletion is
bounded and best effort. There is no persistent GET notification stream.

The default local address is `http://127.0.0.1:9637/mcp`. HTTP is restricted to
literal loopback endpoints and localhost by both endpoint validation and Android
network policy. Other endpoints require HTTPS with platform certificate
validation. URLs containing user info, queries or fragments are rejected; tokens
belong in the separate credential field. Redirects are rejected and credentials
are not forwarded to a different origin. Android 17+ local-network permission has
a user-initiated settings entry; it is not required as a blanket precondition for
public HTTPS servers.

Each operation has a deadline, response/request size limits and cancellable
connections. `tools/call` is never automatically retried, reinitialized or
replayed after a failure. Cancellation closes local connections and attempts a
bounded `notifications/cancelled`; it cannot establish that a remote side effect
was prevented or reversed. The JSON and native model paths use the same rule.

For the local MCP Server, Bearer authentication does not grant pairing. The first
gated call can return `-32002` and requires approval in MCP Server. Denial or
expiry can return `-32003`. AI Agent does not approve that request, rename its
client to evade it, or replay the original tool automatically. After completing
the server-side pairing, the user can start a new task. HTTP 401/403, pairing,
catalog, protocol, timeout and size failures produce fixed classifications;
remote error prose, tokens and HTTP headers are not copied into diagnostics.

The implementation follows the MCP
[transport](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports),
[lifecycle](https://modelcontextprotocol.io/specification/2025-11-25/basic/lifecycle)
and [tools](https://modelcontextprotocol.io/specification/2025-11-25/server/tools)
specifications within the above bounds. Android's permission behavior is described
in the [local network permission documentation](https://developer.android.com/privacy-and-security/local-network-permission).

## Catalog, validation and execution

Task preparation freezes one catalog, tool policy and server session set. The
same catalog feeds the prompt, JSON decision schema, native definitions,
`DecisionValidator`, handlers and runner. Model and fallback caches include the
catalog fingerprint. A task without the admitted MCP group does not connect to
configured servers. An enabled source failure is reported instead of silently
dropping its selected tools.

Names use `mcp_<server>_<tool>`. Names already fitting the local lowercase format
remain readable; other supported MCP names use a bounded lowercase slug and a
stable digest, while the private route retains the original case-sensitive name.
Collisions are rejected. Descriptions are bounded and identified as untrusted
server data. Server annotations never lower the user-selected risk. Sensitive
and cautious-mode operations pass through the existing confirmation gate.

External schemas keep standard open-object semantics when additional properties
are allowed or unspecified. Built-in schemas retain their closed-object rules.
Supported keywords are `type`, `properties`, `required`, `additionalProperties`,
`items`, `enum`, `default`, `description`, `title`, `minLength`, `maxLength`,
`minItems`, `maxItems`, `minimum` and `maximum`. An empty or annotation-only child
schema accepts bounded JSON values. Types, arrays, schema nesting and values
remain bounded; external defaults are annotations and are not inserted into
arguments. Unknown constraints, including references, patterns and combinators,
are rejected visibly during discovery rather than removed. Tools requiring the
MCP Tasks extension are unavailable. Optional task support does not trigger an
asynchronous task workflow.

Before dispatch, the client lists tools again and compares the selected tool
definitions with the frozen definitions. A changed or missing selection, or an
observed `notifications/tools/list_changed`, invalidates execution. It does not
silently bind to the new definition after confirmation. This check cannot freeze
remote server code or prevent a remote change after the final check.

Results are observations marked as untrusted MCP data. Text is bounded; binary
content becomes an omission marker, with no automatic image upload, URL fetch or
resource read. `structuredContent` is compacted within its own bound; the source
does not validate the server's optional `outputSchema`. `isError: true` produces
`TOOL_FAILED`, an unsuccessful step and a native error result, and does not
increase the successful-tool counter. A normal JSON-RPC tool response is not
itself evidence that the user's task is complete.

Private history retains bounded arguments and observations under the existing
retention/redaction policy. Diagnostic export removes them and only preserves
known built-in tool names; custom MCP aliases are omitted because they include
user configuration. Credentials are never intentional prompt/history content,
and literal credential echoes are rejected in discovery or redacted in results.
MCP actions execute with the remote server's authority, outside the host's
built-in file-directory grant. Users should set server risk accordingly.
