package com.example.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.CursorShape
import com.example.util.PointerServiceCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val dataStore = SettingsDataStore(context)

    // Master switch flows
    val isOverlayActive: StateFlow<Boolean> = PointerServiceCoordinator.isOverlayActive
    val isAccessibilityActive: StateFlow<Boolean> = PointerServiceCoordinator.isAccessibilityActive

    // Settings flows
    val firstRun: StateFlow<Boolean> = dataStore.firstRunFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cursorSize: StateFlow<Int> = dataStore.cursorSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)
        val cursorSizeText: StateFlow<Int> = dataStore.cursorSizeTextFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)
    val cursorSizeHover: StateFlow<Int> = dataStore.cursorSizeHoverFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)
    val trackTextCursor: StateFlow<Boolean> = dataStore.trackTextCursorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val trackHoverCursor: StateFlow<Boolean> = dataStore.trackHoverCursorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cursorSensitivity: StateFlow<Float> = dataStore.cursorSensitivityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.0f)

    val cursorColor: StateFlow<String> = dataStore.cursorColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "WHITE")

    val cursorClickVfx: StateFlow<Boolean> = dataStore.cursorClickVfxFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cursorClickVfxColor: StateFlow<String> = dataStore.cursorClickVfxColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PURPLE")

    val defaultCursorShape: StateFlow<String> = dataStore.defaultCursorShapeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT")

    val hoverCursorShape: StateFlow<String> = dataStore.hoverCursorShapeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "POINTER")

    val textCursorShape: StateFlow<String> = dataStore.textCursorShapeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "TEXT")

    val defaultCursorImage: StateFlow<String> = dataStore.defaultCursorImageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val hoverCursorImage: StateFlow<String> = dataStore.hoverCursorImageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val textCursorImage: StateFlow<String> = dataStore.textCursorImageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val cursorHighlightEnabled: StateFlow<Boolean> = dataStore.cursorHighlightEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val cursorHighlightThickness: StateFlow<Int> = dataStore.cursorHighlightThicknessFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val cursorHighlightColor: StateFlow<String> = dataStore.cursorHighlightColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PURPLE")

    val panelAlpha: StateFlow<Float> = dataStore.panelAlphaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.9f)

    val panelSize: StateFlow<String> = dataStore.panelSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MEDIUM")

    val panelColor: StateFlow<String> = dataStore.panelColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT")

    val vibration: StateFlow<Boolean> = dataStore.vibrationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val scrollSpeed: StateFlow<Float> = dataStore.scrollSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val autoStart: StateFlow<Boolean> = dataStore.autoStartFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val longClickMs: StateFlow<Int> = dataStore.longClickMsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 800)

    val doubleClickMs: StateFlow<Int> = dataStore.doubleClickMsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 130)

    // Stats flows
    val clicksToday: StateFlow<Int> = dataStore.clicksTodayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dragCount: StateFlow<Int> = dataStore.dragCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pointerDistance: StateFlow<Float> = dataStore.pointerDistanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0f)

    // Update functions
    fun updateCursorSize(size: Int) {
        viewModelScope.launch { dataStore.updateCursorSize(size) }
    }
    fun updateCursorSizeText(size: Int) {
        viewModelScope.launch { dataStore.updateCursorSizeText(size) }
    }
    fun updateCursorSizeHover(size: Int) {
        viewModelScope.launch { dataStore.updateCursorSizeHover(size) }
    }
    fun updateTrackTextCursor(track: Boolean) {
        viewModelScope.launch { dataStore.updateTrackTextCursor(track) }
    }
    fun updateTrackHoverCursor(track: Boolean) {
        viewModelScope.launch { dataStore.updateTrackHoverCursor(track) }
    }

    fun updateCursorSensitivity(sensitivity: Float) {
        viewModelScope.launch { dataStore.updateCursorSensitivity(sensitivity) }
    }

    fun updateCursorColor(color: String) {
        viewModelScope.launch { dataStore.updateCursorColor(color) }
    }

    fun updateCursorClickVfx(enabled: Boolean) {
        viewModelScope.launch { dataStore.updateCursorClickVfx(enabled) }
    }

    fun updateCursorClickVfxColor(color: String) {
        viewModelScope.launch { dataStore.updateCursorClickVfxColor(color) }
    }

    fun updateDefaultCursorShape(shape: String) {
        viewModelScope.launch { dataStore.updateDefaultCursorShape(shape) }
    }

    fun updateHoverCursorShape(shape: String) {
        viewModelScope.launch { dataStore.updateHoverCursorShape(shape) }
    }

    fun updateTextCursorShape(shape: String) {
        viewModelScope.launch { dataStore.updateTextCursorShape(shape) }
    }

    fun updateDefaultCursorImage(imagePath: String) {
        viewModelScope.launch { dataStore.updateDefaultCursorImage(imagePath) }
    }

    fun updateHoverCursorImage(imagePath: String) {
        viewModelScope.launch { dataStore.updateHoverCursorImage(imagePath) }
    }

    fun updateTextCursorImage(imagePath: String) {
        viewModelScope.launch { dataStore.updateTextCursorImage(imagePath) }
    }

    fun updateCursorHighlightEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.updateCursorHighlightEnabled(enabled) }
    }

    fun updateCursorHighlightThickness(thickness: Int) {
        viewModelScope.launch { dataStore.updateCursorHighlightThickness(thickness) }
    }

    fun updateCursorHighlightColor(color: String) {
        viewModelScope.launch { dataStore.updateCursorHighlightColor(color) }
    }

    fun updatePanelAlpha(alpha: Float) {
        viewModelScope.launch { dataStore.updatePanelAlpha(alpha) }
    }

    fun updatePanelSize(size: String) {
        viewModelScope.launch { dataStore.updatePanelSize(size) }
    }

    fun updatePanelColor(color: String) {
        viewModelScope.launch { dataStore.updatePanelColor(color) }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch { dataStore.updateVibration(enabled) }
    }

    fun updateScrollSpeed(speed: Float) {
        viewModelScope.launch { dataStore.updateScrollSpeed(speed) }
    }

    fun updateAutoStart(enabled: Boolean) {
        viewModelScope.launch { dataStore.updateAutoStart(enabled) }
    }

    fun updateLongClickMs(ms: Int) {
        viewModelScope.launch { dataStore.updateLongClickMs(ms) }
    }

    fun updateDoubleClickMs(ms: Int) {
        viewModelScope.launch { dataStore.updateDoubleClickMs(ms) }
    }

    fun completeFirstRun() {
        viewModelScope.launch { dataStore.updateFirstRun(false) }
    }

    fun resetSettings() {
        viewModelScope.launch { dataStore.resetAllSettings() }
    }

    // Permission helpers
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasAccessibilityPermission(context: Context): Boolean {
        // Can directly check coordinator binding status
        return PointerServiceCoordinator.isAccessibilityActive.value
    }
}
