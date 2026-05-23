package com.gemini.zoomwidget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.RemoteViews
import com.gemini.zoomwidget.R

class ZoomWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_ZOOM) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, open the app
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(launchIntent)
            } else {
                // Permission granted, toggle zoom
                val contentResolver = context.contentResolver
                val currentDensity = Settings.Secure.getString(contentResolver, "display_density_forced")
                
                // 320 is typically "Small/Zoomed Out", 600 is "Large/Zoomed In"
                val zoomedOutDensity = "320"
                val zoomedInDensity = "600"

                val newDensity = if (currentDensity == zoomedInDensity) {
                    zoomedOutDensity
                } else {
                    zoomedInDensity
                }

                Settings.Secure.putString(contentResolver, "display_density_forced", newDensity)

                // Update the widget to reflect the change
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisAppWidget = ComponentName(context, ZoomWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE_ZOOM = "com.gemini.zoomwidget.ACTION_TOGGLE_ZOOM"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.zoom_widget)

            val intent = Intent(context, ZoomWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_ZOOM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.zoom_widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
