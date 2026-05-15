package com.hjw.qbremote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TorrentWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        private var cachedDownloadSpeed: Long = 0
        private var cachedUploadSpeed: Long = 0
        private var cachedTorrentCount: Int = 0

        fun updateData(downloadSpeed: Long, uploadSpeed: Long, torrentCount: Int) {
            cachedDownloadSpeed = downloadSpeed
            cachedUploadSpeed = uploadSpeed
            cachedTorrentCount = torrentCount
        }

        fun refreshWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, TorrentWidgetProvider::class.java)
            )
            for (widgetId in widgetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.torrent_widget)

            views.setTextViewText(
                R.id.widget_speed,
                "↓ ${formatSpeed(cachedDownloadSpeed)}  ↑ ${formatSpeed(cachedUploadSpeed)}"
            )
            views.setTextViewText(R.id.widget_count, cachedTorrentCount.toString())

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0L) return "0 KB/s"
            val kb = bytesPerSec / 1024L
            return if (kb >= 1024 * 1024) {
                val gb = kb / (1024 * 1024)
                "${gb} GB/s"
            } else if (kb >= 1024) {
                "${kb / 1024}.${(kb % 1024) * 10 / 1024} MB/s"
            } else {
                "$kb KB/s"
            }
        }
    }
}
