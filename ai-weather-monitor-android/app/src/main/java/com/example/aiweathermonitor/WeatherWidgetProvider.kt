package com.example.aiweathermonitor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.serialization.json.Json

class WeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val sharedPrefs = context.getSharedPreferences(WeatherApiConfig.SharedPrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY, null)
        val jsonParser = Json { ignoreUnknownKeys = true }

        val state = try {
            if (!jsonStr.isNullOrBlank()) {
                jsonParser.decodeFromString(WeatherState.serializer(), jsonStr)
            } else {
                null
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            android.util.Log.w("WeatherWidget", "Cached weather state is corrupted")
            null
        } catch (e: Exception) {
            android.util.Log.e("WeatherWidget", "Failed to load cached weather state: ${e.message}", e)
            null
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)

            if (state != null) {
                val tempStr = if (state.isCelsius) {
                    "${state.temperature.toInt()}°C"
                } else {
                    "${((state.temperature * 9 / 5) + 32).toInt()}°F"
                }
                views.setTextViewText(R.id.widget_temp, tempStr)
                views.setTextViewText(R.id.widget_city, state.selectedCity)
                views.setTextViewText(R.id.widget_description, state.weatherCode.getWeatherCodeDescription())

                val connectionStatus = if (state.isOnline) "" else "Offline"
                views.setTextViewText(R.id.widget_online_status, connectionStatus)
            } else {
                views.setTextViewText(R.id.widget_temp, "--°")
                views.setTextViewText(R.id.widget_city, "Sunnyside")
                views.setTextViewText(R.id.widget_description, "Open app to load weather")
                views.setTextViewText(R.id.widget_online_status, "")
            }

            // Pending intent to open the app on click
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
