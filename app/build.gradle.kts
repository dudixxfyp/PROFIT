plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Chave de assinatura: o build automático (GitHub Actions) cria app/profit.jks a partir
// dos secrets. Sem esse arquivo, o build usa a chave de debug (bom para testar no seu celular).
val releaseKeystore = file("profit.jks")

android {
    namespace = "com.duda.profit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duda.profit"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore.exists()) {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystore.exists()) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
