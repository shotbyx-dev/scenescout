package com.scenescout.app

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Installs a global crash catcher: the next launch shows the stack trace
 * in-app so a crash can be diagnosed without adb/logcat access.
 */
class SceneScoutApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stamp = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val trace = buildString {
                    appendLine("SceneScout crash — $stamp")
                    appendLine("Thread: ${thread.name}")
                    appendLine(throwable.stackTraceToString().take(12000))
                }
                File(filesDir, CRASH_FILE).writeText(trace)
            } catch (_: Exception) {
                // Never let the crash reporter crash.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val CRASH_FILE = "last_crash.log"

        /** Returns the saved crash trace, if any, and clears it. */
        fun consumeCrashLog(app: Application): String? {
            val f = File(app.filesDir, CRASH_FILE)
            if (!f.exists()) return null
            return try {
                val text = f.readText()
                f.delete()
                text.ifBlank { null }
            } catch (_: Exception) {
                null
            }
        }
    }
}
