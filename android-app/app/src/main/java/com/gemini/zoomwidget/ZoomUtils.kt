package com.gemini.zoomwidget

import android.content.Context
import android.content.Intent
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
            // Check Settings.Global (where Android actually stores the override)
            val currentDpi = try {
                Settings.Global.getString(resolver, "display_density_forced")
            } catch (e: Exception) {
                // Fallback to Secure if Global fails
                Settings.Secure.getString(resolver, "display_density_forced")
            }
            currentDpi == zoomedInDpi.toString()
        }

        // 4. Set new values
        val targetDpi = if (isCurrentlyZoomedIn) zoomedOutDpi else zoomedInDpi
        val targetIndex = if (isCurrentlyZoomedIn) zoomedOutIndex else zoomedInIndex

        applySettings(context, targetDpi, targetIndex)
    }

    private fun applySettings(context: Context, dpi: Int, index: Int) {
        val resolver = context.contentResolver
        try {
            // A. Apply DPI via "wm density" command (most reliable method)
            // This is equivalent to "adb shell wm density <dpi>" and works with
            // WRITE_SECURE_SETTINGS permission. It triggers an immediate screen refresh.
            try {
                val process = Runtime.getRuntime().exec(arrayOf("wm", "density", dpi.toString()))
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    Log.d(TAG, "wm density $dpi applied successfully")
                } else {
                    val errorOutput = process.errorStream.bufferedReader().readText()
                    Log.e(TAG, "wm density failed (exit=$exitCode): $errorOutput")
                }
            } catch (e: Exception) {
                Log.e(TAG, "wm density command failed", e)
            }

            // B. Update Settings.Global for persistence (so the value survives reboots
            // and is consistent with what wm density sets)
            try {
                Settings.Global.putString(resolver, "display_density_forced", dpi.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Could not write display_density_forced to Settings.Global", e)
            }

            // C. Update Samsung-specific slider index
            try {
                Settings.System.putInt(resolver, "screen_zoom", index)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set Samsung screen_zoom (requires WRITE_SETTINGS)")
            }

            // D. Broadcast Samsung-specific refresh intent
            val intent = Intent("com.samsung.android.intent.action.SCREEN_ZOOM_CHANGED")
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            context.sendBroadcast(intent)
            
            Log.d(TAG, "Applied Zoom Change: DPI=$dpi, Index=$index")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply zoom settings", e)
        }
    }
}
