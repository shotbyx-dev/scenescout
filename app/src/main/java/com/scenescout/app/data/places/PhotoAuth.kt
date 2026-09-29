package com.scenescout.app.data.places

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import okhttp3.OkHttpClient

/**
 * Header-based auth for Google Place Photo loads, per Google's current
 * "Secure direct mobile web service calls" guidance: the API key travels in
 * the `X-Goog-Api-Key` header (never in the URL), plus `X-Android-Package`
 * and `X-Android-Cert` so an Android-restricted key is honored.
 *
 * Coil loads photo URLs through [photoHttpClient]; the interceptor attaches
 * the headers only to Google API hosts, so nothing leaks elsewhere.
 */
object PhotoAuth {

    /** Current Places/photos API key (build key or the user's pasted key). */
    @Volatile
    var apiKey: String? = null

    private var initialized = false

    /**
     * Captures the package name and signing-cert fingerprint once, publishing
     * them on [GooglePlaces] so both REST calls and photo requests can carry
     * the Android app-restriction headers. Call early (e.g. from the main
     * activity); safe to call repeatedly.
     */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val app = context.applicationContext
        GooglePlaces.appPackage = app.packageName
        GooglePlaces.appCertSha1 = signingCertSha1Hex(app)
    }

    fun photoHttpClient(context: Context): OkHttpClient {
        val appContext = context.applicationContext
        // Ensure the package/cert values exist even if init() wasn't called.
        if (GooglePlaces.appPackage == null) init(appContext)
        return OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                // Strict match: a bare endsWith("googleapis.com") would also
                // match "evilgoogleapis.com" and leak the key header there.
                val authed = if (host == "googleapis.com" ||
                    host.endsWith(".googleapis.com")) {
                    request.newBuilder().apply {
                        apiKey?.takeIf { it.isNotBlank() }?.let {
                            header("X-Goog-Api-Key", it)
                        }
                        GooglePlaces.appPackage?.let {
                            header("X-Android-Package", it)
                        }
                        GooglePlaces.appCertSha1?.let {
                            header("X-Android-Cert", it)
                        }
                    }.build()
                } else {
                    request
                }
                chain.proceed(authed)
            }
            .build()
    }

    /**
     * SHA-1 of the app's signing certificate as colon-less lowercase hex,
     * the form the `X-Android-Cert` header expects. Null when it can't be
     * determined — the request still goes out with the other headers.
     */
    fun signingCertSha1Hex(context: Context): String? = try {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            ).signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
            )?.signatures
        }
        val sig = signatures?.firstOrNull() ?: return null
        MessageDigest.getInstance("SHA-1")
            .digest(sig.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    } catch (_: Exception) {
        null
    }
}
