import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

val versionCodeProp = versionProps.getProperty("VERSION_CODE", "1").toIntOrNull()
val versionNameProp: String = versionProps.getProperty("VERSION_NAME", "1.1.0")

fun getGitHash() = providers
    .exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }
    .standardOutput
    .asText
    .map {
        it.trim().ifBlank { "unknown" }
    }
    .getOrElse("unknown")


androidComponents {
    onVariants { variant ->
        val hash = getGitHash()
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                output.outputFileName.set("jm-mobile_v${versionNameProp}_${hash}.apk")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// 发布签名配置：优先读 secrets.properties（本地，不入库），其次读同名环境变量（CI）。
// JM_RELEASE_KEY_PASSWORD 缺省时回退到 store 口令。
val secretsProps = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun secret(name: String): String? =
    (secretsProps.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }

val releaseStoreFile: java.io.File? = secret("JM_RELEASE_STORE_FILE")?.let { rootProject.file(it) }
val releaseStorePassword: String? = secret("JM_RELEASE_STORE_PASSWORD")
val releaseKeyAlias: String? = secret("JM_RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? = secret("JM_RELEASE_KEY_PASSWORD") ?: releaseStorePassword

val hasReleaseSigning: Boolean =
    releaseStoreFile != null && releaseStoreFile.exists() &&
        releaseStorePassword != null && releaseKeyAlias != null

// CI 上设置 JM_REQUIRE_RELEASE_SIGNING=true：缺少签名配置直接失败，避免产出 debug 签名的“发布包”。
if (secret("JM_REQUIRE_RELEASE_SIGNING")?.toBoolean() == true && !hasReleaseSigning) {
    throw GradleException(
        "缺少发布签名配置。请提供 JM_RELEASE_STORE_FILE / JM_RELEASE_STORE_PASSWORD / " +
            "JM_RELEASE_KEY_ALIAS（可选 JM_RELEASE_KEY_PASSWORD），" +
            "来源为 secrets.properties 或同名环境变量。当前 keystore 路径: ${releaseStoreFile ?: "未设置"}"
    )
}

android {
    namespace = "com.par9uet.jm"
    compileSdk = 36

    defaultConfig {
        applicationId = "jmcomicoi.net"
        // Android 6.0 Marshmallow is API 23.
        minSdk = 23
        targetSdk = 35
        versionCode = versionCodeProp
        versionName = versionNameProp

        // testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "JM: 未配置发布签名（JM_RELEASE_STORE_FILE / JM_RELEASE_STORE_PASSWORD / " +
                        "JM_RELEASE_KEY_ALIAS），release 变体回退到 debug 签名，产物不可用于分发。"
                )
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

composeCompiler {}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.material.icons.extended)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.paging3.runtime)
    implementation(libs.paging3.compose)
    implementation(libs.kizitonwose.calendar)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.jmcomic.api)
    implementation(libs.jmcomic.core) {
        exclude(group = "org.sejda.imageio", module = "webp-imageio")
    }
    implementation(libs.jmcomic.android.support)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
