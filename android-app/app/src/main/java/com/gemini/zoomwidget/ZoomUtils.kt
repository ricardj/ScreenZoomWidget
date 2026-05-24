package com.gemini.zoomwidget

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.util.Log

object ZoomUtils {
    private const val TAG = "ZoomUtils"

    fun toggleZoom(context: Context) {
        val resolver = context.contentResolver

        // 1. Get current state (preferring Samsung's index if available)
        val samsungZoomIndex = try {
            Settings.System.getInt(resolver, "screen_zoom")
        } catch (e: Exception) {
            try {
                // Samsung may have moved screen_zoom to Secure on some versions
                Settings.Secure.getInt(resolver, "screen_zoom")
            } catch (e2: Exception) {
                -1
            }
        }

        // 2. Define toggle targets
        val zoomedOutDpi = 320
        val zoomedInDpi = 600
        val zoomedOutIndex = 0
        val zoomedInIndex = 4

        // 3. Determine if we are currently "Zoomed In"
        val isCurrentlyZoomedIn = if (samsungZoomIndex != -1) {
            samsungZoomIndex >= 3
        } else {
            val currentDpi = try {
                Settings.Secure.getString(resolver, "display_density_forced")
            } catch (e: Exception) { null }
            currentDpi == zoomedInDpi.toString()
        }

        // 4. Set new values
        val targetDpi = if (isCurrentlyZoomedIn) zoomedOutDpi else zoomedInDpi
        val targetIndex = if (isCurrentlyZoomedIn) zoomedOutIndex else zoomedInIndex

        Log.d(TAG, "Toggle: currently=${if (isCurrentlyZoomedIn) "ZoomedIn" else "ZoomedOut"} " +
            "(samsungIdx=$samsungZoomIndex) → target DPI=$targetDpi, index=$targetIndex")

        applySettings(context, targetDpi, targetIndex)
    }

    private fun applySettings(context: Context, dpi: Int, index: Int) {
        val resolver = context.contentResolver
        var dpiApplied = false

        // ═══════════════════════════════════════════════════════════════
        // PRIMARY: IWindowManager.setForcedDisplayDensityForUser
        //
        // This is the SAME call that "adb shell wm density" makes.
        // With targetSdk=26, the hidden API restriction (max-target-o)
        // is lifted, so reflection is allowed. The app's
        // WRITE_SECURE_SETTINGS permission satisfies the WMS check.
        // ═══════════════════════════════════════════════════════════════
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val windowManagerBinder = getServiceMethod.invoke(null, "window") as IBinder

            val iWindowManagerStubClass = Class.forName("android.view.IWindowManager\$Stub")
            val asInterfaceMethod = iWindowManagerStubClass.getMethod("asInterface", IBinder::class.java)
            val windowManager = asInterfaceMethod.invoke(null, windowManagerBinder)

            val setDensityMethod = windowManager.javaClass.getMethod(
                "setForcedDisplayDensityForUser",
                Int::class.javaPrimitiveType,  // displayId
                Int::class.javaPrimitiveType,  // density
                Int::class.javaPrimitiveType   // userId
            )

            // displayId=0 (default display), density=dpi, userId=-2 (USER_CURRENT)
            setDensityMethod.invoke(windowManager, 0, dpi, -2)
            dpiApplied = true
            Log.d(TAG, "PRIMARY (IWindowManager): SUCCESS - DPI=$dpi applied")
        } catch (e: Exception) {
            Log.w(TAG, "PRIMARY (IWindowManager): FAILED", e)
        }

        // ═══════════════════════════════════════════════════════════════
        // FALLBACK A: wm density via shell
        // ═══════════════════════════════════════════════════════════════
        if (!dpiApplied) {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("/system/bin/sh", "-c", "wm density $dpi")
                )
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    dpiApplied = true
                    Log.d(TAG, "FALLBACK A (wm shell): SUCCESS - DPI=$dpi")
                } else {
                    val stderr = process.errorStream.bufferedReader().readText()
                    Log.w(TAG, "FALLBACK A (wm shell): FAILED exit=$exitCode stderr=$stderr")
                }
            } catch (e: Exception) {
                Log.w(TAG, "FALLBACK A (wm shell): FAILED", e)
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // FALLBACK B: Settings.Secure write (takes effect after reboot)
        // ═══════════════════════════════════════════════════════════════
        if (!dpiApplied) {
            try {
                val ok = Settings.Secure.putString(resolver, "display_density_forced", dpi.toString())
                Log.d(TAG, "FALLBACK B (Settings.Secure): ${if (ok) "written" else "FAILED"} (reboot needed)")
            } catch (e: Exception) {
                Log.w(TAG, "FALLBACK B (Settings.Secure): FAILED", e)
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // SAMSUNG: Update screen_zoom index (best-effort)
        // Samsung moved screen_zoom to secure settings on some versions,
        // so try Secure first, then System.
        // ═══════════════════════════════════════════════════════════════
        try {
            Settings.Secure.putInt(resolver, "screen_zoom", index)
            Log.d(TAG, "Samsung screen_zoom (Secure): set to $index")
        } catch (e: Exception) {
            try {
                Settings.System.putInt(resolver, "screen_zoom", index)
                Log.d(TAG, "Samsung screen_zoom (System): set to $index")
            } catch (e2: Exception) {
                Log.w(TAG, "Samsung screen_zoom: FAILED on both Secure and System", e2)
            }
        }

        // Samsung broadcast for UI consistency
        try {
            val intent = Intent("com.samsung.android.intent.action.SCREEN_ZOOM_CHANGED")
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Samsung broadcast: FAILED", e)
        }

        Log.d(TAG, "Zoom change complete: DPI=$dpi, Index=$index, dpiApplied=$dpiApplied")
    }
}
