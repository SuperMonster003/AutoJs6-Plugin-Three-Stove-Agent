# P10 MCP tool extension

Date: 2026-09-26 (UTC+8). AI Agent 1.2.0 development candidate, build 88.
This implements the three original P10 items without adding, splitting or
discarding roadmap stages. P9.1 JSON Wi-Fi and P9.2 real online vision acceptance
remain separately pending. This candidate is not a published release.

## Scope and boundaries

- `McpToolSource` supports explicitly configured Streamable HTTP servers,
  including the local MCP Server at `http://127.0.0.1:9637/mcp`. External hosts
  require HTTPS. Supported protocol revisions are 2025-11-25, 2025-06-18 and
  2025-03-26. JSON and POST SSE responses are implemented; legacy HTTP+SSE,
  stdio, OAuth, the 2026 protocol family and a persistent GET stream are not.
- Discovered tools require explicit selection. Profiles and the `mcp` group
  start disabled; the default per-server risk is SENSITIVE. The same immutable
  per-run catalog supplies prompts, native tool definitions, validation and
  dispatch. Global settings, presets and task options continue to intersect.
- User configuration uses Android Keystore AES-GCM, an atomic no-backup file,
  revision checks and a private same-UID endpoint. The UI never receives a
  stored bearer token. Changing an endpoint always clears selected tools. Its
  old token is removed unless a new token is explicitly supplied.
- Existing confirmation, cancellation, budgets and step history apply to MCP.
  Server annotations cannot lower local risk. A tool error becomes TOOL_FAILED
  and an unsuccessful observation, including native `isError`. Network failure
  does not automatically replay a tool. Catalog changes invalidate the frozen
  selection. Remote code and side effects cannot be frozen or rolled back by
  this client.
- External schemas retain standard open-object semantics. Unsupported
  constraints are visibly rejected instead of removed. Empty/annotation-only
  child schemas and type unions are supported within bounded JSON values.
  Tools requiring asynchronous MCP Tasks are rejected. Result images/resources
  are omitted, not automatically fetched or sent to a model; optional server
  outputSchema is not validated.
- The local MCP Server appendix E still reserves a future Android MCP Client;
  no implementation or host capability proxy was found. The existing PC MCP
  Bridge is a different component. The source boundary is replaceable and will
  prefer that capability proxy if it becomes available.

Full limits, credential handling, negotiation, pairing, catalog invalidation
and diagnostics are in [the protocol document](p10-mcp-tool-protocol.md).

## Related repositories

| Repository | Local commit / version | Scope |
|---|---|---|
| AutoJs6 | 3cdf7de13c + aabd63444d / 6.8.0 build 5297 | Default-off mcp option, TOOL_FAILED, tests, ten-language changelog and protocol evidence |
| TypeScript Declarations | 333ccc0 / 4.23.0 | AgentToolGroup and compile smoke |
| Documentation | 42c1df3 / generation 81 | Public task-option docs and generated pages/search index |
| Ace Editor | 4f0e533 / 1.15.0 build 115 | Generated Agent declarations and packaging |
| Offline Docs | db32a90 / 6.8.5 build 62 | Matching documentation assets and packaging |

All three Agent release SDK AARs came from the same clean isolated host build
at 3cdf7de13c. The Agent AAR SHA-256 is
`3f4e0aa5a5c6d7adede4a998603386a5c338c150a97fc9a75beb95e0028c2626`;
the common and host-capability AAR bytes remain unchanged. Source, license and
hashes are recorded in `libs/README.md`, `THIRD_PARTY_NOTICES.md` and the lock.
No AIDL transaction was added or reordered. Existing host event/result readers
preserve error JSON without an ERROR_CODES whitelist; an older host still
rejects an explicit unknown mcp group at its public JavaScript option boundary.

The declaration generator's local `-Publish -SkipBuild` workflow copied assets
only; it did not publish to npm. The existing Types publishConfig modification
and Ace releases directory were preserved as user work. Host Rhino files were
not changed. No repository was pushed, tagged or externally published.

## Deterministic verification

Final JVM: 578 tests, 577 passed, one existing opt-in performance test skipped,
zero failures/errors. New coverage includes 23 MCP profile/transport/source
tests, four encrypted-store tests and 14 core integration tests. Selected-tool
fingerprints, catalog changes, cancellation, action no-replay, schema rejection,
JSON/native errors and default-off network behavior are exercised.

