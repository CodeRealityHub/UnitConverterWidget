package com.example.unitwidget.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.unitwidget.core.RateMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RateSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("https://open.er-api.com/v6/latest/INR").build()
            val body = OkHttpClient().newCall(request).execute().use { it.body?.string() ?: error("Empty response") }
            val rates = JSONObject(body).getJSONObject("rates")
            val result = mutableMapOf<String, Double>()
            listOf("USD","EUR","GBP","AED","SGD","JPY","AUD","CAD").forEach { code ->
                val perInr = rates.optDouble(code, Double.NaN)
                if (perInr.isFinite() && perInr > 0) result[code] = 1.0 / perInr
            }
            if (result.isEmpty()) return@withContext Result.retry()
            RateMemory.rates = result
            RateMemory.updatedAt = System.currentTimeMillis()
            val manager = GlanceAppWidgetManager(applicationContext)
            manager.getGlanceIds(UnitConverterWidget::class.java).forEach { UnitConverterWidget().update(applicationContext, it) }
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun schedule(context: Context) {
            val periodic = PeriodicWorkRequestBuilder<RateSyncWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("inr_rate_sync", ExistingPeriodicWorkPolicy.KEEP, periodic)
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RateSyncWorker>().build())
        }
    }
}
