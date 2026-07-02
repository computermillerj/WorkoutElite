package com.workoutelite.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun KeepScreenOn(active: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(active, activity) {
        if (active) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

class AndroidWorkoutCuePlayer(context: Context) : WorkoutCuePlayer {
    private val appContext = context.applicationContext

    private val toneGenerator: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, TONE_VOLUME) }.getOrNull()
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun playIntervalCue() {
        beep(ToneGenerator.TONE_PROP_BEEP, durationMillis = 140)
        vibrate(durationMillis = 80)
    }

    override fun playHalfwayCue() {
        beep(ToneGenerator.TONE_PROP_ACK, durationMillis = 220)
        vibrate(durationMillis = 160)
    }

    override fun playCompleteCue() {
        beep(ToneGenerator.TONE_PROP_PROMPT, durationMillis = 320)
        vibrate(durationMillis = 250)
    }

    private fun beep(toneType: Int, durationMillis: Int) {
        runCatching { toneGenerator?.startTone(toneType, durationMillis) }
    }

    private fun vibrate(durationMillis: Long) {
        runCatching {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }

    private companion object {
        const val TONE_VOLUME = 80
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