Debug, Android test, R8 release and fake-host APK builds passed. Final lint is
zero errors and six existing warnings. The single-APK signed archive and native
page-alignment guard passed; there are no native libraries. Ten-language
Markdown verification passed for all 36 generated artifacts.

Host JVM: eight task-option tests and seven Agent API tests passed. TypeScript
smoke compiled. Ace LSP declaration generation and Debug assembly passed.
Online docs normalized/generated all 143 modules; the search index contains
6226 entries and passed syntax validation. Offline Debug/Release verification
matched 199 files / 11605704 bytes with contents SHA-256
`5d60c61cce1d134871562773370b2138dc9471676557b7a49288673d166c6da6`.

Android uses two private `AI_Agent_Conformance_P92_*` AVDs: API 24 / x86 and
API 37.1 / x86_64 / 16 KiB. The five Agent/fake-host APKs were re-signed with
the standard Android test key solely for these isolated installations. Every
non-signature ZIP entry matches the corresponding officially signed build.
This is not a claim that an officially signed Agent was installed on a physical
device this turn. The internal Android suite targets Debug; R8 is tested through
the independent external fake-host APK.

The 12 new MCP Android cases pass on both AVDs: five real `:agent` HostLink /
HTTP cases, two encrypted-storage/private-endpoint cases and five settings UI
cases. They cover JSON sensitive confirmation despite server read-only hints,
native isError, cancellation before dispatch with session cleanup, disabled
global/preset groups making zero connections, Android Keystore reopen,
write-only tokens, explicit discovery selection, unsupported schemas, lifecycle
cancellation/recreation and narrow Arabic RTL/dark layouts. Controlled model
responses make no online requests.

External Debug and R8 suites each pass 10 cases on API 24 (two vision-only cases
skipped) and 11 on API 37.1 (one old-Android-only case skipped). In both variants
and on both platforms the new trusted-host attempt to bind private MCP settings
is rejected. The existing cross-UID model/capability, grant, native continuation
and process-death checks remain active.

Actual local MCP Server interoperability separately passes on both AVDs. The
installed official Server APK is 1.0.2 / build 67, SHA-256
`2dd26f232d4d2d855e0433a94d250d285d734d31f582034b97a637967be74ca7`.
Its runtime source corresponds to 1061bbc; the audited repository HEAD 88c2573
adds documentation changes. The test starts a fresh disposable developer-mode
server, passes its temporary token only through private stdin/file handling,
then removes the input file. Each platform reports 33 discovered tools, 31
supported definitions and the expected MCP_PAIRING_REQUIRED outcome when trying
only the non-mutating device_info tool. Two unsupported definitions remain
unavailable; discovery does not silently strip their constraints. The real SDK
transport test does not approve pairing, attach a real host capability broker
or claim successful device execution. HTTP/tool success and isError integration
are separately established by controlled HTTP fixtures.

