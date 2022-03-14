package com.termux.gui

import android.annotation.SuppressLint
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException


class Settings private constructor() {
    
    companion object {
        private const val TIMEOUT_KEY = "timeout"
        private const val TIMEOUT_DEFAULT = 3
        private const val BACKGROUND_KEY = "background"
        private const val BACKGROUND_DEFAULT = false
        private const val LOGLEVEL_KEY = "loglevel"
        private const val LOGLEVEL_DEFAULT = 0
        val instance = Settings()
    }
    
    
    
    var timeout: Int = TIMEOUT_DEFAULT
    var background: Boolean = BACKGROUND_DEFAULT
    var loglevel: Int = LOGLEVEL_DEFAULT
    
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
    
    
    fun load(c: Context) {
        val prefs = settingsPreferences(c)
        if (prefs != null) {
            timeout = prefs.getInt(TIMEOUT_KEY, TIMEOUT_DEFAULT)
            background = prefs.getBoolean(BACKGROUND_KEY, BACKGROUND_DEFAULT)
            loglevel = prefs.getInt(LOGLEVEL_KEY, LOGLEVEL_DEFAULT)
        }
    }
    
    
    @SuppressLint("ApplySharedPref")
    fun save(c: Context) {
        val prefs = settingsPreferences(c)
        if (prefs != null) {
            val e = prefs.edit()
            e.putInt(TIMEOUT_KEY, timeout)
            e.putBoolean(BACKGROUND_KEY, background)
            e.putInt(LOGLEVEL_KEY, loglevel)
            e.commit()
        }
    }
    
    
    
    
}