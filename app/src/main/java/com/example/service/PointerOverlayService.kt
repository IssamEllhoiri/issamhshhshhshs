package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.datastore.SettingsDataStore
import com.example.data.model.CursorShape
import com.example.overlay.cursor.CursorView
import com.example.overlay.panel.FloatingPanelView
import com.example.util.PointerServiceCoordinator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class PointerOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "pointer_service"
        private const val NOTIFICATION_ID = 4221
        const val ACTION_STOP = "com.pointerx.STOP_SERVICE"
    }

    private lateinit var windowManager: WindowManager
    private var cursorView: CursorView? = null
    private var panelView: FloatingPanelView? = null

    private lateinit var cursorParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var dataStore: SettingsDataStore

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dataStore = SettingsDataStore(this)
        
        PointerServiceCoordinator.overlayService = this
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        setupOverlays()
        observeCoordinatorState()
        observePreferences()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PointerX Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "PointerX mouse overlay service running state"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, PointerOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Using standard compass icon as pointer representation
            .setContentTitle("PointerX يعمل")
            .setContentText("اضغط لفتح إعدادات المؤشر")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupOverlays() {
        val density = resources.displayMetrics.density
        val cursorSize = (80 * density).toInt()

        val (screenWidth, screenHeight) = PointerServiceCoordinator.getRealScreenSize(this)
        val startX = screenWidth / 2f
        val startY = screenHeight / 2f

        // 1. Cursor Window Params
        cursorParams = WindowManager.LayoutParams(
            cursorSize,
            cursorSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (startX - (cursorSize / 2f)).toInt()
            y = (startY - (cursorSize / 2f)).toInt()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        cursorView = CursorView(this).apply {
            // Register click listener triggers through coordinator
            PointerServiceCoordinator.updateCursorPosition(startX, startY)
        }
        windowManager.addView(cursorView, cursorParams)

        // 2. Floating Control Panel Params
        // Initial state is Collapsed: 18dp x 60dp
        val cWidth = (18 * density).toInt()
        val cHeight = (60 * density).toInt()

        panelParams = WindowManager.LayoutParams(
            cWidth,
            cHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - cWidth
            y = screenHeight / 2 - cHeight / 2
        }

        panelView = FloatingPanelView(this, windowManager, panelParams, dataStore, serviceScope)
        windowManager.addView(panelView, panelParams)
    }

    private fun observeCoordinatorState() {
        // Observe Cursor position updates to move the Window layout
        serviceScope.launch {
            PointerServiceCoordinator.cursorX.collect { curX ->
                updateCursorPositionOnScreen(curX, PointerServiceCoordinator.cursorY.value)
            }
        }
        serviceScope.launch {
            PointerServiceCoordinator.cursorY.collect { curY ->
                updateCursorPositionOnScreen(PointerServiceCoordinator.cursorX.value, curY)
            }
        }

        // Observe Cursor shape changes
        serviceScope.launch {
            PointerServiceCoordinator.cursorShape.collect { shape ->
                cursorView?.setShape(shape)
            }
        }
    }

    private fun observePreferences() {
        // Read Settings dynamically
        serviceScope.launch {
            dataStore.cursorSizeFlow.collectLatest { size ->
                cursorView?.setCursorSize(size)
            }
        }
        serviceScope.launch {
            dataStore.cursorColorFlow.collectLatest { color ->
                cursorView?.setCursorColor(color)
            }
        }
        serviceScope.launch {
            dataStore.cursorClickVfxFlow.collectLatest { enabled ->
                cursorView?.setClickVfxEnabled(enabled)
            }
        }
        serviceScope.launch {
            dataStore.cursorClickVfxColorFlow.collectLatest { color ->
                cursorView?.setClickVfxColor(color)
            }
        }
        serviceScope.launch {
            dataStore.panelAlphaFlow.collectLatest { alpha ->
                panelView?.alpha = alpha
            }
        }
        serviceScope.launch {
            dataStore.defaultCursorShapeFlow.collectLatest { shapeStr ->
                val shape = try {
                    CursorShape.valueOf(shapeStr.uppercase())
                } catch (e: Exception) {
                    CursorShape.DEFAULT
                }
                PointerServiceCoordinator.defaultCursorShape = shape
            }
        }
        serviceScope.launch {
            dataStore.hoverCursorShapeFlow.collectLatest { shapeStr ->
                val shape = try {
                    CursorShape.valueOf(shapeStr.uppercase())
                } catch (e: Exception) {
                    CursorShape.POINTER
                }
                PointerServiceCoordinator.hoverCursorShape = shape
            }
        }
        serviceScope.launch {
            dataStore.textCursorShapeFlow.collectLatest { shapeStr ->
                val shape = try {
                    CursorShape.valueOf(shapeStr.uppercase())
                } catch (e: Exception) {
                    CursorShape.TEXT
                }
                PointerServiceCoordinator.textCursorShape = shape
            }
        }
        serviceScope.launch {
            dataStore.defaultCursorImageFlow.collectLatest { imagePath ->
                cursorView?.setDefaultCursorImage(imagePath)
            }
        }
        serviceScope.launch {
            dataStore.hoverCursorImageFlow.collectLatest { imagePath ->
                cursorView?.setHoverCursorImage(imagePath)
            }
        }
        serviceScope.launch {
            dataStore.textCursorImageFlow.collectLatest { imagePath ->
                cursorView?.setTextCursorImage(imagePath)
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun updateCursorPositionOnScreen(x: Float, y: Float) {
        val view = cursorView ?: return
        val offset = view.hotspotOffset
        
        cursorParams.x = (x - offset).toInt()
        cursorParams.y = (y - offset).toInt()
        
        try {
            windowManager.updateViewLayout(view, cursorParams)
        } catch (e: Exception) {
            // Avoid crash if overlay view not yet fully drawn/attached
        }
    }

    fun triggerClickAnimation() {
        cursorView?.triggerClickVfx()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        
        // Remove views safely on service destruction
        cursorView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        
        PointerServiceCoordinator.overlayService = null
    }
}
