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
            val currentDpi = Settings.Secure.getString(resolver, "display_density_forced")
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
            // A. Update Android standard DPI
            Settings.Secure.putString(resolver, "display_density_forced", dpi.toString())
            
            // B. Update Samsung-specific slider index
            try {
                Settings.System.putInt(resolver, "screen_zoom", index)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set Samsung screen_zoom (requires WRITE_SETTINGS)")
            }

            // C. Force refresh via WindowManager API
            try {
                val serviceManagerClass = Class.forName("android.os.ServiceManager")
                val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
                val windowManagerBinder = getServiceMethod.invoke(null, "window") as IBinder
                
                val iWindowManagerStubClass = Class.forName("android.view.IWindowManager\$Stub")
                val asInterfaceMethod = iWindowManagerStubClass.getMethod("asInterface", IBinder::class.java)
                val iWindowManagerInstance = asInterfaceMethod.invoke(null, windowManagerBinder)
                
                val setForcedDisplayDensityForUserMethod = iWindowManagerInstance.javaClass.getMethod(
                    "setForcedDisplayDensityForUser",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                
                // displayId = 0, density = dpi, userId = -2 (USER_CURRENT)
                setForcedDisplayDensityForUserMethod.invoke(iWindowManagerInstance, 0, dpi, -2)
            } catch (e: Exception) {
                Log.e(TAG, "Reflection API failed", e)
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
