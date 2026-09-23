# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, shared plugin constants)
- `host-capability-api.aar` (host module `plugin-api/host-capability-api`, shared capability broker contract V1)
- `ai-agent-api.aar` (host module `plugin-api/ai-agent-api`, AI Agent control plane and model broker contract V1)

Current provenance: all three release AARs were assembled together from AutoJs6 6.8.0 / 5288,
host commit `0a472f7fee`, on 2026-09-23. License and individual hashes are in
`../THIRD_PARTY_NOTICES.md` and `../locks/host-api-aars.lock`.

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration.

All artifacts are release builds of the host modules and depend on `common-plugin-api.aar`
(the `PluginInfo` parcelable), so they must come from the same host contract line. When the
host contract changes, restage every artifact and extend the lock file in the same commit.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
