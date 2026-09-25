plugins {
    id("org.autojs.build.versions")
    id("org.autojs.build.jvm-convention")
    id("org.autojs.build.signs")
    id("com.android.application")
}

android {
    namespace = "org.autojs.plugin.ai.agent.fakehost"
    compileSdk = versions.sdkVersionCompile
    defaultConfig {
        // Exercise the real installed-host verifier. Install ONLY in a disposable AVD.
        applicationId = "org.autojs.autojs6"
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = 5289
        versionName = "conformance"
        testApplicationId = "org.autojs.plugin.ai.agent.fakehost.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { aidl = true; buildConfig = false }
    signingConfigs {
        if (signs.isValid) create("pluginTest") {
            storeFile = rootProject.file("app").resolve(signs.properties["storeFile"] as String)
            storePassword = signs.properties["storePassword"] as String
            keyAlias = signs.properties["keyAlias"] as String
            keyPassword = signs.properties["keyPassword"] as String
        }
    }
    buildTypes { debug { if (signs.isValid) signingConfig = signingConfigs.getByName("pluginTest") } }
}
androidComponents { beforeVariants(selector().withBuildType("release")) { it.enable = false } }
dependencies {
    // The app module validates these same repository-local AARs against locks/host-api-aars.lock.
    implementation(files("../../libs/common-plugin-api.aar", "../../libs/host-capability-api.aar", "../../libs/ai-agent-api.aar"))
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.ext.junit)
}
