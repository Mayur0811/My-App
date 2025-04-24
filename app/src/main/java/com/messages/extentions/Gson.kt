package com.messages.extentions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

var gson = Gson()

inline fun <reified T : Any> T.toJson(): String = gson.toJson(this, T::class.java)

inline fun <reified T : Any> String.fromJson(): T =
    gson.fromJson(this, object : TypeToken<T>() {}.type)