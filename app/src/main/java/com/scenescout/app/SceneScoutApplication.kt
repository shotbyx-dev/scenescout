package com.scenescout.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.scenescout.app.data.places.PhotoAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Installs a global crash catcher: the next launch shows the stack trace
 * in-app so a crash can be diagnosed without adb/logcat access.
 *
 * Also supplies Coil's image loader: Place Photo requests go through an
 * OkHttp client that attaches the API key as headers (X-Goog-Api-Key,
 * X-Android-Package, X-Android-Cert) instead of embedding it in the URL.
 */
class SceneScoutApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        // The interceptor reads PhotoAuth.apiKey per request, so a pasted key
        // takes effect without rebuilding the loader.
        return ImageLoader.Builder(this)
            .okHttpClient { PhotoAuth.photoHttpClient(this) }
            .build()
    }

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
