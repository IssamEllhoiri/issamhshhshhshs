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
        val trackText = PointerServiceCoordinator.trackTextCursor
        val trackHover = PointerServiceCoordinator.trackHoverCursor
        if (!trackText && !trackHover) {
            lastNodeJob?.cancel()
            PointerServiceCoordinator.updateCursorShape(PointerServiceCoordinator.defaultCursorShape, "GENERAL")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= 60) { // Much faster throttled lookup to feel snappy and instant (60ms)
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
            // Ensure the final hover state is captured quickly when the cursor stops moving
            lastNodeJob?.cancel()
            lastNodeJob = serviceScope.launch {
                delay(70) // Short delay to capture final rested position instantly
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
        }

        if (root != null) {
            val node = findNodeAtPoint(root, x, y)
            if (node != null) {
                val result = determineShapeFromNode(node)
                try {
                    node.recycle()
                } catch (e: Exception) {}
                try {
                    root.recycle()
                } catch (e: Exception) {}
                // High Performance: Return the result directly when a node is found in the active window.
                // This avoids checking other windows when pointing to a general area of the active screen,
                // making the cursor return to default size/shape instantly!
                return result
            } else {
                try {
                    root.recycle()
                } catch (e: Exception) {}
            }
        }

        // Fallback check: check other windows (like status bar/nav bar/dialogs) if not found in active root
        try {
            val wins = windows
            if (wins != null) {
                for (win in wins) {
                    val r = win.root ?: continue
                    val node = findNodeAtPoint(r, x, y)
                    if (node != null) {
                        val result = determineShapeFromNode(node)
                        try {
                            node.recycle()
                        } catch (e: Exception) {}
                        try {
                            r.recycle()
                        } catch (e: Exception) {}
                        if (result.first != PointerServiceCoordinator.defaultCursorShape) {
                            return result
                        }
                    } else {
                        try {
                            r.recycle()
                        } catch (e: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {}

        return Pair(PointerServiceCoordinator.defaultCursorShape, "GENERAL")
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

    private fun findClickableOrSpecialAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = try { AccessibilityNodeInfo.obtain(node) } catch (e: Exception) { null } ?: return null
        var depth = 0
        while (current != null && depth < 10) {
            val className = current.className?.toString() ?: ""
            val text = current.text?.toString() ?: ""
            val contentDesc = current.contentDescription?.toString() ?: ""
            val viewId = current.viewIdResourceName?.lowercase() ?: ""
            val lowerText = text.lowercase()
            val lowerDesc = contentDesc.lowercase()

            // If we hit WebView, stop climbing to avoid treating the entire WebView as a link
            if (className.contains("WebView", ignoreCase = true)) {
                current.recycle()
                return null
            }

            // 1. Check if this node is editable
            val isEditable = current.isEditable || className.contains("EditText", ignoreCase = true)

            // 2. Check if this node is a file/folder indicator
            val isFileOrFolder = lowerText.contains(".pdf") || lowerText.contains(".zip") || lowerText.contains(".mp3") ||
                    lowerText.contains(".mp4") || lowerText.contains(".jpg") || lowerText.contains(".png") ||
                    lowerText.contains(".txt") || lowerText.contains(".doc") || lowerText.contains(".docx") ||
                    lowerText.contains(".xlsx") || lowerText.contains(".apk") || lowerText.contains(".rar") ||
                    lowerText.contains("file") || lowerText.contains("folder") || lowerText.contains("document") ||
                    lowerText.contains("ملف") || lowerText.contains("مجلد") || lowerText.contains("مستند") ||
                    lowerDesc.contains("file") || lowerDesc.contains("folder") || lowerDesc.contains("document") ||
                    lowerDesc.contains("ملف") || lowerDesc.contains("مجلد") || lowerDesc.contains("مستند") ||
                    viewId.contains("file") || viewId.contains("folder") || viewId.contains("document")

            // 3. Check if this node is a link
            val isLink = lowerText.startsWith("http://") || lowerText.startsWith("https://") ||
                    lowerText.contains("www.") || lowerText.contains("link") || lowerText.contains("رابط") ||
                    lowerDesc.contains("link") || lowerDesc.contains("رابط") ||
                    viewId.contains("link") || viewId.contains("url")

            // If we found any of these specific indicators, or if the node is clickable/focusable/enabled to be clicked
            if (isEditable || isFileOrFolder || isLink || current.isClickable) {
                return current
            }

            val parent = try { current.parent } catch (e: Exception) { null }
            current.recycle()
            current = parent
            depth++
        }
        current?.recycle()
        return null
    }

    private fun determineShapeFromNode(node: AccessibilityNodeInfo): Pair<CursorShape, String> {
        val trackText = PointerServiceCoordinator.trackTextCursor
        val trackHover = PointerServiceCoordinator.trackHoverCursor

        val specialNode = findClickableOrSpecialAncestor(node)
        val activeNode = specialNode ?: node

        val className = activeNode.className?.toString() ?: ""
        val text = activeNode.text?.toString() ?: ""
        val contentDesc = activeNode.contentDescription?.toString() ?: ""
        val viewId = activeNode.viewIdResourceName?.lowercase() ?: ""
        val packageName = activeNode.packageName?.toString() ?: ""

        val lowerText = text.lowercase()
        val lowerDesc = contentDesc.lowercase()
        val lowerPackage = packageName.lowercase()

        // Check if we are interacting with a browser or WebView container
        val isBrowserPackage = lowerPackage.contains("browser") || 
                lowerPackage.contains("chrome") || 
                lowerPackage.contains("firefox") || 
                lowerPackage.contains("opera") || 
                lowerPackage.contains("duckduckgo") || 
                lowerPackage.contains("microsoft.emmx") || 
                lowerPackage.contains("googlequicksearchbox") ||
                lowerPackage.contains("brave") ||
                lowerPackage.contains("via") ||
                className.contains("WebView", ignoreCase = true)

        // 1. Browser Search Bar & Web Search Engine Inputs Detection (محرك البحث)
        if (trackText) {
            val isSearchField = (activeNode.isEditable || className.contains("EditText", ignoreCase = true)) ||
                    (isBrowserPackage && (
                        viewId.contains("search") || viewId.contains("url") || viewId.contains("address") || viewId.contains("omnibox") || viewId.contains("location") ||
                        lowerText.contains("search") || lowerText.contains("بحث") || lowerText.contains("find") || lowerText.contains("اكتب") || lowerText.contains("type") || lowerText.contains("عنوان") ||
                        lowerDesc.contains("search") || lowerDesc.contains("بحث") || lowerDesc.contains("find") || lowerDesc.contains("type") || lowerDesc.contains("url")
                    ))

            if (isSearchField) {
                if (specialNode != null) {
                    try { specialNode.recycle() } catch (e: Exception) {}
                }
                return Pair(PointerServiceCoordinator.textCursorShape, "TEXT")
            }
        }

        // 2. Web Links & Browser Clickable Elements Detection (روابط داخل متصفح)
        if (trackHover) {
            val isLink = lowerText.startsWith("http://") || lowerText.startsWith("https://") ||
                    lowerText.contains("www.") || lowerText.contains("link") || lowerText.contains("رابط") ||
                    lowerDesc.contains("link") || lowerDesc.contains("رابط") ||
                    viewId.contains("link") || viewId.contains("url") ||
                    (isBrowserPackage && activeNode.isClickable && (
                        className.contains("View") || className.contains("TextView") || className.contains("Anchor") ||
                        lowerText.isNotEmpty() || lowerDesc.isNotEmpty()
                    ))

            // 3. File/Folder detection (ملفات أو مجلدات)
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
                if (specialNode != null) {
                    try { specialNode.recycle() } catch (e: Exception) {}
                }
                return Pair(PointerServiceCoordinator.hoverCursorShape, "HOVER")
            }
        }

        // General Editable text fields fallback
        if (trackText) {
            if (activeNode.isEditable || className.contains("EditText", ignoreCase = true) || (className.contains("TextView", ignoreCase = true) && activeNode.isFocusable)) {
                if (specialNode != null) {
                    try { specialNode.recycle() } catch (e: Exception) {}
                }
                return Pair(PointerServiceCoordinator.textCursorShape, "TEXT")
            }
        }

        // Clickable interactive nodes (buttons, lists, cards, switches, checkboxes)
        if (trackHover) {
            if (activeNode.isClickable || className.contains("Button", ignoreCase = true) || className.contains("Card", ignoreCase = true) || className.contains("Switch", ignoreCase = true) || className.contains("CheckBox", ignoreCase = true) || className.contains("ImageButton", ignoreCase = true)) {
                if (specialNode != null) {
                    try { specialNode.recycle() } catch (e: Exception) {}
                }
                return Pair(PointerServiceCoordinator.hoverCursorShape, "HOVER")
            }
        }

        // Progress indicators/Wait states
        if (className.contains("ProgressBar", ignoreCase = true) || className.contains("ProgressIndicator", ignoreCase = true)) {
            if (specialNode != null) {
                try { specialNode.recycle() } catch (e: Exception) {}
            }
            return Pair(CursorShape.WAIT, "WAIT")
        }

        // Non-interactive/disabled elements
        if (!activeNode.isEnabled) {
            if (specialNode != null) {
                try { specialNode.recycle() } catch (e: Exception) {}
            }
            return Pair(CursorShape.NOT_ALLOWED, "NOT_ALLOWED")
        }

        if (specialNode != null) {
            try { specialNode.recycle() } catch (e: Exception) {}
        }
        return Pair(PointerServiceCoordinator.defaultCursorShape, "GENERAL")
    }

    // Gesture dispatching implementations
    fun clickAt(x: Float, y: Float, duration: Long = 15L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val builder = GestureDescription.Builder().addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    fun doubleClickAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path, 0L, 15L)
        val stroke2 = GestureDescription.StrokeDescription(path, 100L, 15L) // 100ms delay
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
