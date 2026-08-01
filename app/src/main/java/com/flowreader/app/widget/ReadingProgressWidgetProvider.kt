package com.flowreader.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flowreader.app.R
import com.flowreader.app.data.repository.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReadingProgressWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.dataStore.data.first()
                val title = prefs[stringPreferencesKey("widget_book_title")] ?: "尚未开始阅读"
                val progress = prefs[intPreferencesKey("widget_progress_percent")] ?: 0

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.widget_reading_progress).apply {
                        setTextViewText(R.id.widgetBookTitle, title)
                        setTextViewText(R.id.widgetProgressText, "$progress%")
                        setProgressBar(R.id.widgetProgressBar, 100, progress, false)
                    }
                    manager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
