package com.gemini.zoomwidget

import android.content.Context
import android.os.IBinder
import android.provider.Settings
import android.util.Log

object ZoomUtils {
    private const val TAG = "ZoomUtils"

    fun toggleZoom(context: Context) {
        val contentResolver = context.contentResolver
        val currentDensityStr = Settings.Secure.getString(contentResolver, "display_density_forced")
        
        // 320 is typically "Small/Zoomed Out", 600 is "Large/Zoomed In"
        val zoomedOutDensity = 320
        val zoomedInDensity = 600

        // If current is the zoomedIn value, we go to zoomedOut. Otherwise (default or other), we go to zoomedIn.
        val targetDensity = if (currentDensityStr == zoomedInDensity.toString()) {
            zoomedOutDensity
        } else {
            zoomedInDensity
        }

        setDensity(targetDensity)
    }

    private fun setDensity(density: Int) {
        try {
            // Get the IWindowManager service via ServiceManager
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val windowManagerBinder = getServiceMethod.invoke(null, "window") as IBinder
            
            // Convert the IBinder to IWindowManager
            val iWindowManagerStubClass = Class.forName("android.view.IWindowManager\$Stub")
            val asInterfaceMethod = iWindowManagerStubClass.getMethod("asInterface", IBinder::class.java)
            val iWindowManagerInstance = asInterfaceMethod.invoke(null, windowManagerBinder)
            
            // Find the setForcedDisplayDensityForUser method
            // Signature: setForcedDisplayDensityForUser(int displayId, int density, int userId)
            val setForcedDisplayDensityForUserMethod = iWindowManagerInstance.javaClass.getMethod(
                "setForcedDisplayDensityForUser",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            
            // Invoke: displayId = 0 (main), density = our target, userId = -2 (UserHandle.USER_CURRENT)
            setForcedDisplayDensityForUserMethod.invoke(iWindowManagerInstance, 0, density, -2)
            
            Log.d(TAG, "Successfully pushed density change to $density via WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set density via reflection. Make sure WRITE_SECURE_SETTINGS is granted.", e)
        }
    }
}
