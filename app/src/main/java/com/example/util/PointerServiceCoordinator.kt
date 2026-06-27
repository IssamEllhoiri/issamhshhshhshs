package com.example.util

import android.content.Context
import com.example.data.model.CursorShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.service.PointerAccessibilityService
import com.example.service.PointerOverlayService

object PointerServiceCoordinator {

    private val _isAccessibilityActive = MutableStateFlow(false)
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _cursorX = MutableStateFlow(200f)
    val cursorX: StateFlow<Float> = _cursorX.asStateFlow()

    private val _cursorY = MutableStateFlow(400f)
    val cursorY: StateFlow<Float> = _cursorY.asStateFlow()

    private val _cursorShape = MutableStateFlow(CursorShape.DEFAULT)
    val cursorShape: StateFlow<CursorShape> = _cursorShape.asStateFlow()

    var defaultCursorShape: CursorShape = CursorShape.DEFAULT
        set(value) {
            val oldDefault = field
            field = value
            if (_cursorShape.value == oldDefault) {
                _cursorShape.value = value
            }
        }

    var hoverCursorShape: CursorShape = CursorShape.POINTER
    var textCursorShape: CursorShape = CursorShape.TEXT

    private val _activeCursorType = MutableStateFlow("GENERAL")
    val activeCursorType: StateFlow<String> = _activeCursorType.asStateFlow()

    private val _isDragMode = MutableStateFlow(false)
    val isDragMode: StateFlow<Boolean> = _isDragMode.asStateFlow()

    private val _isScrollMode = MutableStateFlow(false)
    val isScrollMode: StateFlow<Boolean> = _isScrollMode.asStateFlow()

    // Strong/Weak reference to running service instances in the same process
    var accessibilityService: PointerAccessibilityService? = null
        set(value) {
            field = value
            _isAccessibilityActive.value = (value != null)
        }

    var overlayService: PointerOverlayService? = null
        set(value) {
            field = value
            _isOverlayActive.value = (value != null)
        }

    fun updateCursorPosition(x: Float, y: Float) {
        _cursorX.value = x
        _cursorY.value = y
        // Request accessibility service to update node-based cursor shape
        accessibilityService?.onCursorMoved(x.toInt(), y.toInt())
    }

    fun updateCursorShape(shape: CursorShape, type: String = "OTHER") {
        _cursorShape.value = shape
        _activeCursorType.value = type
    }

    fun toggleDragMode(active: Boolean) {
        _isDragMode.value = active
        if (active) {
            updateCursorShape(CursorShape.GRAB, "DRAG")
        } else {
            updateCursorShape(defaultCursorShape, "GENERAL")
        }
    }

    fun toggleScrollMode(active: Boolean) {
        _isScrollMode.value = active
        if (active) {
            updateCursorShape(CursorShape.CROSSHAIR, "SCROLL")
        } else {
            updateCursorShape(defaultCursorShape, "GENERAL")
        }
    }

    // Helper functions to request actions
    fun requestClick(duration: Long = 50L) {
        accessibilityService?.clickAt(_cursorX.value, _cursorY.value, duration)
        overlayService?.triggerClickAnimation()
    }

    fun requestRightClick() {
        accessibilityService?.rightClickAt(_cursorX.value, _cursorY.value)
        overlayService?.triggerClickAnimation()
    }

    fun requestDoubleClick() {
        accessibilityService?.doubleClickAt(_cursorX.value, _cursorY.value)
        overlayService?.triggerClickAnimation()
    }

    fun requestLongClick(duration: Long = 800L) {
        accessibilityService?.longClickAt(_cursorX.value, _cursorY.value, duration)
        overlayService?.triggerClickAnimation()
    }

    fun requestDrag(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        accessibilityService?.drag(fromX, fromY, toX, toY)
    }

    fun requestScroll(dx: Float, dy: Float) {
        accessibilityService?.scroll(_cursorX.value, _cursorY.value, dx, dy)
    }

    fun requestBack() {
        accessibilityService?.performBack()
    }

    fun requestHome() {
        accessibilityService?.performHome()
    }

    fun requestRecents() {
        accessibilityService?.performRecents()
    }

    fun getRealScreenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            Pair(metrics.widthPixels, metrics.heightPixels)
        }
    }
}
