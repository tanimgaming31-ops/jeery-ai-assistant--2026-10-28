plugins {
    id("com.android.application")
}
android {
    namespace = "world.tanim.jerry"
    compileSdk = 34
    defaultConfig {
        applicationId = "ai.jeery.assistant.clean"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-independent"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// WebSocket transport for Gemini's Live API. The client never embeds an API key.
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
