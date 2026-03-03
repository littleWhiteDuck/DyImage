import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinKsp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.lsplugin.jgit)
}

val configFile = rootProject.file("sign.properties")
val prop = Properties()
prop.load(FileInputStream(configFile))

val repo = jgit.repo()
val verCode = (repo?.commitCount("refs/remotes/origin/main") ?: 1)
val verName = repo?.latestTag?.removePrefix("v") ?: "0.1"
val enableAbiSplits = providers
    .gradleProperty("enableAbiSplits")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(true)
    .get()

android {
    namespace = "hua.dy.image"
    compileSdk = 36
    ndkVersion = "29.0.14033849"

    defaultConfig {
        applicationId = "hua.dy.image2"
        minSdk = 24
        targetSdk = 36
        versionCode = verCode
        versionName = verName

        androidResources.localeFilters += setOf("zh")

        //noinspection WrongGradleMethod
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        signingConfigs {
            create("keyStore") {
                keyAlias = prop.getProperty("alias")
                keyPassword = prop.getProperty("keyPassword")
                storeFile = File(prop.getProperty("file"))
                storePassword = prop.getProperty("password")
                enableV3Signing = true
            }
        }

        /*ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }*/
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")

        }
    }

    buildTypes {
        val signConfig = signingConfigs.getByName("keyStore")

        release {
            signingConfig = signConfig
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin.compilerOptions.jvmTarget = JvmTarget.JVM_17

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += setOf(
                "okhttp3/**",
                "kotlin/**",
                "org/**",
                "**/*.properties",
                "**.bin",
                "**.json",
                "**VERSION",
                "META-INF/*.version",
                "**/LICENSE.txt",
            )
        }
    }
    splits {
        abi {
            isEnable = enableAbiSplits
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    androidComponents.onVariants { v ->
        val variant = v as com.android.build.api.variant.impl.ApplicationVariantImpl
        variant.outputs.forEach { output ->
            val abiFilter =
                output.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }?.identifier
                    ?: "universal"
            output.outputFileName.set("EImage-$verName($verCode)_${abiFilter}.apk")
        }
    }

}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.document.file.util)
    implementation(libs.splitties.appctx)
    implementation(libs.coil)
    implementation(libs.coil.gif)

    // room
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.paging)

    implementation(libs.navigation.compose)
    implementation(libs.compose.paging)
    implementation(libs.paging.runtime)

    implementation(libs.shared.preference)
    implementation(libs.androidx.datastore.preferences)
//    implementation(libs.system.ui.controll)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(files("libs/ffmpeg-kit-min.aar"))
    implementation(libs.smart.exception.java)


}
