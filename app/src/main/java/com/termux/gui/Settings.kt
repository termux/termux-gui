package com.termux.gui

import android.annotation.SuppressLint
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Settings for the app.
 */
class Settings private constructor() {
    
    companion object {
        private const val TIMEOUT_KEY = "timeout"
        private const val TIMEOUT_DEFAULT = 3
        private const val BACKGROUND_KEY = "background"
        private const val BACKGROUND_DEFAULT = false
        private const val LOGLEVEL_KEY = "loglevel"
        private const val LOGLEVEL_DEFAULT = 0
        private const val JAVASCRIPT_KEY = "javascript"
        private const val JAVASCRIPT_DEFAULT = false
        val instance = Settings()
    }


    /**
     * Timeout for the service in seconds if no clients are connected.
     */
    var timeout: Int = TIMEOUT_DEFAULT

    /**
     * Whether the service should be immediately stopped in the background if no clients are connected.
     */
    var background: Boolean = BACKGROUND_DEFAULT

    /**
     * The log level for the logger.
     */
    var loglevel: Int = LOGLEVEL_DEFAULT

    /**
     * Whether JavaScript should be enabled in WebViews by default.
     */
    var javascript: Boolean = JAVASCRIPT_DEFAULT


    /**
     * Get the encrypted shared preferences for the settings.
     */
    @Suppress("DEPRECATION")
    private fun settingsPreferences(c: Context): EncryptedSharedPreferences? {
        return try {
            val m = MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(c, 
                    "settings_prefs",
                    m,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
                    as? EncryptedSharedPreferences
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Load the settings from the shared preferences.
     */
    fun load(c: Context) {
        val prefs = settingsPreferences(c)
        if (prefs != null) {
            timeout = prefs.getInt(TIMEOUT_KEY, TIMEOUT_DEFAULT)
            background = prefs.getBoolean(BACKGROUND_KEY, BACKGROUND_DEFAULT)
            loglevel = prefs.getInt(LOGLEVEL_KEY, LOGLEVEL_DEFAULT)
            javascript = prefs.getBoolean(JAVASCRIPT_KEY, JAVASCRIPT_DEFAULT)
        }
    }

    /**
     * Save the settings to the shared preferences.
     */
    @SuppressLint("ApplySharedPref")
    fun save(c: Context) {
        val prefs = settingsPreferences(c)
        if (prefs != null) {
            val e = prefs.edit()
            e.putInt(TIMEOUT_KEY, timeout)
            e.putBoolean(BACKGROUND_KEY, background)
            e.putInt(LOGLEVEL_KEY, loglevel)
            e.putBoolean(JAVASCRIPT_KEY, javascript)
            e.commit()
        }
    }
    
    
    
    
}