plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.daggerHilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.google.service)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.messages"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.messages"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir}/messages.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "SECRET_KEY", "\"messages\"")
            isDebuggable = true
        }
        release {
            buildConfigField("String", "SECRET_KEY", "\"messages\"")
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.protolite.well.known.types)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.emoji)
    implementation(libs.androidx.emoji.views)
    implementation(libs.androidx.emoji.views.helper)
    implementation(libs.androidx.emoji.picker)

    // kotlin serialization
    implementation(libs.kotlinx.serialization.json)

    //custom chrome tab
    implementation(libs.androidx.browser)

    //dagger hilt
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    //room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    //ssp-sdp
    implementation(libs.ssp)
    implementation(libs.sdp)

    //lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    ksp(libs.lifecycle.compiler)
    implementation(libs.lifecycle.process)

    //gson
    api(libs.google.gson)

    //glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    //dots indicator
    implementation(libs.dotsIndicator)
    //lottie
    implementation(libs.lottie)

    //phone number utils
    implementation(libs.phone.number.utils)

    //vcard
    implementation(libs.ez.vcard)

    //eventbus
    implementation(libs.eventbus)

    // google ml kit translate
    implementation(libs.mlkit.translate)

    //android sms mms
    implementation(libs.android.smsmms)

    //location
    implementation(libs.play.service.location)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
}