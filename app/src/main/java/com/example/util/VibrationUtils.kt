package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationUtils {

    fun vibrate(context: Context, milliseconds: Long, amplitude: Int) {
        val vibrator = getVibrator(context) ?: return
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val amp = amplitude.coerceIn(1, 255)
            val effect = VibrationEffect.createOneShot(milliseconds, amp)
            vibrator.vibrate(effect, audioAttributes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateWaveform(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator = getVibrator(context) ?: return
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect, audioAttributes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrateLeftClick(context: Context) {
        vibrate(context, 20L, 80)
    }

    fun vibrateDoubleClick(context: Context) {
        // [delay, vibrate, delay, vibrate]
        // 0ms delay, 15ms vibrate, 80ms delay, 15ms vibrate
        vibrateWaveform(context, longArrayOf(0, 15, 80, 15), intArrayOf(0, 80, 0, 80))
    }

    fun vibrateLongClick(context: Context) {
        vibrate(context, 40L, 120)
    }

    fun vibrateStartDrag(context: Context) {
        vibrate(context, 30L, 100)
    }

    fun vibrateToggleDragMode(context: Context) {
        vibrateWaveform(context, longArrayOf(0, 20, 40, 40), intArrayOf(0, 100, 0, 60))
    }

    fun vibrateSnapToEdge(context: Context) {
        vibrate(context, 15L, 60)
    }

    fun vibrateError(context: Context) {
        vibrateWaveform(context, longArrayOf(0, 50, 50, 50), intArrayOf(0, 150, 0, 150))
    }
}
