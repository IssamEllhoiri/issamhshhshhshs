package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.CursorShape
import com.example.util.PointerServiceCoordinator
import kotlinx.coroutines.*

class PointerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastNodeJob: Job? = null
    private var lastUpdateTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        PointerServiceCoordinator.accessibilityService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Required, but can be empty or used for tracking focus changes
    }

    override fun onInterrupt() {
        // Required
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        PointerServiceCoordinator.accessibilityService = null
    }

    fun onCursorMoved(x: Int, y: Int) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= 50) { // Throttled node lookup: 50ms interval (20 updates/sec) for super-fast PC feeling
            lastUpdateTime = currentTime
            lastNodeJob?.cancel()
            lastNodeJob = serviceScope.launch {
                if (!PointerServiceCoordinator.isDragMode.value && !PointerServiceCoordinator.isScrollMode.value) {
                    val result = detectShapeAtPoint(x, y)
                    withContext(Dispatchers.Main) {
                        PointerServiceCoordinator.updateCursorShape(result.first, result.second)
                    }
                }
            }
        } else {
            // Ensure the final hover state is captured when the cursor stops moving
            lastNodeJob?.cancel()
            lastNodeJob = serviceScope.launch {
                delay(60) // Short delay to capture final rested position
                if (!PointerServiceCoordinator.isDragMode.value && !PointerServiceCoordinator.isScrollMode.value) {
                    val result = detectShapeAtPoint(x, y)
                    withContext(Dispatchers.Main) {
                        PointerServiceCoordinator.updateCursorShape(result.first, result.second)
                    }
                }
            }
        }
    }

    private fun detectShapeAtPoint(x: Int, y: Int): Pair<CursorShape, String> {
        val root = try {
            rootInActiveWindow
        } catch (e: Exception) {
            null
        } ?: return Pair(PointerServiceCoordinator.defaultCursorShape, "GENERAL")

        val node = findNodeAtPoint(root, x, y)
        if (node == null) {
            try {
                root.recycle()
            } catch (e: Exception) {}
            return Pair(PointerServiceCoordinator.defaultCursorShape, "GENERAL")
        }

        val result = determineShapeFromNode(node)
        // Recycle traversed node structure safely (except root which we recycle manually or let systems handle)
        try {
            node.recycle()
        } catch (e: Exception) {}
        try {
            root.recycle()
        } catch (e: Exception) {}
        return result
    }

    private fun findNodeAtPoint(node: AccessibilityNodeInfo, x: Int, y: Int, depth: Int = 0): AccessibilityNodeInfo? {
        if (depth > 50) return null // Prevent StackOverflow on deeply nested trees
        
        val rect = Rect()
        try {
            node.getBoundsInScreen(rect)
        } catch (e: Exception) {
            return null
        }
        
        if (!rect.contains(x, y)) {
            return null
        }

        // Iterate backwards through children to match top-most elements first
        try {
            val childCount = node.childCount
            for (i in childCount - 1 downTo 0) {
                val child = try {
                    node.getChild(i)
                } catch (e: Exception) {
                    null
                } ?: continue
                
                val found = findNodeAtPoint(child, x, y, depth + 1)
                if (found != null) {
                    // If we found a match, recycle this node and return the match
                    if (found != child) {
                        try {
                            child.recycle()
                        } catch (e: Exception) {}
                    }
                    return found
                }
                try {
                    child.recycle()
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            // Safe fallback if childCount or other operations throw
        }

        // Returns current node if it contains the coordinates and has no matching children
        return try {
            AccessibilityNodeInfo.obtain(node)
        } catch (e: Exception) {
            null
        }
    }

    private fun determineShapeFromNode(node: AccessibilityNodeInfo): Pair<CursorShape, String> {
        val className = node.className?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val lowerText = text.lowercase()
        val lowerDesc = contentDesc.lowercase()

        // 1. Link detection (روابط)
        val isLink = lowerText.startsWith("http://") || lowerText.startsWith("https://") ||
                lowerText.contains("www.") || lowerText.contains("link") || lowerText.contains("رابط") ||
                lowerDesc.contains("link") || lowerDesc.contains("رابط") ||
                viewId.contains("link") || viewId.contains("url")

        // 2. File/Folder detection (ملفات أو مجلدات)
        val isFileOrFolder = lowerText.contains(".pdf") || lowerText.contains(".zip") || lowerText.contains(".mp3") ||
                lowerText.contains(".mp4") || lowerText.contains(".jpg") || lowerText.contains(".png") ||
                lowerText.contains(".txt") || lowerText.contains(".doc") || lowerText.contains(".docx") ||
                lowerText.contains(".xlsx") || lowerText.contains(".apk") || lowerText.contains(".rar") ||
                lowerText.contains("file") || lowerText.contains("folder") || lowerText.contains("document") ||
                lowerText.contains("ملف") || lowerText.contains("مجلد") || lowerText.contains("مستند") ||
                lowerDesc.contains("file") || lowerDesc.contains("folder") || lowerDesc.contains("document") ||
                lowerDesc.contains("ملف") || lowerDesc.contains("مجلد") || lowerDesc.contains("مستند") ||
                viewId.contains("file") || viewId.contains("folder") || viewId.contains("document")

        if (isLink || isFileOrFolder) {
            return Pair(PointerServiceCoordinator.hoverCursorShape, "HOVER")
        }

        // Editable text fields
        if (node.isEditable || className.contains("EditText", ignoreCase = true) || className.contains("TextView", ignoreCase = true) && node.isFocusable) {
            return Pair(PointerServiceCoordinator.textCursorShape, "TEXT")
        }

        // Clickable interactive nodes (buttons, lists, cards, switches, checkboxes)
        if (node.isClickable || className.contains("Button", ignoreCase = true) || className.contains("Card", ignoreCase = true) || className.contains("Switch", ignoreCase = true) || className.contains("CheckBox", ignoreCase = true) || className.contains("ImageButton", ignoreCase = true)) {
            return Pair(PointerServiceCoordinator.hoverCursorShape, "HOVER")
        }

        // Progress indicators/Wait states
        if (className.contains("ProgressBar", ignoreCase = true) || className.contains("ProgressIndicator", ignoreCase = true)) {
            return Pair(CursorShape.WAIT, "WAIT")
        }

        // Non-interactive/disabled elements
        if (!node.isEnabled) {
            return Pair(CursorShape.NOT_ALLOWED, "NOT_ALLOWED")
        }

        return Pair(PointerServiceCoordinator.defaultCursorShape, "GENERAL")
    }

    // Gesture dispatching implementations
    fun clickAt(x: Float, y: Float, duration: Long = 50L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val builder = GestureDescription.Builder().addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    fun doubleClickAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path, 0L, 50L)
        val stroke2 = GestureDescription.StrokeDescription(path, 130L, 50L) // 130ms delay
        val builder = GestureDescription.Builder().addStroke(stroke1).addStroke(stroke2)
        dispatchGesture(builder.build(), null, null)
    }

    fun rightClickAt(x: Float, y: Float) {
        // Simulator long click for right click context menu trigger
        longClickAt(x, y, 500L)
    }

    fun longClickAt(x: Float, y: Float, duration: Long = 800L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val builder = GestureDescription.Builder().addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    fun drag(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 600L)
        val builder = GestureDescription.Builder().addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    fun scroll(x: Float, y: Float, dx: Float, dy: Float) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + dx, y + dy)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 200L)
        val builder = GestureDescription.Builder().addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
}
