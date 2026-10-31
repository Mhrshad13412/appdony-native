package com.appdony.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlin.math.abs

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

    // ── خوردن به دیوار و گیج شدن ──
    val kick = Animatable(0f)      // ضربه‌ی برخورد؛ فنر میره و برمی‌گرده و نرم می‌لرزه
    var kickX = 0f                 // جهت دیواری که بهش خورده (۱ راست، ‎-۱ چپ)
    var kickY = 0f                 // (۱ پایین، ‎-۱ بالا)
    val dizzy = Animatable(0f)     // میزان گیجی ۰ تا ۱
    var dizzyJob: Job? = null
    var lastBonk = 0L

    // ── افتادن کاموا و دویدن گربه دنبالش ──
    var ballLost by mutableStateOf(false)   // کاموا از پای گربه جدا شده
    val ballWX = Animatable(0f)             // جای کاموای آزاد روی صفحه (پیکسل)
    val ballWY = Animatable(0f)
    val ballSpin = Animatable(0f)
    val grab = Animatable(0f)               // پنجه برای برداشتن کاموا
    var ballJob: Job? = null
    var dockJob: Job? = null

    // ── تشخیص تکون دادن سریع ──
    private var lastAxis = 0
    private var lastSign = 0
    private var shakeCount = 0
    private var shakeStart = 0L

    fun resetShake() {
        lastAxis = 0
        lastSign = 0
        shakeCount = 0
    }

    /** وقتی چند بار پشت‌سرهم و سریع جهت کشیدن عوض بشه true برمی‌گردونه */
    fun feedShake(dx: Float, dy: Float, t: Long, minStep: Float): Boolean {
        val ax = abs(dx)
        val ay = abs(dy)
        if (maxOf(ax, ay) < minStep) return false
        val axis = if (ax >= ay) 1 else 2
        val sign = if (axis == 1) (if (dx > 0f) 1 else -1) else (if (dy > 0f) 1 else -1)
        if (axis == lastAxis && sign == -lastSign) {
            if (shakeCount == 0 || t - shakeStart > 900L) {
                shakeCount = 1
                shakeStart = t
            } else {
                shakeCount++
            }
            if (shakeCount >= 4) {
                resetShake()
                return true
            }
        }
        lastAxis = axis
        lastSign = sign
        return false
    }

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
