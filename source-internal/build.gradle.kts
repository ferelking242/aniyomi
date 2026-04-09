plugins {
    id("mihon.library")
    kotlin("android")
}

android {
    namespace = "eu.kanade.tachiyomi.source.internal"
}

dependencies {
    implementation(kotlinx.coroutines.core)
    api(libs.logcat)
}
