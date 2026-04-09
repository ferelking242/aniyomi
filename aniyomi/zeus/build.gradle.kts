plugins {
    id("mihon.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "eu.kanade.tachiyomi.zeus"
}

dependencies {
    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.serialization.json)
    api(libs.logcat)
}
