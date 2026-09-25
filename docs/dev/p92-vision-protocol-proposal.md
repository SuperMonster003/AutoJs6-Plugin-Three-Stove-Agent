# P9.2 vision protocol decision

Status: **accepted architectural direction, implementation pending**. On
2026-09-25 the maintainer selected "Extend V2: negotiate 2.1 image input" in
response to the original P9.2 decision. No new roadmap stage or public Agent
JS API is introduced by this decision.

## Decision and current boundary

The original P9.2 host item explicitly offers either image content parts plus a
vision capability in the V2 family, or a separate vision family, with the
maintainer choosing. The current host document,
`AutoJs6/docs/dev/ai-provider-protocol-v2.md`, says protocol 2.0 is text-only and
puts other modalities in separate families/plugins. Existing text payload and
MIME validators enforce that boundary. An image cannot simply be slipped into
an existing text payload.

Accepted direction: retain the V2 family and introduce an explicitly negotiated
2.1 image-input extension. Keep 2.0 text requests and their wire encoding intact.
This explicitly revises the existing text-only boundary for negotiated 2.1;
the 2.0 boundary remains intact. Images are inputs to text/tool generation; image generation,
audio and video are outside this P9.2 proposal.

| Choice | Result | Cost and compatibility |
|---|---|---|
| Extend V2 through negotiated 2.1 (recommended) | Text, images, native tool calls and tool results can share the same bounded model conversation and existing profile | Revise the text-only policy; extend codecs, target capabilities, quotas and version negotiation. Old hosts/providers keep 2.0; they never receive images |
| Separate vision family | Retain the existing text-only policy and use an explicit vision service/contract | Add discovery, handshake, session, quota and model-broker adapters for a second family; define how its images coexist with the native tool loop and how profiles are shared. Whether it needs a separate APK is a further packaging consequence, not an automatic requirement imposed by this proposal |

## Proposed 2.1 behavior for review

- Both the Provider and the exact selected target must advertise vision, and the
  host must negotiate the image-input extension. Older components and text-only
  targets remain fully usable for text and existing tools. Unsupported image
  requests fail before upload; no silent image dropping or alternate-model use.
- Append an explicit image content-part variant and image capability/limit
  metadata. Preserve existing AIDL transaction order and old text encoding.
  Keep textual input limits, image byte/pixel limits and descriptor quotas
  separately checked, with an overall bounded request. Exact constants and
  wire fixtures belong to implementation review, not guessed device evidence.
- Agent requests `screen_capture` only when the selected target and host support
  it. Use the original roadmap conversion: longest edge 1280, JPEG quality 70.
  The host captures through its existing screenshot capability; the Agent has
  no independent Provider binding or credentials.
- Send host-owned image references/descriptors through the model broker.
  Validate MIME, decoded dimensions, declared bytes, ownership and deadlines;
  close every descriptor on rejection, cancellation and completion. Do not put
  screenshot bytes, base64, credentials or file paths into ordinary logs.
- Map image input in 3-Stone AI for the configured online protocols. Capability
  declaration must reflect the selected model/profile; LiteRT remains text-only
  unless separately verified to accept images. Do not infer vision merely from
  an online protocol name.
- Account for image tokens conservatively before admission and reconcile actual
  usage. Preserve run/call/time/output budgets and user confirmation rules.
  Images must work with the P9.1 native loop, including continuation ownership;
  defining only an initial screenshot upload would leave screen-driven tasks
  incomplete.
- Validate old/new host and Provider combinations, descriptor cancellation,
  malformed/oversized images, text regressions and real supported-model use.
  The current P9.1 task failures remain recorded; vision implementation cannot
  relabel them as successful text/native comparisons.

The maintainer's answer is recorded here and in the existing roadmap session
log. Detailed wire fields, limits and compatibility tests still require
implementation and review before P9.2 can be checked off.
