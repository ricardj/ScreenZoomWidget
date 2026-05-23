package com.gemini.zoomwidget

import android.content.Context
import android.provider.Settings
import android.util.Log

object ZoomUtils {
    private const val TAG = "ZoomUtils"

    fun toggleZoom(context: Context) {
        val resolver = context.contentResolver
        
        // 1. Get current state (preferring Samsung's index if available)
        val currentDensityStr = Settings.Secure.getString(resolver, "display_density_forced")
        val samsungZoomIndex = try {
            Settings.System.getInt(resolver, "screen_zoom")
        } catch (e: Exception) {
            -1
        }

        // 2. Define toggle targets
        // DPI: 320 (Min) <-> 600 (Max)
        // Samsung Index: 0 (Min) <-> 4 (Max)
        val zoomedOutDpi = "320"
        val zoomedInDpi = "600"
        val zoomedOutIndex = 0
        val zoomedInIndex = 4

        // 3. Determine if we are currently "Zoomed In"
        val isCurrentlyZoomedIn = if (samsungZoomIndex != -1) {
            samsungZoomIndex >= 3 // Index 3 or 4 is considered zoomed in
        } else {
            currentDensityStr == zoomedInDpi
        }

        // 4. Set new values
        val targetDpi = if (isCurrentlyZoomedIn) zoomedOutDpi else zoomedInDpi
        val targetIndex = if (isCurrentlyZoomedIn) zoomedOutIndex else zoomedInIndex

        try {
            // Standard Android DPI change
            Settings.Secure.putString(resolver, "display_density_forced", targetDpi)
            
            // Samsung-specific slider change
            try {
                Settings.System.putInt(resolver, "screen_zoom", targetIndex)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set Samsung screen_zoom index (requires WRITE_SETTINGS)")
            }
            
            Log.d(TAG, "Toggled zoom to DPI: $targetDpi, Samsung Index: $targetIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle zoom settings", e)
        }
    }
}
