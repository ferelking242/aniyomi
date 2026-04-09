plugins {
    id("mihon.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "eu.kanade.tachiyomi.zeus"
}

dependencies {
    implementation(platform(kotlinx.coroutines.bom))
    implementation(kotlinx.coroutines.core)
    implementation(kotlinx.serialization.json)
    api(libs.logcat)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
