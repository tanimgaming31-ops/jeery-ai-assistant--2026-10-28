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
