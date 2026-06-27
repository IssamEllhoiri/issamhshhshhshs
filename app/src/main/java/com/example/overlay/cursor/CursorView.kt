package com.example.overlay.cursor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.data.model.CursorShape

class CursorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cursorShape = CursorShape.DEFAULT
    private var cursorSizePx = 24f * resources.displayMetrics.density
    private var cursorColor = Color.parseColor("#EEEEF8") // Standard default light
    private var clickVfxEnabled = true

    // Visual effect variables
    private var clickRippleRadius = 0f
    private var clickRippleAlpha = 0
    private var clickAnimator: ValueAnimator? = null
    
    // Rotation for WAIT cursor
    private var waitRotationAngle = 0f
    private val waitAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 800
        repeatCount = ValueAnimator.INFINITE
        interpolator = null // Linear
        addUpdateListener {
            waitRotationAngle = it.animatedValue as Float
            if (cursorShape == CursorShape.WAIT) {
                invalidate()
            }
        }
    }

    // Paints
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = cursorColor // Unified color, no black border!
        strokeWidth = 2f * resources.displayMetrics.density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val vfxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#6C63FF")
    }

    // Custom bitmaps for image-based cursors
    private var defaultCursorBitmap: Bitmap? = null
    private var hoverCursorBitmap: Bitmap? = null
    private var textCursorBitmap: Bitmap? = null

    // Offset for cursor drawing to ensure click ripple can expand outside it
    // Cursor hotspot is exactly at (hotspotOffset, hotspotOffset)
    val hotspotOffset: Float
        get() = width / 2f

    init {
        // Start wait rotation animator
        waitAnimator.start()
    }

    fun setShape(shape: CursorShape) {
        if (cursorShape != shape) {
            cursorShape = shape
            invalidate()
        }
    }

    fun setCursorSize(dp: Int) {
        cursorSizePx = dp * resources.displayMetrics.density
        invalidate()
    }

    fun setCursorColor(colorString: String) {
        cursorColor = when (colorString.uppercase()) {
            "PURPLE" -> Color.parseColor("#6C63FF")
            "BLUE" -> Color.parseColor("#40C4FF")
            "RED" -> Color.parseColor("#FF5252")
            else -> Color.parseColor("#EEEEF8") // WHITE
        }
        strokePaint.color = cursorColor // Make outline color same as main color for a solid, unified look
        invalidate()
    }

    fun setClickVfxEnabled(enabled: Boolean) {
        clickVfxEnabled = enabled
    }

    fun setClickVfxColor(colorString: String) {
        val colorVal = when (colorString.uppercase()) {
            "PURPLE" -> Color.parseColor("#6C63FF")
            "BLUE" -> Color.parseColor("#40C4FF")
            "RED" -> Color.parseColor("#FF5252")
            "GOLD" -> Color.parseColor("#FFB300")
            "GREEN" -> Color.parseColor("#4CAF50")
            "WHITE" -> Color.parseColor("#EEEEF8")
            else -> Color.parseColor("#6C63FF") // PURPLE
        }
        vfxPaint.color = colorVal
        invalidate()
    }

    fun setDefaultCursorImage(path: String?) {
        defaultCursorBitmap = try {
            if (!path.isNullOrEmpty()) BitmapFactory.decodeFile(path) else null
        } catch (e: Exception) {
            null
        }
        invalidate()
    }

    fun setHoverCursorImage(path: String?) {
        hoverCursorBitmap = try {
            if (!path.isNullOrEmpty()) BitmapFactory.decodeFile(path) else null
        } catch (e: Exception) {
            null
        }
        invalidate()
    }

    fun setTextCursorImage(path: String?) {
        textCursorBitmap = try {
            if (!path.isNullOrEmpty()) BitmapFactory.decodeFile(path) else null
        } catch (e: Exception) {
            null
        }
        invalidate()
    }

    fun triggerClickVfx() {
        if (!clickVfxEnabled) return
        
        clickAnimator?.cancel()
        
        val maxRadius = 24f * resources.displayMetrics.density
        clickAnimator = ValueAnimator.ofFloat(0f, maxRadius).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                clickRippleRadius = animator.animatedValue as Float
                clickRippleAlpha = ((1.0f - fraction) * 153).toInt() // Max opacity 60% (153/255)
                invalidate()
            }
        }
        clickAnimator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val hX = hotspotOffset
        val hY = hotspotOffset

        // 1. Draw click VFX ripple under the cursor
        if (clickRippleAlpha > 0 && clickRippleRadius > 0f) {
            vfxPaint.alpha = clickRippleAlpha
            canvas.drawCircle(hX, hY, clickRippleRadius, vfxPaint)
        }

        // 2. Draw custom image cursor if available for active category
        val activeType = com.example.util.PointerServiceCoordinator.activeCursorType.value
        val customBitmap = when (activeType) {
            "GENERAL" -> defaultCursorBitmap
            "HOVER" -> hoverCursorBitmap
            "TEXT" -> textCursorBitmap
            else -> null
        }

        if (customBitmap != null) {
            val srcWidth = customBitmap.width.toFloat()
            val srcHeight = customBitmap.height.toFloat()
            if (srcWidth > 0f && srcHeight > 0f) {
                val scaleFactor = cursorSizePx / Math.max(srcWidth, srcHeight)
                canvas.save()
                // Move to hotspot
                canvas.translate(hX, hY)
                canvas.scale(scaleFactor, scaleFactor)
                
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(customBitmap, 0f, 0f, paint)
                
                canvas.restore()
                return // Bypass standard vector drawing
            }
        }

        // 3. Draw actual vector cursor shape
        if (cursorShape == CursorShape.NONE) return

        canvas.save()
        
        // Match hotspot alignment
        // Center-aligned: TEXT, CROSSHAIR, WAIT, RESIZE_H, RESIZE_V
        // Top-Left aligned: DEFAULT, POINTER, GRAB, GRABBING, NOT_ALLOWED
        val isCenterAligned = when (cursorShape) {
            CursorShape.TEXT, CursorShape.CROSSHAIR, CursorShape.WAIT, 
            CursorShape.RESIZE_H, CursorShape.RESIZE_V -> true
            else -> false
        }

        if (isCenterAligned) {
            canvas.translate(hX, hY)
        } else {
            // Top-left aligned cursors have hotspot at (hX, hY)
            canvas.translate(hX, hY)
        }

        val scale = cursorSizePx / (24f * resources.displayMetrics.density)
        canvas.scale(scale, scale)

        fillPaint.color = cursorColor

        when (cursorShape) {
            CursorShape.DEFAULT -> drawDefaultArrow(canvas)
            CursorShape.POINTER -> drawPointerHand(canvas)
            CursorShape.TEXT -> drawTextCursor(canvas)
            CursorShape.WAIT -> drawWaitCursor(canvas)
            CursorShape.CROSSHAIR -> drawCrosshairCursor(canvas)
            CursorShape.RESIZE_H -> drawResizeHCursor(canvas)
            CursorShape.RESIZE_V -> drawResizeVCursor(canvas)
            CursorShape.GRAB -> drawGrabHand(canvas)
            CursorShape.GRABBING -> drawGrabbingHand(canvas)
            CursorShape.NOT_ALLOWED -> drawNotAllowedCursor(canvas)
            else -> {}
        }

        canvas.restore()
    }

    private fun drawDefaultArrow(canvas: Canvas) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, 22f)
            lineTo(6f, 16f)
            lineTo(12f, 27f)
            lineTo(15f, 25.5f)
            lineTo(9f, 14.5f)
            lineTo(16f, 14.5f)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawPointerHand(canvas: Canvas) {
        val path = Path().apply {
            moveTo(0f, 12f)
            lineTo(0f, 4f)
            quadTo(1.5f, 1f, 3f, 4f)
            lineTo(3f, 8f)
            // Middle finger
            lineTo(3f, 6f)
            quadTo(4.5f, 3f, 6f, 6f)
            lineTo(6f, 9f)
            // Ring
            lineTo(6f, 7f)
            quadTo(7.5f, 4.5f, 9f, 7f)
            lineTo(9f, 10f)
            // Pinky
            lineTo(9f, 9f)
            quadTo(10.5f, 6.5f, 12f, 9f)
            lineTo(12f, 15f)
            // Thumb
            lineTo(6f, 19f)
            lineTo(1f, 19f)
            quadTo(-2f, 16f, 0f, 12f)
            close()
        }
        
        canvas.save()
        canvas.translate(-4f, 0f)
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
        canvas.restore()
    }

    private fun drawTextCursor(canvas: Canvas) {
        // I-beam text cursor
        val density = resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cursorColor
            strokeWidth = 2.5f * density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        // Vertical bar
        canvas.drawLine(0f, -9f, 0f, 9f, paint)
        // Top crossbar
        canvas.drawLine(-4f, -9f, 4f, -9f, paint)
        // Bottom crossbar
        canvas.drawLine(-4f, 9f, 4f, 9f, paint)
    }

    private fun drawWaitCursor(canvas: Canvas) {
        // Continuous spinning arc
        val density = resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cursorColor
            strokeWidth = 3f * density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        
        canvas.save()
        canvas.rotate(waitRotationAngle)
        val rect = RectF(-10f, -10f, 10f, 10f)
        canvas.drawArc(rect, 0f, 270f, false, paint)
        canvas.restore()
    }

    private fun drawCrosshairCursor(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cursorColor
            strokeWidth = 1.5f * density
        }
        val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cursorColor
            style = Paint.Style.FILL
        }

        // Crosshairs (+)
        canvas.drawLine(-11f, 0f, 11f, 0f, paintLine)
        canvas.drawLine(0f, -11f, 0f, 11f, paintLine)
        
        // Center dot
        canvas.drawCircle(0f, 0f, 2.5f, paintDot)
    }

    private fun drawResizeHCursor(canvas: Canvas) {
        // Horizontal arrows <- ->
        val path = Path().apply {
            // Left arrow
            moveTo(-11f, 0f)
            lineTo(-6f, -4f)
            lineTo(-6f, -1.5f)
            lineTo(6f, -1.5f)
            lineTo(6f, -4f)
            lineTo(11f, 0f)
            lineTo(6f, 4f)
            lineTo(6f, 1.5f)
            lineTo(-6f, 1.5f)
            lineTo(-6f, 4f)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawResizeVCursor(canvas: Canvas) {
        // Vertical arrows
        val path = Path().apply {
            moveTo(0f, -11f)
            lineTo(-4f, -6f)
            lineTo(-1.5f, -6f)
            lineTo(-1.5f, 6f)
            lineTo(-4f, 6f)
            lineTo(0f, 11f)
            lineTo(4f, 6f)
            lineTo(1.5f, 6f)
            lineTo(1.5f, -6f)
            lineTo(4f, -6f)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawGrabHand(canvas: Canvas) {
        // Open hand
        val path = Path().apply {
            moveTo(-6f, 6f)
            lineTo(-6f, -2f)
            quadTo(-5f, -4f, -4f, -2f)
            lineTo(-4f, 4f)
            // Middle
            lineTo(-4f, -4f)
            quadTo(-3f, -6f, -2f, -4f)
            lineTo(-2f, 4f)
            // Ring
            lineTo(-2f, -3f)
            quadTo(-1f, -5f, 0f, -3f)
            lineTo(0f, 4f)
            // Pinky
            lineTo(0f, -1f)
            quadTo(1f, -3f, 2f, -1f)
            lineTo(2f, 6f)
            // Palm body
            lineTo(2f, 10f)
            quadTo(0f, 14f, -3f, 14f)
            quadTo(-6f, 14f, -6f, 10f)
            // Thumb
            lineTo(-10f, 5f)
            quadTo(-11f, 3f, -9f, 2f)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawGrabbingHand(canvas: Canvas) {
        // Fist / Closed hand
        val path = Path().apply {
            moveTo(-5f, 5f)
            lineTo(-5f, 1f)
            quadTo(-3.5f, -1f, -2f, 1f)
            lineTo(-2f, 5f)
            // Middle
            lineTo(-2f, 1.5f)
            quadTo(-0.5f, -0.5f, 1f, 1.5f)
            lineTo(1f, 5f)
            // Ring
            lineTo(1f, 2f)
            quadTo(2.5f, 0f, 4f, 2f)
            lineTo(4f, 5f)
            // Pinky
            lineTo(4f, 3f)
            quadTo(5.5f, 1f, 7f, 3f)
            lineTo(7f, 9f)
            // Body
            lineTo(5f, 12f)
            quadTo(0f, 14f, -4f, 11f)
            // Thumb closed
            lineTo(-8f, 7f)
            quadTo(-9f, 5f, -7f, 4f)
            close()
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawNotAllowedCursor(canvas: Canvas) {
        // Red circle with slash
        val density = resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5252")
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }

        // Draw circle
        canvas.drawCircle(0f, 0f, 9f, paint)
        // Draw diagonal line
        canvas.drawLine(-6.3f, -6.3f, 6.3f, 6.3f, paint)
    }
}
