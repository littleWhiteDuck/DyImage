package hua.dy.image.utils

import androidx.preference.PreferenceManager
import splitties.init.appCtx
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import androidx.core.content.edit

private val sharedPreference = PreferenceManager.getDefaultSharedPreferences(appCtx)

class SharedPreferenceEntrust<T>(
    private val key: String,
    private val defaultValue: T
): ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getPreferenceValue(key, defaultValue)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setPreferenceValue(key, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getPreferenceValue(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> sharedPreference.getString(key, defaultValue) as T
            is Long -> sharedPreference.getLong(key, defaultValue) as T
            is Set<*> -> sharedPreference.getStringSet(key, defaultValue as Set<String>) as T
            is Boolean -> sharedPreference.getBoolean(key, defaultValue) as T
            is Float -> sharedPreference.getFloat(key, defaultValue) as T
            is Int -> sharedPreference.getInt(key, defaultValue) as T
            else -> throw IllegalArgumentException("Type Error, cannot get value!")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setPreferenceValue(key: String, value: T) {
        sharedPreference.edit {
            when (value) {
                is String -> putString(key, value)
                is Long -> putLong(key, value)
                is Set<*> -> putStringSet(key, value as Set<String>)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Int -> putInt(key, value)
                else -> throw IllegalArgumentException("Type Error, cannot be saved!")
            }
        }
    }

}