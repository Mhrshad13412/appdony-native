package com.appdony.app.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

object CrashReporter {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val text = buildString {
                    append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) - ${Build.MODEL}\n")
                    append("Thread: ${thread.name}\n\n")
                    append(Log.getStackTraceString(throwable))
                }
                File(appContext.filesDir, FILE_NAME).writeText(text)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.readText().ifBlank { null } else null
        } catch (_: Throwable) {
            null
        }
    }

    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (_: Throwable) {
        }
    }
}
