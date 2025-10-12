package com.example.bits_helper.ui.theme

import android.content.Context
import android.content.SharedPreferences

class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_THEME = "theme_type"
    }
    
    fun getThemeType(): ThemeType {
        val themeName = prefs.getString(KEY_THEME, ThemeType.SYSTEM.name)
        return try {
            ThemeType.valueOf(themeName ?: ThemeType.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeType.SYSTEM
        }
    }
    
    fun setThemeType(themeType: ThemeType) {
        prefs.edit().putString(KEY_THEME, themeType.name).apply()
    }
    
    fun shouldUseDarkTheme(isSystemInDarkTheme: Boolean): Boolean {
        return when (getThemeType()) {
            ThemeType.LIGHT -> false
            ThemeType.DARK -> true
            ThemeType.SYSTEM -> isSystemInDarkTheme
        }
    }
}
