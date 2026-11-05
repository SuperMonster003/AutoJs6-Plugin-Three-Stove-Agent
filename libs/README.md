# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, shared plugin constants, and since 1.4.0 the settings transfer contract v1 with `PluginSettingsTransferProvider`, roadmap P20)
- `host-capability-api.aar` (host module `plugin-api/host-capability-api`, shared capability broker contract V1)
- `three-stove-agent-api.aar` (host module `plugin-api/three-stove-agent-api`, 3-Stove Agent control plane and model broker contract V2)
- `speech-api.aar` (host module `plugin-api/speech-api`, speech plugin contract V1: `ISpeechPlugin`, `ISpeechSession`, the wake and session callbacks and their constants; the resident listener binds 3-Macaw Speech with it, roadmap D69 / D74)

Current provenance: the first three release AARs were assembled together from AutoJs6 6.8.0 / 5298,
host commit `86d9bfa26b`, on 2026-09-27. `speech-api.aar` (roadmap D69, 2026-10-17) is byte-identical to the
copy 3-Macaw Speech stages: `:plugin-api:speech-api:assembleRelease` from the host sources of commit
`1d6280bf8` (6.8.0, build 5451), which introduces the module (contract version 1). It is a later host build
than the other three; it only needs `PluginInfo` from `common-plugin-api`, which is unchanged between the two
builds (the module only gained capability key constants). All four move to the published 6.8.0 build together.
Since 1.4.0 (roadmap P20, 2026-11-05) `common-plugin-api.aar` is version 1.1.1, the release artifact of host
commit `1676e7489d` built on 2026-11-05 (unsigned), which adds the settings transfer contract v1 and, over 1.1.0, the
import purpose (`PURPOSE_IMPORT`, `PURPOSE_UNDO`, `Request.isUndo`, D97). It only adds classes and members: `javap` shows the same signatures and constants for the 11 classes of the `86d9bfa26b` build, except
that `PluginCapabilityKeys` gains `SUPPORTED_ABIS`, so the other three AARs keep working with it. License and individual hashes are in
`../THIRD_PARTY_NOTICES.md` and `../locks/host-api-aars.lock`.

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration.

All artifacts are release builds of the host modules and depend on `common-plugin-api.aar`
(the `PluginInfo` parcelable), so they must come from the same host contract line (6.8.0). When the
host contract changes, restage every artifact and extend the lock file in the same commit.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
