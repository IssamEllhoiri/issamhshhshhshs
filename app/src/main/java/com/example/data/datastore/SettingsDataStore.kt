package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pointerx_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val CURSOR_SIZE = intPreferencesKey("cursor_size")
        val CURSOR_SENSITIVITY = floatPreferencesKey("cursor_sensitivity")
        val CURSOR_ACCEL = booleanPreferencesKey("cursor_accel")
        val CURSOR_COLOR = stringPreferencesKey("cursor_color")
        val CURSOR_CLICK_VFX = booleanPreferencesKey("cursor_click_vfx")
        val CURSOR_CLICK_VFX_COLOR = stringPreferencesKey("cursor_click_vfx_color")
        val DEFAULT_CURSOR_SHAPE = stringPreferencesKey("default_cursor_shape")
        val HOVER_CURSOR_SHAPE = stringPreferencesKey("hover_cursor_shape")
        val TEXT_CURSOR_SHAPE = stringPreferencesKey("text_cursor_shape")
        val DEFAULT_CURSOR_IMAGE = stringPreferencesKey("default_cursor_image")
        val HOVER_CURSOR_IMAGE = stringPreferencesKey("hover_cursor_image")
        val TEXT_CURSOR_IMAGE = stringPreferencesKey("text_cursor_image")
        
        val CURSOR_HIGHLIGHT_ENABLED = booleanPreferencesKey("cursor_highlight_enabled")
        val CURSOR_HIGHLIGHT_THICKNESS = intPreferencesKey("cursor_highlight_thickness")
        val CURSOR_HIGHLIGHT_COLOR = stringPreferencesKey("cursor_highlight_color")
        
        val PANEL_ALPHA = floatPreferencesKey("panel_alpha")
        val PANEL_SIZE = stringPreferencesKey("panel_size")
        val PANEL_SIDE = stringPreferencesKey("panel_side")
        val PANEL_Y_POSITION = intPreferencesKey("panel_y_position")
        val TOUCHPAD_SIZE = stringPreferencesKey("touchpad_size")
        val PANEL_COLOR = stringPreferencesKey("panel_color")
        
        val LONG_CLICK_MS = intPreferencesKey("long_click_ms")
        val DOUBLE_CLICK_MS = intPreferencesKey("double_click_ms")
        val VIBRATION = booleanPreferencesKey("vibration")
        val VIBRATION_INTENSITY = intPreferencesKey("vibration_intensity")
        
        val SCROLL_SPEED = floatPreferencesKey("scroll_speed")
        val SCROLL_REVERSED = booleanPreferencesKey("scroll_reversed")
        
        val AUTO_START = booleanPreferencesKey("auto_start")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val LANGUAGE = stringPreferencesKey("language")
        val FIRST_RUN = booleanPreferencesKey("first_run")
        
        // Stats
        val SESSION_TIME = longPreferencesKey("session_time")
        val CLICKS_TODAY = intPreferencesKey("clicks_today")
        val DRAG_COUNT = intPreferencesKey("drag_count")
        val POINTER_DISTANCE = floatPreferencesKey("pointer_distance")
    }

    val cursorSizeFlow: Flow<Int> = context.dataStore.data.map { it[CURSOR_SIZE] ?: 24 }
    val cursorSensitivityFlow: Flow<Float> = context.dataStore.data.map { it[CURSOR_SENSITIVITY] ?: 2.0f }
    val cursorAccelFlow: Flow<Boolean> = context.dataStore.data.map { it[CURSOR_ACCEL] ?: true }
    val cursorColorFlow: Flow<String> = context.dataStore.data.map { it[CURSOR_COLOR] ?: "WHITE" }
    val cursorClickVfxFlow: Flow<Boolean> = context.dataStore.data.map { it[CURSOR_CLICK_VFX] ?: true }
    val cursorClickVfxColorFlow: Flow<String> = context.dataStore.data.map { it[CURSOR_CLICK_VFX_COLOR] ?: "PURPLE" }
    val defaultCursorShapeFlow: Flow<String> = context.dataStore.data.map { it[DEFAULT_CURSOR_SHAPE] ?: "DEFAULT" }
    val hoverCursorShapeFlow: Flow<String> = context.dataStore.data.map { it[HOVER_CURSOR_SHAPE] ?: "POINTER" }
    val textCursorShapeFlow: Flow<String> = context.dataStore.data.map { it[TEXT_CURSOR_SHAPE] ?: "TEXT" }
    val defaultCursorImageFlow: Flow<String> = context.dataStore.data.map { it[DEFAULT_CURSOR_IMAGE] ?: "" }
    val hoverCursorImageFlow: Flow<String> = context.dataStore.data.map { it[HOVER_CURSOR_IMAGE] ?: "" }
    val textCursorImageFlow: Flow<String> = context.dataStore.data.map { it[TEXT_CURSOR_IMAGE] ?: "" }
    
    val cursorHighlightEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[CURSOR_HIGHLIGHT_ENABLED] ?: false }
    val cursorHighlightThicknessFlow: Flow<Int> = context.dataStore.data.map { it[CURSOR_HIGHLIGHT_THICKNESS] ?: 4 }
    val cursorHighlightColorFlow: Flow<String> = context.dataStore.data.map { it[CURSOR_HIGHLIGHT_COLOR] ?: "PURPLE" }

    val panelAlphaFlow: Flow<Float> = context.dataStore.data.map { it[PANEL_ALPHA] ?: 0.9f }
    val panelSizeFlow: Flow<String> = context.dataStore.data.map { it[PANEL_SIZE] ?: "MEDIUM" }
    val panelSideFlow: Flow<String> = context.dataStore.data.map { it[PANEL_SIDE] ?: "RIGHT" }
    val panelYPositionFlow: Flow<Int> = context.dataStore.data.map { it[PANEL_Y_POSITION] ?: -1 } // -1 means center
    val touchpadSizeFlow: Flow<String> = context.dataStore.data.map { it[TOUCHPAD_SIZE] ?: "MEDIUM" }
    val panelColorFlow: Flow<String> = context.dataStore.data.map { it[PANEL_COLOR] ?: "DEFAULT" }

    val longClickMsFlow: Flow<Int> = context.dataStore.data.map { it[LONG_CLICK_MS] ?: 800 }
    val doubleClickMsFlow: Flow<Int> = context.dataStore.data.map { it[DOUBLE_CLICK_MS] ?: 130 }
    val vibrationFlow: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION] ?: true }
    val vibrationIntensityFlow: Flow<Int> = context.dataStore.data.map { it[VIBRATION_INTENSITY] ?: 80 }

    val scrollSpeedFlow: Flow<Float> = context.dataStore.data.map { it[SCROLL_SPEED] ?: 1.0f }
    val scrollReversedFlow: Flow<Boolean> = context.dataStore.data.map { it[SCROLL_REVERSED] ?: false }

    val autoStartFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_START] ?: false }
    val batterySaverFlow: Flow<Boolean> = context.dataStore.data.map { it[BATTERY_SAVER] ?: false }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "ar" }
    val firstRunFlow: Flow<Boolean> = context.dataStore.data.map { it[FIRST_RUN] ?: true }

    // Stats
    val sessionTimeFlow: Flow<Long> = context.dataStore.data.map { it[SESSION_TIME] ?: 0L }
    val clicksTodayFlow: Flow<Int> = context.dataStore.data.map { it[CLICKS_TODAY] ?: 0 }
    val dragCountFlow: Flow<Int> = context.dataStore.data.map { it[DRAG_COUNT] ?: 0 }
    val pointerDistanceFlow: Flow<Float> = context.dataStore.data.map { it[POINTER_DISTANCE] ?: 0.0f }

    // Bulk settings flow
    val allSettingsFlow: Flow<PointerSettings> = context.dataStore.data.map { preferences ->
        PointerSettings(
            cursorSize = preferences[CURSOR_SIZE] ?: 24,
            cursorSensitivity = preferences[CURSOR_SENSITIVITY] ?: 2.0f,
            cursorAccel = preferences[CURSOR_ACCEL] ?: true,
            cursorColor = preferences[CURSOR_COLOR] ?: "WHITE",
            cursorClickVfx = preferences[CURSOR_CLICK_VFX] ?: true,
            cursorClickVfxColor = preferences[CURSOR_CLICK_VFX_COLOR] ?: "PURPLE",
            panelAlpha = preferences[PANEL_ALPHA] ?: 0.9f,
            panelSize = preferences[PANEL_SIZE] ?: "MEDIUM",
            panelSide = preferences[PANEL_SIDE] ?: "RIGHT",
            panelYPosition = preferences[PANEL_Y_POSITION] ?: -1,
            touchpadSize = preferences[TOUCHPAD_SIZE] ?: "MEDIUM",
            longClickMs = preferences[LONG_CLICK_MS] ?: 800,
            doubleClickMs = preferences[DOUBLE_CLICK_MS] ?: 130,
            vibration = preferences[VIBRATION] ?: true,
            vibrationIntensity = preferences[VIBRATION_INTENSITY] ?: 80,
            scrollSpeed = preferences[SCROLL_SPEED] ?: 1.0f,
            scrollReversed = preferences[SCROLL_REVERSED] ?: false,
            autoStart = preferences[AUTO_START] ?: false,
            batterySaver = preferences[BATTERY_SAVER] ?: false,
            language = preferences[LANGUAGE] ?: "ar",
            firstRun = preferences[FIRST_RUN] ?: true
        )
    }

    suspend fun updateCursorSize(size: Int) = context.dataStore.edit { it[CURSOR_SIZE] = size }
    suspend fun updateCursorSensitivity(sensitivity: Float) = context.dataStore.edit { it[CURSOR_SENSITIVITY] = sensitivity }
    suspend fun updateCursorAccel(accel: Boolean) = context.dataStore.edit { it[CURSOR_ACCEL] = accel }
    suspend fun updateCursorColor(color: String) = context.dataStore.edit { it[CURSOR_COLOR] = color }
    suspend fun updateCursorClickVfx(vfx: Boolean) = context.dataStore.edit { it[CURSOR_CLICK_VFX] = vfx }
    suspend fun updateCursorClickVfxColor(color: String) = context.dataStore.edit { it[CURSOR_CLICK_VFX_COLOR] = color }
    suspend fun updateDefaultCursorShape(shape: String) = context.dataStore.edit { it[DEFAULT_CURSOR_SHAPE] = shape }
    suspend fun updateHoverCursorShape(shape: String) = context.dataStore.edit { it[HOVER_CURSOR_SHAPE] = shape }
    suspend fun updateTextCursorShape(shape: String) = context.dataStore.edit { it[TEXT_CURSOR_SHAPE] = shape }
    suspend fun updateDefaultCursorImage(imagePath: String) = context.dataStore.edit { it[DEFAULT_CURSOR_IMAGE] = imagePath }
    suspend fun updateHoverCursorImage(imagePath: String) = context.dataStore.edit { it[HOVER_CURSOR_IMAGE] = imagePath }
    suspend fun updateTextCursorImage(imagePath: String) = context.dataStore.edit { it[TEXT_CURSOR_IMAGE] = imagePath }
    
    suspend fun updateCursorHighlightEnabled(enabled: Boolean) = context.dataStore.edit { it[CURSOR_HIGHLIGHT_ENABLED] = enabled }
    suspend fun updateCursorHighlightThickness(thickness: Int) = context.dataStore.edit { it[CURSOR_HIGHLIGHT_THICKNESS] = thickness }
    suspend fun updateCursorHighlightColor(color: String) = context.dataStore.edit { it[CURSOR_HIGHLIGHT_COLOR] = color }

    suspend fun updatePanelAlpha(alpha: Float) = context.dataStore.edit { it[PANEL_ALPHA] = alpha }
    suspend fun updatePanelSize(size: String) = context.dataStore.edit { it[PANEL_SIZE] = size }
    suspend fun updatePanelSide(side: String) = context.dataStore.edit { it[PANEL_SIDE] = side }
    suspend fun updatePanelYPosition(y: Int) = context.dataStore.edit { it[PANEL_Y_POSITION] = y }
    suspend fun updateTouchpadSize(size: String) = context.dataStore.edit { it[TOUCHPAD_SIZE] = size }
    suspend fun updatePanelColor(color: String) = context.dataStore.edit { it[PANEL_COLOR] = color }

    suspend fun updateLongClickMs(ms: Int) = context.dataStore.edit { it[LONG_CLICK_MS] = ms }
    suspend fun updateDoubleClickMs(ms: Int) = context.dataStore.edit { it[DOUBLE_CLICK_MS] = ms }
    suspend fun updateVibration(vibrate: Boolean) = context.dataStore.edit { it[VIBRATION] = vibrate }
    suspend fun updateVibrationIntensity(intensity: Int) = context.dataStore.edit { it[VIBRATION_INTENSITY] = intensity }

    suspend fun updateScrollSpeed(speed: Float) = context.dataStore.edit { it[SCROLL_SPEED] = speed }
    suspend fun updateScrollReversed(reversed: Boolean) = context.dataStore.edit { it[SCROLL_REVERSED] = reversed }

    suspend fun updateAutoStart(start: Boolean) = context.dataStore.edit { it[AUTO_START] = start }
    suspend fun updateBatterySaver(saver: Boolean) = context.dataStore.edit { it[BATTERY_SAVER] = saver }
    suspend fun updateLanguage(lang: String) = context.dataStore.edit { it[LANGUAGE] = lang }
    suspend fun updateFirstRun(firstRun: Boolean) = context.dataStore.edit { it[FIRST_RUN] = firstRun }

    // Increments
    suspend fun incrementClicksToday() = context.dataStore.edit { prefs ->
        val current = prefs[CLICKS_TODAY] ?: 0
        prefs[CLICKS_TODAY] = current + 1
    }

    suspend fun incrementDragCount() = context.dataStore.edit { prefs ->
        val current = prefs[DRAG_COUNT] ?: 0
        prefs[DRAG_COUNT] = current + 1
    }

    suspend fun addPointerDistance(dist: Float) = context.dataStore.edit { prefs ->
        val current = prefs[POINTER_DISTANCE] ?: 0.0f
        prefs[POINTER_DISTANCE] = current + dist
    }

    suspend fun updateSessionTime(timeMs: Long) = context.dataStore.edit { prefs ->
        val current = prefs[SESSION_TIME] ?: 0L
        prefs[SESSION_TIME] = current + timeMs
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { prefs ->
            prefs.clear()
            prefs[FIRST_RUN] = false
        }
    }
}

data class PointerSettings(
    val cursorSize: Int,
    val cursorSensitivity: Float,
    val cursorAccel: Boolean,
    val cursorColor: String,
    val cursorClickVfx: Boolean,
    val cursorClickVfxColor: String,
    val panelAlpha: Float,
    val panelSize: String,
    val panelSide: String,
    val panelYPosition: Int,
    val touchpadSize: String,
    val longClickMs: Int,
    val doubleClickMs: Int,
    val vibration: Boolean,
    val vibrationIntensity: Int,
    val scrollSpeed: Float,
    val scrollReversed: Boolean,
    val autoStart: Boolean,
    val batterySaver: Boolean,
    val language: String,
    val firstRun: Boolean
)
