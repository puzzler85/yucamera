package com.whyj03.yucamera

import android.content.Context
import androidx.core.content.edit

data class NasConfig(
    val host: String = "",
    val port: Int = 445,
    val username: String = "",
    val password: String = "",
    val shareName: String = "",
    val remotePath: String = "/"
) {
    val isConfigured get() = host.isNotBlank() && shareName.isNotBlank()
}

object NasConfigPrefs {
    private const val PREFS_NAME = "nas_config"

    fun save(context: Context, config: NasConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString("host", config.host)
            putInt("port", config.port)
            putString("username", config.username)
            putString("password", config.password)
            putString("shareName", config.shareName)
            putString("remotePath", config.remotePath)
        }
    }

    fun load(context: Context): NasConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return NasConfig(
            host = prefs.getString("host", "") ?: "",
            port = prefs.getInt("port", 445),
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            shareName = prefs.getString("shareName", "") ?: "",
            remotePath = prefs.getString("remotePath", "/") ?: "/"
        )
    }
}
