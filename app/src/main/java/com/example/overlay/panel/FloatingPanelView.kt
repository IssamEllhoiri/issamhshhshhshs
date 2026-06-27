package com.example.overlay.panel

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.example.data.model.CursorShape
import com.example.data.datastore.SettingsDataStore
import com.example.util.PointerServiceCoordinator
import com.example.util.VibrationUtils
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sqrt

enum class IconType {
    L_CLICK, R_CLICK, D_CLICK, LONG_CLICK, DRAG, SCROLL, KBD, BACK, HOME, RECENTS
}

@SuppressLint("ViewConstructor")
class FloatingPanelView(
    context: Context,
    private val windowManager: WindowManager,
    private val parentParams: WindowManager.LayoutParams,
    private val dataStore: SettingsDataStore,
    private val scope: CoroutineScope
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private var isExpanded = false

    // Dimensions
    private var collapsedWidth = (18 * density).toInt()
    private var collapsedHeight = (60 * density).toInt()
    private var expandedWidth = (260 * density).toInt()
    private var expandedHeight = (380 * density).toInt()

    private lateinit var collapsedView: CollapsedView
    private lateinit var expandedView: ExpandedView
    
    // Compact mode for hiding button grid and showing touchpad only
    private var isCompactMode = false
    private var compactExpandedHeight = (196 * density).toInt()

    // Scaling and Color settings fields
    private var scaleFactor = 1.0f
    private var panelColorTheme = "DEFAULT"

    // Touch variables for dragging the entire panel
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // Settings state
    private var vibrationEnabled = true
    private var sensitivity = 2.0f
    private var accelerationEnabled = true
    private var longClickMs = 800

    init {
        setupViews()
        loadSettings()
    }

    private fun setupViews() {
        // Collapsed View
        collapsedView = CollapsedView(context).apply {
            layoutParams = LayoutParams(collapsedWidth, collapsedHeight).apply {
                gravity = Gravity.CENTER
            }
            setOnClickListener {
                expandPanel()
            }
        }

        // Expanded View
        expandedView = ExpandedView(context).apply {
            layoutParams = LayoutParams(expandedWidth, expandedHeight)
            visibility = View.GONE
            onMinimizeClick = {
                collapsePanel()
            }
            onCloseClick = {
                // Terminate service via coordinator
                PointerServiceCoordinator.overlayService?.stopSelf()
            }
        }

        addView(collapsedView)
        addView(expandedView)

        // Title bar drag handling
        expandedView.titleBar.setOnTouchListener { _, event ->
            handlePanelDrag(event)
        }
    }

    private fun loadSettings() {
        scope.launch {
            dataStore.vibrationFlow.collect {
                vibrationEnabled = it
            }
        }
        scope.launch {
            dataStore.cursorSensitivityFlow.collect {
                sensitivity = it
            }
        }
        scope.launch {
            dataStore.cursorAccelFlow.collect {
                accelerationEnabled = it
            }
        }
        scope.launch {
            dataStore.longClickMsFlow.collect {
                longClickMs = it
            }
        }
        scope.launch {
            dataStore.panelSizeFlow.collect { size ->
                updatePanelScale(size)
            }
        }
        scope.launch {
            dataStore.panelColorFlow.collect { color ->
                panelColorTheme = color
                collapsedView.invalidate()
                expandedView.invalidate()
            }
        }
    }

    private fun updatePanelScale(size: String) {
        scaleFactor = when (size.uppercase()) {
            "SMALL" -> 0.8f
            "LARGE" -> 1.2f
            else -> 1.0f // MEDIUM
        }

        collapsedWidth = (18 * density * scaleFactor).toInt()
        collapsedHeight = (60 * density * scaleFactor).toInt()
        expandedWidth = (260 * density * scaleFactor).toInt()
        expandedHeight = (380 * density * scaleFactor).toInt()
        compactExpandedHeight = (196 * density * scaleFactor).toInt()

        collapsedView.layoutParams = LayoutParams(collapsedWidth, collapsedHeight).apply {
            gravity = Gravity.CENTER
        }

        expandedView.layoutParams = LayoutParams(expandedWidth, expandedHeight)

        // Scale the views
        collapsedView.scaleX = scaleFactor
        collapsedView.scaleY = scaleFactor
        expandedView.scaleX = scaleFactor
        expandedView.scaleY = scaleFactor

        collapsedView.pivotX = collapsedWidth / 2f
        collapsedView.pivotY = collapsedHeight / 2f
        expandedView.pivotX = expandedWidth / 2f
        expandedView.pivotY = expandedHeight / 2f

        val targetW = if (isExpanded) expandedWidth else collapsedWidth
        val targetH = if (isExpanded) {
            if (isCompactMode) compactExpandedHeight else expandedHeight
        } else {
            collapsedHeight
        }

        parentParams.width = targetW
        parentParams.height = targetH

        try {
            windowManager.updateViewLayout(this, parentParams)
        } catch (e: Exception) {}

        requestLayout()
    }

    fun toggleCompactMode() {
        isCompactMode = !isCompactMode
        
        expandedView.updateCompactState(isCompactMode)
        
        val targetHeight = if (isCompactMode) compactExpandedHeight else expandedHeight
        
        val animator = ValueAnimator.ofInt(parentParams.height, targetHeight).apply {
            duration = 200
            addUpdateListener {
                parentParams.height = it.animatedValue as Int
                try {
                    windowManager.updateViewLayout(this@FloatingPanelView, parentParams)
                } catch (e: Exception) {}
            }
        }
        animator.start()
    }

    private fun handlePanelDrag(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = parentParams.x
                initialY = parentParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                parentParams.x = (initialX + dx).toInt()
                parentParams.y = (initialY + dy).toInt()
                
                // Constrain vertical position using real physical screen bounds and dynamic panel height
                val (screenWidth, screenHeight) = PointerServiceCoordinator.getRealScreenSize(context)
                val limitTop = (20 * density).toInt()
                val currentPanelHeight = parentParams.height
                val limitBottom = screenHeight - currentPanelHeight - (20 * density).toInt()
                parentParams.y = parentParams.y.coerceIn(limitTop, limitBottom)

                windowManager.updateViewLayout(this, parentParams)
                true
            }
            MotionEvent.ACTION_UP -> {
                snapToScreenEdge()
                true
            }
            else -> false
        }
    }

    private fun snapToScreenEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val centerOfScreen = screenWidth / 2f
        val currentX = parentParams.x + (if (isExpanded) expandedWidth else collapsedWidth) / 2f

        val targetX = if (currentX < centerOfScreen) {
            // Snap Left
            if (isExpanded) 0 else -(collapsedWidth - (14 * density).toInt())
        } else {
            // Snap Right
            if (isExpanded) screenWidth - expandedWidth else screenWidth - (14 * density).toInt()
        }

        val startX = parentParams.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 200
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                parentParams.x = it.animatedValue as Int
                try {
                    windowManager.updateViewLayout(this@FloatingPanelView, parentParams)
                } catch (e: Exception) {
                    // Avoid crash if view removed
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (vibrationEnabled) {
                        VibrationUtils.vibrateSnapToEdge(context)
                    }
                }
            })
        }
        animator.start()
    }

    private fun expandPanel() {
        if (isExpanded) return
        isExpanded = true

        val initialW = collapsedWidth
        val initialH = collapsedHeight

        collapsedView.visibility = View.GONE
        expandedView.visibility = View.VISIBLE
        expandedView.alpha = 0f

        // Center on screen x coordinate or expand inwards
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        if (parentParams.x < screenWidth / 2f) {
            parentParams.x = 0
        } else {
            parentParams.x = screenWidth - expandedWidth
        }

        // Animated width and height expansion
        val targetH = if (isCompactMode) compactExpandedHeight else expandedHeight
        val widthAnimator = ValueAnimator.ofInt(initialW, expandedWidth)
        val heightAnimator = ValueAnimator.ofInt(initialH, targetH)

        widthAnimator.addUpdateListener {
            parentParams.width = it.animatedValue as Int
            try {
                windowManager.updateViewLayout(this, parentParams)
            } catch (e: Exception) {}
        }
        heightAnimator.addUpdateListener {
            parentParams.height = it.animatedValue as Int
            try {
                windowManager.updateViewLayout(this, parentParams)
            } catch (e: Exception) {}
        }

        val animatorSet = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                expandedView.alpha = alpha
            }
        }

        widthAnimator.start()
        heightAnimator.start()
        animatorSet.start()
    }

    private fun collapsePanel() {
        if (!isExpanded) return
        isExpanded = false

        val fadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 150
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                expandedView.alpha = it.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    expandedView.visibility = View.GONE
                    collapsedView.visibility = View.VISIBLE
                    
                    parentParams.width = collapsedWidth
                    parentParams.height = collapsedHeight
                    
                    // Snap back to edge properly as collapsed view
                    snapToScreenEdge()
                }
            })
        }
        fadeAnimator.start()
    }

    // Inner Touchpad View inside Floating Panel
    inner class TouchpadArea(context: Context) : View(context) {
        private var lastX = 0f
        private var lastY = 0f
        private var touchStartTime = 0L
        private var touchStartX = 0f
        private var touchStartY = 0f
        private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#1C1C28")
            strokeWidth = 2f * density
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val curX = event.x
            val curY = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = curX
                    lastY = curY
                    touchStartX = curX
                    touchStartY = curY
                    touchStartTime = System.currentTimeMillis()
                    
                    if (PointerServiceCoordinator.isDragMode.value) {
                        PointerServiceCoordinator.updateCursorShape(CursorShape.GRABBING)
                        if (vibrationEnabled) VibrationUtils.vibrateStartDrag(context)
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    var dx = curX - lastX
                    var dy = curY - lastY

                    // Apply sensitivity
                    dx *= sensitivity
                    dy *= sensitivity

                    // Apply acceleration logic
                    if (accelerationEnabled) {
                        val distance = sqrt(dx * dx + dy * dy)
                        val speedThreshold = 8f * density
                        if (distance > speedThreshold) {
                            dx *= 1.8f
                            dy *= 1.8f
                        }
                    }

                    if (PointerServiceCoordinator.isScrollMode.value) {
                        // Scroll Mode: Scroll screen using coordinates instead of moving mouse
                        val scrollMultiplier = -1.2f // inverted scroll feel
                        PointerServiceCoordinator.requestScroll(0f, dy * scrollMultiplier)
                    } else {
                        // Move Mouse mode
                        val (sW, sH) = PointerServiceCoordinator.getRealScreenSize(context)
                        val screenWidth = sW.toFloat()
                        val screenHeight = sH.toFloat()

                        var newCursorX = PointerServiceCoordinator.cursorX.value + dx
                        var newCursorY = PointerServiceCoordinator.cursorY.value + dy

                        newCursorX = newCursorX.coerceIn(0f, screenWidth)
                        newCursorY = newCursorY.coerceIn(0f, screenHeight)

                        PointerServiceCoordinator.updateCursorPosition(newCursorX, newCursorY)
                    }

                    lastX = curX
                    lastY = curY
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - touchStartTime
                    val travelDist = sqrt((curX - touchStartX) * (curX - touchStartX) + (curY - touchStartY) * (curY - touchStartY))
                    
                    if (PointerServiceCoordinator.isDragMode.value) {
                        PointerServiceCoordinator.updateCursorShape(CursorShape.GRAB)
                    }

                    // Touchpad tap detector (Short Tap to click)
                    if (duration < 200 && travelDist < 10 * density) {
                        PointerServiceCoordinator.requestClick()
                        if (vibrationEnabled) VibrationUtils.vibrateLeftClick(context)
                    }
                    return true
                }
            }
            return false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Draw visual decor inside touchpad
            val radius = 12f * density
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0E0E18")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, radius, radius, bgPaint)
            
            // Draw subtle grids or borders
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2A2A3C")
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
            }
            canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }
    }

    // Programmatically drawn custom M3 styled button
    inner class PanelButton(
        context: Context,
        val label: String,
        val iconType: IconType,
        private val onClick: () -> Unit
    ) : View(context) {

        var isActive = false
            set(value) {
                if (field != value) {
                    field = value
                    invalidate()
                }
            }

        private var isPressedState = false

        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#2A2A3C")
            strokeWidth = 1f * density
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9090A8")
            textSize = 10f * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isPressedState = true
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    isPressedState = false
                    invalidate()
                    onClick()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isPressedState = false
                    invalidate()
                    return true
                }
            }
            return false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val radius = 10f * density

            // 1. Draw Background
            bgPaint.color = when {
                isActive -> Color.parseColor("#6C63FF")
                isPressedState -> Color.parseColor("#CC6C63FF") // translucent active color
                else -> Color.parseColor("#1C1C28") // default dark
            }
            val rect = RectF(0f, 0f, w, h)
            canvas.drawRoundRect(rect, radius, radius, bgPaint)
            canvas.drawRoundRect(rect, radius, radius, borderPaint)

            // 2. Draw Icon & Label
            val activeColor = Color.parseColor("#EEEEF8")
            val defaultColor = Color.parseColor("#9090A8")
            val currentColor = if (isActive || isPressedState) activeColor else defaultColor

            iconPaint.color = currentColor
            textPaint.color = currentColor

            // Draw Icon programmatically
            canvas.save()
            canvas.translate(w / 2f, h / 2f - 4f * density)
            drawIcon(canvas, iconType)
            canvas.restore()

            // Draw text centered
            canvas.drawText(label, w / 2f, h - 8f * density, textPaint)
        }

        private fun drawIcon(canvas: Canvas, type: IconType) {
            val size = 7f * density
            when (type) {
                IconType.L_CLICK -> {
                    // Computer mouse with left click highlighted
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawRoundRect(-size, -size, size, size, 4f * density, 4f * density, iconPaint)
                    // Draw scroll wheel line
                    canvas.drawLine(0f, -size, 0f, 0f, iconPaint)
                    // Highlight left side of top part
                    val highlightPaint = Paint(iconPaint).apply {
                        style = Paint.Style.FILL
                    }
                    val path = Path().apply {
                        addRoundRect(-size, -size, 0f, 0f, floatArrayOf(4f*density, 4f*density, 0f, 0f, 0f, 0f, 0f, 0f), Path.Direction.CW)
                    }
                    canvas.drawPath(path, highlightPaint)
                }
                IconType.R_CLICK -> {
                    // Computer mouse with right click highlighted
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawRoundRect(-size, -size, size, size, 4f * density, 4f * density, iconPaint)
                    canvas.drawLine(0f, -size, 0f, 0f, iconPaint)
                    // Highlight right side of top part
                    val highlightPaint = Paint(iconPaint).apply {
                        style = Paint.Style.FILL
                    }
                    val path = Path().apply {
                        addRoundRect(0f, -size, size, 0f, floatArrayOf(0f, 0f, 4f*density, 4f*density, 0f, 0f, 0f, 0f), Path.Direction.CW)
                    }
                    canvas.drawPath(path, highlightPaint)
                }
                IconType.D_CLICK -> {
                    // Two cursor icons overlapping
                    iconPaint.style = Paint.Style.STROKE
                    val path1 = Path().apply {
                        moveTo(-4f, -4f)
                        lineTo(-4f, 4f)
                        lineTo(-1f, 1f)
                        lineTo(3f, 1f)
                        close()
                    }
                    canvas.drawPath(path1, iconPaint)
                    
                    canvas.save()
                    canvas.translate(4f, 4f)
                    canvas.drawPath(path1, iconPaint)
                    canvas.restore()
                }
                IconType.LONG_CLICK -> {
                    // Touch icon: index finger touching a ripple circle
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawCircle(0f, 0f, 3f * density, iconPaint)
                    
                    val handPath = Path().apply {
                        moveTo(-2f * density, 4f * density)
                        lineTo(-2f * density, -2f * density)
                        quadTo(-1f * density, -4f * density, 0f, -2f * density)
                        lineTo(0f, 4f * density)
                        quadTo(2f * density, 5f * density, 0f, 8f * density)
                    }
                    canvas.drawPath(handPath, iconPaint)
                }
                IconType.DRAG -> {
                    // 4-way arrow crosshair
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawLine(-size, 0f, size, 0f, iconPaint)
                    canvas.drawLine(0f, -size, 0f, size, iconPaint)
                    
                    // arrow heads
                    canvas.drawLine(-size, 0f, -size + 2f * density, -2f * density, iconPaint)
                    canvas.drawLine(-size, 0f, -size + 2f * density, 2f * density, iconPaint)
                    canvas.drawLine(size, 0f, size - 2f * density, -2f * density, iconPaint)
                    canvas.drawLine(size, 0f, size - 2f * density, 2f * density, iconPaint)
                }
                IconType.SCROLL -> {
                    // 2-way vertical arrow
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawLine(0f, -size, 0f, size, iconPaint)
                    // Up arrow head
                    canvas.drawLine(0f, -size, -2.5f * density, -size + 3f * density, iconPaint)
                    canvas.drawLine(0f, -size, 2.5f * density, -size + 3f * density, iconPaint)
                    // Down arrow head
                    canvas.drawLine(0f, size, -2.5f * density, size - 3f * density, iconPaint)
                    canvas.drawLine(0f, size, 2.5f * density, size - 3f * density, iconPaint)
                }
                IconType.KBD -> {
                    // Keyboard grid outline
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawRect(-size, -size + 2f * density, size, size - 2f * density, iconPaint)
                    // Draw lines
                    canvas.drawLine(-size + 3f * density, 0f, size - 3f * density, 0f, iconPaint)
                }
                IconType.BACK -> {
                    // Back arrow pointing left
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawLine(-size, 0f, size, 0f, iconPaint)
                    canvas.drawLine(-size, 0f, -size + 4f * density, -3f * density, iconPaint)
                    canvas.drawLine(-size, 0f, -size + 4f * density, 3f * density, iconPaint)
                }
                IconType.HOME -> {
                    // House shape
                    iconPaint.style = Paint.Style.STROKE
                    val path = Path().apply {
                        moveTo(0f, -size)
                        lineTo(-size, -1f * density)
                        lineTo(-size + 2f * density, size)
                        lineTo(size - 2f * density, size)
                        lineTo(size, -1f * density)
                        close()
                    }
                    canvas.drawPath(path, iconPaint)
                }
                IconType.RECENTS -> {
                    // Overlapping apps card
                    iconPaint.style = Paint.Style.STROKE
                    canvas.drawRoundRect(-size + 2f*density, -size + 2f*density, size - 2f*density, size - 2f*density, 2f*density, 2f*density, iconPaint)
                    canvas.drawRoundRect(-size, -size, size - 4f*density, size - 4f*density, 2f*density, 2f*density, iconPaint)
                }
            }
        }
    }

    // Collapsed vertical bar sticking to edge of screen
    inner class CollapsedView(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val radius = w / 2f

            // 1. Background
            val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (panelColorTheme.uppercase()) {
                    "BLACK" -> Color.parseColor("#CC000000") // 80% opacity pitch black
                    "BLUE" -> Color.parseColor("#CC0A192F")  // 80% opacity deep navy
                    "RED" -> Color.parseColor("#CC2B1212")   // 80% opacity deep crimson
                    else -> Color.parseColor("#CC12121A")    // 80% opacity slate (Default)
                }
                style = Paint.Style.FILL
            }
            val rect = RectF(0f, 0f, w, h)
            canvas.drawRoundRect(rect, radius, radius, paintBg)

            // 2. Outline border
            val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (panelColorTheme.uppercase()) {
                    "BLACK" -> Color.parseColor("#444444")
                    "BLUE" -> Color.parseColor("#172A45")
                    "RED" -> Color.parseColor("#401F1F")
                    else -> Color.parseColor("#2A2A3C")
                }
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
            }
            canvas.drawRoundRect(rect, radius, radius, paintBorder)

            // 3. Arrow icon pointing inward
            val paintArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EEEEF8")
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = Path()
            // Determine arrow direction based on screen edge
            val isOnLeft = parentParams.x < resources.displayMetrics.widthPixels / 2f
            if (isOnLeft) {
                // Arrow pointing right
                path.moveTo(w / 2f - 2f * density, h / 2f - 4f * density)
                path.lineTo(w / 2f + 2f * density, h / 2f)
                path.lineTo(w / 2f - 2f * density, h / 2f + 4f * density)
            } else {
                // Arrow pointing left
                path.moveTo(w / 2f + 2f * density, h / 2f - 4f * density)
                path.lineTo(w / 2f - 2f * density, h / 2f)
                path.lineTo(w / 2f + 2f * density, h / 2f + 4f * density)
            }
            canvas.drawPath(path, paintArrow)
        }
    }

    // Expanded Control Panel view layout
    inner class ExpandedView(context: Context) : FrameLayout(context) {
        var onMinimizeClick: (() -> Unit)? = null
        var onCloseClick: (() -> Unit)? = null

        lateinit var titleBar: FrameLayout
        private lateinit var touchpadArea: TouchpadArea
        lateinit var gridLayout: FrameLayout
        private val buttonList = mutableListOf<PanelButton>()
        lateinit var toggleCompactButton: android.widget.TextView

        init {
            setupPanel()
        }

        private fun setupPanel() {
            // Background Canvas Style
            setBackgroundResource(0) // dynamic drawable inside onDraw

            // 1. Title bar (40dp)
            titleBar = FrameLayout(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (40 * density).toInt())
                
                // Title Text (Empty as requested to hide the app name)
                val titleTv = android.widget.TextView(context).apply {
                    text = ""
                    setTextColor(Color.parseColor("#EEEEF8"))
                    textSize = 15f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((16 * density).toInt(), 0, 0, 0)
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.START)
                }
                addView(titleTv)

                // High-visibility transparent eye emoji button to toggle compact mode (showing/hiding buttons grid)
                toggleCompactButton = android.widget.TextView(context).apply {
                    text = "👁️"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                    
                    // Transparent background (selectableItemBackground if supported, or null for simple click)
                    val outValue = android.util.TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
                    setBackgroundResource(outValue.resourceId)
                    
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
                        leftMargin = (16 * density).toInt()
                    }
                    
                    setOnClickListener { toggleCompactMode() }
                }
                addView(toggleCompactButton)

                // Minimize button [_]
                val minBtn = android.widget.ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_media_play) // Let's draw standard icons programmatically or use system ones
                    // Actually, we can draw programmatically. Let's make a beautiful titlebar button
                    setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                    setOnClickListener { onMinimizeClick?.invoke() }
                    layoutParams = LayoutParams((36 * density).toInt(), (36 * density).toInt(), Gravity.END).apply {
                        rightMargin = (44 * density).toInt()
                        gravity = Gravity.CENTER_VERTICAL
                    }
                }
                // Close button [x]
                val closeBtn = android.widget.ImageView(context).apply {
                    setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                    setOnClickListener { onCloseClick?.invoke() }
                    layoutParams = LayoutParams((36 * density).toInt(), (36 * density).toInt(), Gravity.END).apply {
                        rightMargin = (8 * density).toInt()
                        gravity = Gravity.CENTER_VERTICAL
                    }
                }
                
                // Let's draw a custom minimize and close indicator inside Title bar onDraw to make it very premium!
            }
            addView(titleBar)

            // Custom Title Bar buttons decoration
            val btnMinimize = View(context).apply {
                layoutParams = LayoutParams((32 * density).toInt(), (32 * density).toInt(), Gravity.END).apply {
                    rightMargin = (48 * density).toInt()
                    topMargin = (4 * density).toInt()
                }
                setOnClickListener { onMinimizeClick?.invoke() }
            }
            val btnClose = View(context).apply {
                layoutParams = LayoutParams((32 * density).toInt(), (32 * density).toInt(), Gravity.END).apply {
                    rightMargin = (12 * density).toInt()
                    topMargin = (4 * density).toInt()
                }
                setOnClickListener { onCloseClick?.invoke() }
            }
            addView(btnMinimize)
            addView(btnClose)

            // 2. Touchpad Area (220dp x 136dp)
            touchpadArea = TouchpadArea(context).apply {
                layoutParams = LayoutParams((228 * density).toInt(), (136 * density).toInt()).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = (44 * density).toInt()
                }
            }
            addView(touchpadArea)

            // 3. Grid of buttons below Touchpad (starting topMargin = 190dp)
            gridLayout = FrameLayout(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (180 * density).toInt()).apply {
                    topMargin = (186 * density).toInt()
                    leftMargin = (16 * density).toInt()
                    rightMargin = (16 * density).toInt()
                }
            }

            // Button definitions
            val bWidth = (72 * density).toInt()
            val bHeight = (40 * density).toInt()
            val spacingX = (6 * density).toInt()
            val spacingY = (6 * density).toInt()

            fun addButton(col: Int, row: Int, label: String, type: IconType, action: (PanelButton) -> Unit) {
                var btn: PanelButton? = null
                btn = PanelButton(context, label, type) {
                    if (vibrationEnabled) VibrationUtils.vibrateLeftClick(context)
                    action(btn!!)
                }
                btn.layoutParams = LayoutParams(bWidth, bHeight).apply {
                    leftMargin = col * (bWidth + spacingX)
                    topMargin = row * (bHeight + spacingY)
                }
                gridLayout.addView(btn)
                buttonList.add(btn)
            }

            // Row 1
            addButton(0, 0, "L-CLK", IconType.L_CLICK) {
                PointerServiceCoordinator.requestClick()
            }
            addButton(1, 0, "R-CLK", IconType.R_CLICK) {
                PointerServiceCoordinator.requestRightClick()
            }
            addButton(2, 0, "D-CLK", IconType.D_CLICK) {
                PointerServiceCoordinator.requestDoubleClick()
            }

            // Row 2
            addButton(0, 1, "LNG-CLK", IconType.LONG_CLICK) {
                PointerServiceCoordinator.requestLongClick(longClickMs.toLong())
            }
            addButton(1, 1, "DRAG", IconType.DRAG) { btn ->
                val nextDrag = !PointerServiceCoordinator.isDragMode.value
                PointerServiceCoordinator.toggleDragMode(nextDrag)
                btn.isActive = nextDrag
                if (vibrationEnabled) VibrationUtils.vibrateToggleDragMode(context)
                
                // Deactivate scroll mode if it was active
                if (nextDrag && PointerServiceCoordinator.isScrollMode.value) {
                    PointerServiceCoordinator.toggleScrollMode(false)
                    findButtonByLabel("SCROLL")?.isActive = false
                }
            }
            addButton(2, 1, "SCROLL", IconType.SCROLL) { btn ->
                val nextScroll = !PointerServiceCoordinator.isScrollMode.value
                PointerServiceCoordinator.toggleScrollMode(nextScroll)
                btn.isActive = nextScroll
                
                // Deactivate drag mode if it was active
                if (nextScroll && PointerServiceCoordinator.isDragMode.value) {
                    PointerServiceCoordinator.toggleDragMode(false)
                    findButtonByLabel("DRAG")?.isActive = false
                }
            }

            // Row 3
            addButton(0, 2, "KBD", IconType.KBD) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }
            addButton(1, 2, "BCK", IconType.BACK) {
                PointerServiceCoordinator.requestBack()
            }
            addButton(2, 2, "HOME", IconType.HOME) {
                PointerServiceCoordinator.requestHome()
            }

            // Row 4: Recent apps (Takes up full/centered width or beautifully aligned)
            val rWidth = (228 * density).toInt()
            val rHeight = (36 * density).toInt()
            val btnRecents = PanelButton(context, "RECENT APPS", IconType.RECENTS) {
                if (vibrationEnabled) VibrationUtils.vibrateLeftClick(context)
                PointerServiceCoordinator.requestRecents()
            }.apply {
                layoutParams = LayoutParams(rWidth, rHeight).apply {
                    topMargin = 3 * (bHeight + spacingY)
                    leftMargin = 0
                }
            }
            gridLayout.addView(btnRecents)
            buttonList.add(btnRecents)

            addView(gridLayout)
        }

        private fun findButtonByLabel(label: String): PanelButton? {
            return buttonList.find { it.label == label }
        }

        fun updateCompactState(compact: Boolean) {
            gridLayout.visibility = if (compact) View.GONE else View.VISIBLE
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val radius = 20f * density

            // Draw primary background card
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (panelColorTheme.uppercase()) {
                    "BLACK" -> Color.parseColor("#E6000000") // 90% opacity pitch black
                    "BLUE" -> Color.parseColor("#E60A192F")  // 90% opacity deep navy
                    "RED" -> Color.parseColor("#E62B1212")   // 90% opacity deep crimson
                    else -> Color.parseColor("#E612121A")    // 90% opacity slate (Default)
                }
                style = Paint.Style.FILL
            }
            val rect = RectF(0f, 0f, w, h)
            canvas.drawRoundRect(rect, radius, radius, bgPaint)

            // Draw glowing subtle accent border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (panelColorTheme.uppercase()) {
                    "BLACK" -> Color.parseColor("#444444")
                    "BLUE" -> Color.parseColor("#172A45")
                    "RED" -> Color.parseColor("#401F1F")
                    else -> Color.parseColor("#2A2A3C")
                }
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
            }
            canvas.drawRoundRect(rect, radius, radius, borderPaint)

            // Draw title bar split line
            val splitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (panelColorTheme.uppercase()) {
                    "BLACK" -> Color.parseColor("#222222")
                    "BLUE" -> Color.parseColor("#1F4068")
                    "RED" -> Color.parseColor("#5C2E2E")
                    else -> Color.parseColor("#1C1C28")
                }
                strokeWidth = 1f * density
            }
            canvas.drawLine(0f, 40f * density, w, 40f * density, splitPaint)

            // Draw custom minimize [_] and close [x] icons in title bar
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#9090A8")
                strokeWidth = 2f * density
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }

            // Minimize icon [_] at end of title bar
            val minX = w - 64 * density
            val minY = 20 * density
            canvas.drawLine(minX - 5 * density, minY + 3 * density, minX + 5 * density, minY + 3 * density, iconPaint)

            // Close icon [x]
            val clX = w - 28 * density
            val clY = 20 * density
            canvas.drawLine(clX - 4 * density, clY - 4 * density, clX + 4 * density, clY + 4 * density, iconPaint)
            canvas.drawLine(clX + 4 * density, clY - 4 * density, clX - 4 * density, clY + 4 * density, iconPaint)
        }
    }
}
