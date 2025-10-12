package com.example.bits_helper

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_SCAN_RESULT_DELAY = "scan_result_delay"
        private const val DEFAULT_SCAN_RESULT_DELAY = 3000L // 3 секунды по умолчанию
        private const val KEY_SYNC_BUTTONS_DISPLAY = "sync_buttons_display"
        private const val DEFAULT_SYNC_BUTTONS_DISPLAY = "both" // "upload", "download", "both"
        private const val KEY_FILTER_FONT_SIZE = "filter_font_size"
        private const val DEFAULT_FILTER_FONT_SIZE = 12f // размер шрифта по умолчанию
        private const val KEY_FILTER_ICON_SIZE = "filter_icon_size"
        private const val DEFAULT_FILTER_ICON_SIZE = 8f // размер иконки по умолчанию
        private const val KEY_COMPACT_MODE_THRESHOLD = "compact_mode_threshold"
        private const val DEFAULT_COMPACT_MODE_THRESHOLD = 300 // порог для компактного режима
    }
    
    fun getScanResultDelay(): Long {
        return prefs.getLong(KEY_SCAN_RESULT_DELAY, DEFAULT_SCAN_RESULT_DELAY)
    }
    
    fun setScanResultDelay(delayMs: Long) {
        prefs.edit().putLong(KEY_SCAN_RESULT_DELAY, delayMs).apply()
    }
    
    fun getScanResultDelaySeconds(): Int {
        return (getScanResultDelay() / 1000).toInt()
    }
    
    fun setScanResultDelaySeconds(seconds: Int) {
        setScanResultDelay((seconds * 1000).toLong())
    }
    
    fun getSyncButtonsDisplay(): String {
        return prefs.getString(KEY_SYNC_BUTTONS_DISPLAY, DEFAULT_SYNC_BUTTONS_DISPLAY) ?: DEFAULT_SYNC_BUTTONS_DISPLAY
    }
    
    fun setSyncButtonsDisplay(display: String) {
        prefs.edit().putString(KEY_SYNC_BUTTONS_DISPLAY, display).apply()
    }
    
    fun showUploadButton(): Boolean {
        val display = getSyncButtonsDisplay()
        return display == "upload" || display == "both"
    }
    
    fun showDownloadButton(): Boolean {
        val display = getSyncButtonsDisplay()
        return display == "download" || display == "both"
    }
    
    fun getFilterFontSize(): Float {
        return prefs.getFloat(KEY_FILTER_FONT_SIZE, DEFAULT_FILTER_FONT_SIZE)
    }
    
    fun setFilterFontSize(size: Float) {
        prefs.edit().putFloat(KEY_FILTER_FONT_SIZE, size).apply()
    }
    
    fun getFilterIconSize(): Float {
        return prefs.getFloat(KEY_FILTER_ICON_SIZE, DEFAULT_FILTER_ICON_SIZE)
    }
    
    fun setFilterIconSize(size: Float) {
        prefs.edit().putFloat(KEY_FILTER_ICON_SIZE, size).apply()
    }
    
    fun getCompactModeThreshold(): Int {
        return prefs.getInt(KEY_COMPACT_MODE_THRESHOLD, DEFAULT_COMPACT_MODE_THRESHOLD)
    }
    
    fun setCompactModeThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_COMPACT_MODE_THRESHOLD, threshold).apply()
    }
}
