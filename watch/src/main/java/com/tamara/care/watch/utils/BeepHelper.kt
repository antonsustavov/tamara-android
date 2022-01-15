package com.tamara.care.watch.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeepHelper @Inject constructor(@ApplicationContext context: Context) {
    val tonNG = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    fun beep(duration: Int) {
        tonNG.startTone(ToneGenerator.TONE_DTMF_0, duration)
    }
}