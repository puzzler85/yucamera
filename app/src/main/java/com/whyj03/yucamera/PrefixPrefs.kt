package com.whyj03.yucamera

import android.content.Context
import androidx.core.content.edit

object PrefixPrefs {
    private const val PREF_NAME = "photo_prefix_prefs"
    private const val KEY_PREFIX = "prefix"
    private const val KEY_COUNTER = "counter"

    fun loadPrefix(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX, "") ?: ""

    fun savePrefix(context: Context, prefix: String) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_PREFIX, prefix) }

    fun loadCounter(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_COUNTER, 1)

    fun saveCounter(context: Context, counter: Int) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_COUNTER, counter) }
}