The superseded candidate used for the complete Android suites was 1.2.0 /
build 88, 767524 bytes, CRC32
`11cd2b40`, SHA-256
`0eb40d4705fa7815b527935ea7869f429050900e91fbbc5fb159d49a1f9168a6`.
Its signer SHA-256 is
`31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
After fixing the initial fixtures, the complete internal Android suites passed
102 cases on API 24 and 108 on API 37.1, with three explicit opt-in skips each
(the two existing screenshot captures and the separate real-server case).
They took 295.723 / 316.420 seconds and used the above candidate before the final
MCP endpoint resource-cleanup refinement. That refinement is verified separately
on the final candidate below; these full-suite receipts are not relabeled as
having run after a later code change.

The real host at source 3cdf7de13c also passed all six independent fake-Agent
peer cases on each AVD, with no skips. Host Debug/instrumentation and fake-Agent
assembly took 26 seconds and used matching standard test signatures. The
reciprocal fake host was restored afterwards for remaining Agent tests. The
host protocol and complete host receipt are committed as aabd63444d.

The final resource-cleanup change closes the private service's initialized MCP
endpoint, cancels discovery and unlinks death listeners, then drains/shuts down
its owned executor. An idle core-thread timeout is a second bound. Accepted
repository writes retain their maintenance guard and complete independently.
Final build/JVM/lint/archive verification passed again (2 min 35 s, 577 JVM
passes / one existing skip, zero lint errors / six existing warnings). The 14
MCP Android cases pass on both AVDs (9.126 / 26.359 s), including two new real
HTTP/queued-write lifecycle cases. Debug and R8 external Binder suites pass
again with the same platform-specific 10/11 passes and 2/1 skips. The real local
Server case also passes on the final Debug candidate on both platforms, with
the same 33 discovered / 31 supported tools and pairing-required observation.
The entire internal Android suite was not repeated after this localized cleanup;
the earlier full-suite and final scoped receipts are kept separately.

The final official R8 candidate is 1.2.0 / build 88, 767852 bytes, CRC32
`47afd9ea`, SHA-256
`f1395767880420bb36a96bc083ffb82973bb60086e3aa02778e50b1b689ee310`.
Its archive bytes and official certificate match the build. Final isolated
Android installations use test-signed APKs with identical non-signature ZIP
entries; the official package was not installed on a physical device this turn.

## Failure records and limits

- The first Agent JVM run had two failures: unconditional MCP instructions
  changed a built-in prompt snapshot and exceeded an existing small context
  fixture. MCP instructions now apply only when external tools are admitted;
  existing prompt and budget assertions were retained. A subsequent review
  found external selector parameters incorrectly entering the built-in compact
  alias pool. The final fix keeps external schema semantics and adds a union /
  required-extra regression for compact and native prompts.
- An intermediate lint report contained 14 new warnings. API 26 IME flags are
  now guarded, the server label uses a format resource, and only the identifier
  hint suppresses dash typography according to the repository ASCII rule.
  The final six warnings match the prior baseline.
- A stale host checkout metadata condition initially prevented fast-forwarding
  the isolated SDK worktree. The intermediate AAR build was not used. After
  verifying identical file blobs and synchronizing to clean 3cdf7de13c, all
  three SDKs were rebuilt together and only those outputs were staged.
- An initial Ace command referenced a nonexistent verify task. The actual LSP
  generation and Debug build subsequently passed. Offline build count was
  corrected after the docs generator incremented it; final packaging and
  archive-content verification were rerun at build 62.
- Each first full Android run had six failures. Five new HostLink cases omitted
  required token/clearToken fields from their fixture save request and were
  correctly rejected as INVALID_REQUEST. The Arabic UI test incorrectly measured
  a GONE cancel button as an active touch target. Fixtures now include the full
  save request and separately assert hidden/visible cancel states with the same
  48dp minimum; production validation and layouts were not weakened. Both
  platforms subsequently passed all 12 MCP cases before full-suite repetition.
- The first optional API 24 interoperability attempt failed before network use
  because the private orchestration's exec-in did not transfer the input file.
  It now uses shell -T, checks the byte count and passes. That first raw result
  was overwritten by the initial runner's fixed log name; the observed failure
  is preserved in a private note. Subsequent runs use unique output names.
- A later real-Server repeat on each AVD failed the expectation that an unpaired
  call always returns pairing-required. Both Server logs record a pairing timeout
  at 13:22:10; PairingGate implements a 60-second approval window followed by a
  30-second denial cooldown. The failing test did not record its fixed returned
  classification, so timeout/cooldown attribution combines those server logs
  with source inspection, not a captured error-code assertion. A fixed-code-only
  assertion message was added; after cooldown, the same client identity passed
  again without approving pairing, changing credentials or replaying a device
  action. The failures and pairing logs remain separate from the final passes.
- Final read-only review found settings endpoint worker pools could outlive
  repeated service instances while another host binding kept the process alive.
  The final cleanup above fixes that leak and adds direct lifecycle verification;
  accepted configuration writes are deliberately not cancelled.

Private raw build/device logs, test-signing receipts and disposable server
credentials are excluded from Git. Device tests do not replace real-model or
real-account acceptance. This turn makes no real model calls, no physical-device
changes and no shopping, ordering or payment actions. No SIM is needed for
this P10 verification; the previously pending online Wi-Fi comparison will
need independent connectivity only while that use case runs.
At completion, both disposable MCP listeners received their STOP_SERVER action
and both private AVDs were shut down after rechecking their exact names and
emulator flags. Their ignored data and test receipts are retained.
