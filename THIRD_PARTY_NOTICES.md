# Third-party notices

This file records third-party components shipped with or consumed by the Three Stove Agent plugin. The
plugin itself is licensed under the Mozilla Public License 2.0; the components below retain their
own licenses. Runtime dependencies are added to this list in the same commit that introduces them.

## AutoJs6 plugin APIs

- Components: `common-plugin-api.aar`, `host-capability-api.aar`, `three-stove-agent-api.aar`
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`, `plugin-api/host-capability-api`, `plugin-api/three-stove-agent-api`), host build 5298 (6.8.0), commit `86d9bfa26b`, built together as release artifacts on 2026-09-27
- common-plugin-api SHA-256: `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15`
- host-capability-api SHA-256: `23024fd981b7846936ef59f595f4da2208adf38d7c2d35594c20c7dfc1a86bd1`
- three-stove-agent-api SHA-256: `02958a7c414c80a7cc839dc8da01b04b367d9b2f3694403f57e93b0e813f0762`
- All hashes are pinned in `locks/host-api-aars.lock` and checked during Gradle configuration
- License: Mozilla Public License 2.0

## Gson

- Component: `com.google.code.gson:gson:2.13.2`, used through tree/stream APIs without reflection
- Source: <https://github.com/google/gson/releases/tag/gson-parent-2.13.2>
- License: Apache License 2.0
- Maven JAR SHA-256: `dd0ce1b55a3ed2080cb70f9c655850cda86c206862310009dcb5e5c95265a5e0`
- Transitive runtime component: `com.google.errorprone:error_prone_annotations:2.41.0`, Apache License 2.0, source <https://github.com/google/error-prone>
- Annotations JAR SHA-256: `a56e782b5b50811ac204073a355a21d915a2107fce13ec711331ad036f660fcc`

## AndroidX AppCompat and Material Components for Android

- Components: `androidx.appcompat:appcompat:1.7.1` and `com.google.android.material:material:1.13.0`, used for the Material 3 standalone interface (themes, buttons, switches, text fields, chips, dialogs, bottom sheets, snackbars and progress indicators)
- Sources: <https://developer.android.com/jetpack/androidx/releases/appcompat#1.7.1> and <https://github.com/material-components/material-components-android/releases/tag/1.13.0>
- License: Apache License 2.0
- appcompat AAR SHA-256: `2ad334a323b28046e89b738c77d184cb3dcca32a551ab048851b2fda23a3ba26`
- appcompat-resources AAR SHA-256: `8e2db31224ca53b108c784da2b361959062716d416b210cfef3d5a3828306df0`
- material AAR SHA-256: `6d5e1cbb67c05bcdcbbf84005787dafd354a155f59a4d3800fd45fa7eb3689f5`
- Transitive runtime components (all Apache License 2.0), as resolved for the release APK:
  - AndroidX: `activity:1.8.0`, `annotation:1.8.1`, `annotation-experimental:1.4.1`, `appcompat-resources:1.7.1`, `arch.core:core-common:2.2.0`, `arch.core:core-runtime:2.2.0`, `cardview:1.0.0`, `collection:1.4.2`, `concurrent:concurrent-futures:1.1.0`, `constraintlayout:2.1.0`, `constraintlayout-core:1.0.0`, `coordinatorlayout:1.1.0`, `core:1.13.0`, `core-ktx:1.13.0`, `cursoradapter:1.0.0`, `customview:1.1.0`, `drawerlayout:1.1.1`, `dynamicanimation:1.1.0`, `emoji2:1.3.0`, `emoji2-views-helper:1.3.0`, `fragment:1.5.4`, `graphics:graphics-shapes:1.0.1`, `interpolator:1.0.0`, `lifecycle-common/livedata/livedata-core/process/runtime/viewmodel/viewmodel-savedstate:2.6.2`, `loader:1.0.0`, `profileinstaller:1.3.1`, `recyclerview:1.2.1`, `resourceinspection-annotation:1.0.1`, `savedstate:1.2.1`, `startup-runtime:1.1.1`, `tracing:1.0.0`, `transition:1.5.0`, `vectordrawable:1.1.0`, `vectordrawable-animated:1.1.0`, `versionedparcelable:1.1.1`, `viewpager:1.0.0`, `viewpager2:1.0.0` (source <https://android.googlesource.com/platform/frameworks/support>); the `annotation-jvm`, `collection-jvm` and `graphics-shapes-android` platform artifacts resolve at the same versions
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core`, `kotlinx-coroutines-core-jvm`, `kotlinx-coroutines-android` and `kotlinx-coroutines-bom`, all 1.6.4 (source <https://github.com/Kotlin/kotlinx.coroutines>)
  - `com.google.guava:listenablefuture:1.0` (source <https://github.com/google/guava>), `org.jetbrains:annotations:13.0` (source <https://github.com/JetBrains/java-annotations>), `org.jspecify:jspecify:1.0.0` (source <https://github.com/jspecify/jspecify>)
- Vector icons in `app/src/main/res/drawable/ic_*.xml` follow Google Material Symbols path data, Apache License 2.0, source <https://github.com/google/material-design-icons>

## Kotlin standard library

- Component: `org.jetbrains.kotlin:kotlin-stdlib:2.3.20` (provided through the Android Gradle Plugin built-in Kotlin support), with `kotlin-stdlib-common:2.3.20`, `kotlin-stdlib-jdk7:1.8.22`, `kotlin-stdlib-jdk8:1.8.22` and `kotlin-bom:1.8.22` resolved into the release APK
- Source: <https://github.com/JetBrains/kotlin>
- License: Apache License 2.0

## Test-only dependencies

These libraries are used by the JVM and instrumentation test source sets only and are not shipped
in the APK.

- JUnit 4 (`junit:junit:4.13.2`): Eclipse Public License 1.0
- AndroidX Test (`androidx.test:runner:1.7.0`, `androidx.test:rules:1.7.0`, `androidx.test.ext:junit:1.3.0`): Apache License 2.0
