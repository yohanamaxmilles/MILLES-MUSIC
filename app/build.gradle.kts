import com.android.build.api.variant.BuildConfigField
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.millesmusic.kxmpzq"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

val dotEnvFile = rootProject.file(".env")
val dotEnvProperties =
    Properties().apply {
      if (dotEnvFile.exists()) {
        dotEnvFile.inputStream().use { load(it) }
      }
    }

fun normalizeSecretValue(raw: String?): String =
    raw?.trim()?.let { trimmed ->
      if (trimmed.length >= 2 &&
          ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
              (trimmed.startsWith("'") && trimmed.endsWith("'")))
      ) {
        trimmed.substring(1, trimmed.length - 1).trim()
      } else {
        trimmed
      }
    }.orEmpty()

fun resolveEnvFallbackSecret(key: String, placeholder: String): String? {
  val dotEnvValue = normalizeSecretValue(dotEnvProperties.getProperty(key))
  if (dotEnvValue.isNotEmpty() && dotEnvValue != placeholder) {
    return null
  }
  val envValue = normalizeSecretValue(System.getenv(key))
  return envValue.takeIf { it.isNotEmpty() && it != placeholder && !it.equals("null", ignoreCase = true) }
}

androidComponents {
  onVariants { variant ->
    resolveEnvFallbackSecret("JAMENDO_CLIENT_ID", "MY_JAMENDO_CLIENT_ID")?.let { secret ->
      val escaped = secret.replace("\\", "\\\\").replace("\"", "\\\"")
      variant.buildConfigFields?.put(
          "JAMENDO_CLIENT_ID",
          BuildConfigField(
              "String",
              "\"$escaped\"",
              "JAMENDO_CLIENT_ID injected from environment"
          )
      )
    }
    resolveEnvFallbackSecret("YOUTUBE_API_KEY", "MY_YOUTUBE_API_KEY")?.let { secret ->
      val escaped = secret.replace("\\", "\\\\").replace("\"", "\\\"")
      variant.buildConfigFields?.put(
          "YOUTUBE_API_KEY",
          BuildConfigField(
              "String",
              "\"$escaped\"",
              "YOUTUBE_API_KEY injected from environment"
          )
      )
    }
  }
}

val dotEnvPresentAtConfig = dotEnvFile.exists()
val jamDotEnvAtConfig = normalizeSecretValue(dotEnvProperties.getProperty("JAMENDO_CLIENT_ID"))
val ytDotEnvAtConfig = normalizeSecretValue(dotEnvProperties.getProperty("YOUTUBE_API_KEY"))
val jamEnvAtConfig = normalizeSecretValue(System.getenv("JAMENDO_CLIENT_ID"))
val ytEnvAtConfig = normalizeSecretValue(System.getenv("YOUTUBE_API_KEY"))
val jamConfiguredAtConfig =
    (jamDotEnvAtConfig.isNotEmpty() && jamDotEnvAtConfig != "MY_JAMENDO_CLIENT_ID" && !jamDotEnvAtConfig.equals("null", ignoreCase = true)) ||
        (jamEnvAtConfig.isNotEmpty() && jamEnvAtConfig != "MY_JAMENDO_CLIENT_ID" && !jamEnvAtConfig.equals("null", ignoreCase = true))
val ytConfiguredAtConfig =
    (ytDotEnvAtConfig.isNotEmpty() && ytDotEnvAtConfig != "MY_YOUTUBE_API_KEY" && !ytDotEnvAtConfig.equals("null", ignoreCase = true)) ||
        (ytEnvAtConfig.isNotEmpty() && ytEnvAtConfig != "MY_YOUTUBE_API_KEY" && !ytEnvAtConfig.equals("null", ignoreCase = true))

tasks.register("verifySecretsConfig") {
  val hasDotEnv = dotEnvPresentAtConfig
  val jamOk = jamConfiguredAtConfig
  val ytOk = ytConfiguredAtConfig
  doLast {
    println("SECRETS_CHECK dotEnvExists=$hasDotEnv jamendoConfigured=$jamOk youtubeConfigured=$ytOk")
  }
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
