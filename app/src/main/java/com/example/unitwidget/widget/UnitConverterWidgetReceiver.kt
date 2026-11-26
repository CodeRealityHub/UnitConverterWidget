package com.example.unitwidget.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class UnitConverterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UnitConverterWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RateSyncWorker.schedule(context)
    }
}
