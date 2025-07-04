// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.ksp) apply  false
    alias(libs.plugins.daggerHilt) apply  false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.google.service) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}