import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// La clave de firma nunca está en el repositorio.
// En GitHub Actions llega por secretos (variables de entorno).
// Para compilar en tu compu: copiá keystore.properties.example a keystore.properties (está en .gitignore).
val firmaLocal = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun firma(clave: String, env: String): String? = System.getenv(env)?.takeIf { it.isNotBlank() } ?: firmaLocal.getProperty(clave)

val keystorePath = firma("storeFile", "TRANQUI_KEYSTORE_PATH")
val keystorePassword = firma("storePassword", "TRANQUI_KEYSTORE_PASSWORD")
val keyAliasFirma = firma("keyAlias", "TRANQUI_KEY_ALIAS")
val hayFirma = keystorePath != null && keystorePassword != null && keyAliasFirma != null && file(keystorePath).exists()

android {
    namespace = "com.tranqui.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tranqui.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.5.1"
    }

    // Firma fija: así cada versión nueva se instala encima de la anterior.
    signingConfigs {
        if (hayFirma) {
            create("tranqui") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = keyAliasFirma
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hayFirma) signingConfig = signingConfigs.getByName("tranqui")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.webkit:webkit:1.11.0")
    // Reproducción persistente con controles del sistema y pantalla de bloqueo.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
}
