package com.example.core.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundEffects {
    private val toneGenerator: ToneGenerator? by lazy {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (t: Throwable) {
            null
        }
    }

    fun playWhistle() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 300)
        } catch (t: Throwable) {
            // Safe fallback
        }
    }

    fun playFlagThrow() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (t: Throwable) {
            // Safe fallback
        }
    }
    
    fun playThud() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 50)
        } catch (t: Throwable) {
            // Safe fallback
        }
    }
}
