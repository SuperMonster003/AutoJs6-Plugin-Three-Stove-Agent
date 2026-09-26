# Third-party notices

This file records third-party components shipped with or consumed by the AI Agent plugin. The
plugin itself is licensed under the Mozilla Public License 2.0; the components below retain their
own licenses. Runtime dependencies are added to this list in the same commit that introduces them.

## AutoJs6 plugin APIs

- Components: `common-plugin-api.aar`, `host-capability-api.aar`, `ai-agent-api.aar`
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`, `plugin-api/host-capability-api`, `plugin-api/ai-agent-api`), host build 5297 (6.8.0), commit `3cdf7de13c`, built together as release artifacts on 2026-09-26
- common-plugin-api SHA-256: `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15`
- host-capability-api SHA-256: `0c9233f43848cc3a9d9a87071f46ecb71935964693a53de017529dfa2de79c28`
- ai-agent-api SHA-256: `3f4e0aa5a5c6d7adede4a998603386a5c338c150a97fc9a75beb95e0028c2626`
- All hashes are pinned in `locks/host-api-aars.lock` and checked during Gradle configuration
- License: Mozilla Public License 2.0

## Gson

- Component: `com.google.code.gson:gson:2.13.2`, used through tree/stream APIs without reflection
- Source: <https://github.com/google/gson/releases/tag/gson-parent-2.13.2>
- License: Apache License 2.0
- Maven JAR SHA-256: `dd0ce1b55a3ed2080cb70f9c655850cda86c206862310009dcb5e5c95265a5e0`
- Transitive runtime component: `com.google.errorprone:error_prone_annotations:2.41.0`, Apache License 2.0, source <https://github.com/google/error-prone>
- Annotations JAR SHA-256: `a56e782b5b50811ac204073a355a21d915a2107fce13ec711331ad036f660fcc`

## Kotlin standard library

- Component: `org.jetbrains.kotlin:kotlin-stdlib` (provided through the Android Gradle Plugin built-in Kotlin support)
- Source: <https://github.com/JetBrains/kotlin>
- License: Apache License 2.0

## Test-only dependencies

These libraries are used by the JVM and instrumentation test source sets only and are not shipped
in the APK.

- JUnit 4 (`junit:junit`): Eclipse Public License 1.0
- AndroidX Test (`androidx.test:runner`, `androidx.test:rules`, `androidx.test.ext:junit`): Apache License 2.0
