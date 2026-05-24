package com.gemini.zoomwidget

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import java.lang.reflect.Method

object ZoomUtils {
    private const val TAG = "ZoomUtils"

    /**
     * Bypass Android's hidden API restrictions (Android 9+).
     * This allows reflection on internal APIs like IWindowManager.
     * Must be called once before any hidden API reflection.
     */
    private var hiddenApisBypassed = false

    private fun bypassHiddenApis() {
        if (hiddenApisBypassed) return
        try {
            val vmRuntimeClass = Class.forName("dalvik.system.VMRuntime")
            val getRuntimeMethod = vmRuntimeClass.getDeclaredMethod("getRuntime")
            val vmRuntime = getRuntimeMethod.invoke(null)
            val setExemptionsMethod = vmRuntimeClass.getDeclaredMethod(
                "setHiddenApiExemptions",
                Array<String>::class.java
            )
            // "L" exempts all classes (all JNI class names start with "L")
            setExemptionsMethod.invoke(vmRuntime, arrayOf("L") as Any)
            hiddenApisBypassed = true
            Log.d(TAG, "Hidden API bypass successful")
        } catch (e: Exception) {
            Log.w(TAG, "Hidden API bypass failed (may not be needed on this Android version)", e)
        }
    }

    fun toggleZoom(context: Context) {
        val resolver = context.contentResolver

        // 1. Get current state (preferring Samsung's index if available)
        val samsungZoomIndex = try {
            Settings.System.getInt(resolver, "screen_zoom")
        } catch (e: Exception) {
            -1
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
            // Read from Settings.Secure (where WMS persists the value)
            val currentDpi = try {
                Settings.Secure.getString(resolver, "display_density_forced")
            } catch (e: Exception) {
                null
            }
            currentDpi == zoomedInDpi.toString()
        }

        // 4. Set new values
        val targetDpi = if (isCurrentlyZoomedIn) zoomedOutDpi else zoomedInDpi
        val targetIndex = if (isCurrentlyZoomedIn) zoomedOutIndex else zoomedInIndex

        Log.d(TAG, "Toggle: currently=${if (isCurrentlyZoomedIn) "ZoomedIn" else "ZoomedOut"} → target DPI=$targetDpi, index=$targetIndex")
        applySettings(context, targetDpi, targetIndex)
    }

    private fun applySettings(context: Context, dpi: Int, index: Int) {
        val resolver = context.contentResolver
        var dpiApplied = false

        // ═══════════════════════════════════════════════════════════════
        // STRATEGY A: IWindowManager reflection (most reliable from app)
        // This is the SAME call that "wm density" makes internally,
        // but via Binder from our process (which has WRITE_SECURE_SETTINGS)
        // instead of via a shell command (which gets blocked by SELinux).
        // ═══════════════════════════════════════════════════════════════
        try {
            // First, bypass hidden API restrictions (Android 9+)
            bypassHiddenApis()

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
            Log.d(TAG, "Strategy A (IWindowManager reflection): SUCCESS - DPI=$dpi")
        } catch (e: Exception) {
            Log.w(TAG, "Strategy A (IWindowManager reflection): FAILED", e)
        }

        // ═══════════════════════════════════════════════════════════════
        // STRATEGY B: "wm density" via shell (fallback)
        // May be blocked by SELinux on Samsung but worth trying.
        // ═══════════════════════════════════════════════════════════════
        if (!dpiApplied) {
            try {
                val process = Runtime.getRuntime().exec(
                    arrayOf("/system/bin/sh", "-c", "wm density $dpi")
                )
                val exitCode = process.waitFor()
                val stdout = process.inputStream.bufferedReader().readText()
                val stderr = process.errorStream.bufferedReader().readText()

                if (exitCode == 0) {
                    dpiApplied = true
                    Log.d(TAG, "Strategy B (wm density shell): SUCCESS - DPI=$dpi")
                } else {
                    Log.w(TAG, "Strategy B (wm density shell): FAILED exit=$exitCode stdout=$stdout stderr=$stderr")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Strategy B (wm density shell): FAILED", e)
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // STRATEGY C: Write display_density_forced to Settings.Secure
        // On AOSP, WindowManagerService has a ContentObserver on this key
        // that triggers a configuration refresh. This is a last resort
        // since not all OEMs implement this observer.
        // ═══════════════════════════════════════════════════════════════
        if (!dpiApplied) {
            try {
                val success = Settings.Secure.putString(
                    resolver, "display_density_forced", dpi.toString()
                )
                Log.d(TAG, "Strategy C (Settings.Secure write): ${if (success) "written" else "FAILED to write"}")
                if (success) dpiApplied = true
            } catch (e: Exception) {
                Log.w(TAG, "Strategy C (Settings.Secure write): FAILED", e)
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // SAMSUNG: Update screen_zoom index and broadcast
        // This keeps the Samsung Settings UI slider in sync.
        // ═══════════════════════════════════════════════════════════════
        try {
            Settings.System.putInt(resolver, "screen_zoom", index)
            Log.d(TAG, "Samsung screen_zoom index set to $index")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set Samsung screen_zoom index", e)
        }

        // Broadcast Samsung-specific refresh intent
        try {
            val intent = Intent("com.samsung.android.intent.action.SCREEN_ZOOM_CHANGED")
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            context.sendBroadcast(intent)
            Log.d(TAG, "Samsung SCREEN_ZOOM_CHANGED broadcast sent")
        } catch (e: Exception) {
            Log.w(TAG, "Samsung broadcast failed", e)
        }

        Log.d(TAG, "Applied Zoom Change: DPI=$dpi, Index=$index, dpiApplied=$dpiApplied")
    }
}
