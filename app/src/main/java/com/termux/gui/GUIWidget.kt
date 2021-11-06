package com.termux.gui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

// All the app widget runtime handling is handled by the ConnectionHandler
class GUIWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (appWidgetIds != null && context != null && appWidgetManager != null) {
            for (id in appWidgetIds) {
                appWidgetManager.updateAppWidget(id, defaultViews(context))
            }
        }
    }
    
    companion object {
        fun defaultViews(c: Context): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget_preview)
            
            val i = Intent(Intent.ACTION_MAIN)
            i.component = ComponentName.createRelative("com.termux", ".app.TermuxActivity")
            v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE))
            return v
        }
    }
}