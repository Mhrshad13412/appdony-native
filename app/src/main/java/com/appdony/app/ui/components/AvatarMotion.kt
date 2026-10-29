package com.appdony.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Job

// ───────────────────────── حرکت‌ها ─────────────────────────

internal class AvatarMotion {
    val offX = Animatable(0f)
    val offY = Animatable(0f)
    val hideShift = Animatable(0f)
    val tilt = Animatable(0f)

    val blink = Animatable(0f)
    val ear = Animatable(0f)
    val pounce = Animatable(0f)
    val land = Animatable(0f)

    // پیشرفت بیرون اومدن کاموا‌های منو
    val menuProg = List(3) { Animatable(0f) }

    // مسیر کاموا‌ها موقع بررسی لینک: ۰ بالای صفحه، ۱ آویزون و بازیگوش، ۲ قل‌خوران سمت راست، ۳ برگشته پیش گربه
    val dlPos = List(3) { Animatable(0f) }
    var dlJob: Job? = null

    var lastLean = 0f

    suspend fun lean(target: Float) {
        tilt.animateTo(target, spring(0.8f, Spring.StiffnessLow))
    }

    suspend fun relax() {
        tilt.animateTo(0f, spring(0.35f, Spring.StiffnessLow))
    }

    suspend fun blinkOnce() {
        blink.animateTo(1f, tween(80))
        blink.animateTo(0f, tween(130))
    }

    suspend fun twitchEar() {
        ear.animateTo(1f, tween(70))
        ear.animateTo(-0.5f, tween(70))
        ear.animateTo(0f, tween(140))
    }

    suspend fun hop(strength: Float = 1f) {
        pounce.animateTo(strength, tween(130))
        pounce.animateTo(0f, spring(0.35f, Spring.StiffnessMedium))
    }

    suspend fun landSquash() {
        land.snapTo(1f)
        land.animateTo(0f, spring(0.3f, Spring.StiffnessLow))
    }
}

