package com.example.aiweathermonitor.util

import java.net.URLEncoder
import com.example.aiweathermonitor.config.WeatherApiConfig

private const val PARAM_LATITUDE = "location.latitude"
private const val PARAM_LONGITUDE = "location.longitude"

/**
 * Safe URL builder to prevent injection vulnerabilities and encoding issues.
 * All parameters are properly URL-encoded.
 */
class SafeUrlBuilder(private val baseUrl: String) {
    private val queryParams = mutableMapOf<String, String>()

    /**
     * Adds a query parameter with safe encoding.
     * Skips empty values to avoid malformed URLs.
     */
    fun addParam(key: String, value: String): SafeUrlBuilder {
        if (value.isNotBlank()) {
            queryParams[key] = URLEncoder.encode(value, "UTF-8")
        }
        return this
    }

    /**
     * Adds a query parameter with numeric value.
     */
    fun addParam(key: String, value: Number): SafeUrlBuilder {
        queryParams[key] = value.toString()
        return this
    }

    /**
     * Adds a query parameter with boolean value.
     */
    fun addParam(key: String, value: Boolean): SafeUrlBuilder {
        queryParams[key] = value.toString()
        return this
    }

    /**
     * Builds the final URL with all parameters.
     */
    fun build(): String {
        if (queryParams.isEmpty()) return baseUrl

        return baseUrl + "?" + queryParams
            .entries
            .joinToString("&") { (key, value) -> "$key=$value" }
    }
}

/**
 * Extension functions for safe type conversions
 */
fun Double?.toFloatSafe(): Float = this?.toFloat() ?: 0f
fun Float?.toDoubleSafe(): Double = this?.toDouble() ?: 0.0
fun String?.orEmpty(): String = this ?: ""

/**
 * Safe list access extensions
 */
fun <T> List<T>?.getOrNull(index: Int): T? = this?.getOrNull(index)
fun <T> List<T>?.getOrDefault(index: Int, default: T): T = this?.getOrNull(index) ?: default

/**
 * Helper function to safely build URLs for common weather API calls.
 */
object UrlBuilder {

    /**
     * Builds URL for OpenMeteo geocoding search.
     */
    fun openMeteoSearch(query: String, limit: Int = 10): String {
        return SafeUrlBuilder(WeatherApiConfig.Endpoints.OPEN_METEO_GEOCODING)
            .addParam("name", query)
            .addParam("count", limit)
            .addParam("language", "en")
            .build()
    }

    /**
     * Builds URL for OpenMeteo weather forecast.
     */
    fun openMeteoForecast(
        latitude: Float,
        longitude: Float,
        hourly: Boolean = true,
        daily: Boolean = true
    ): String {
        return SafeUrlBuilder(WeatherApiConfig.Endpoints.OPEN_METEO_FORECAST)
            .addParam("latitude", latitude)
            .addParam("longitude", longitude)
            .apply {
                if (hourly) addParam("hourly", "temperature_2m,weather_code")
                if (daily) addParam("daily", "temperature_2m_max,temperature_2m_min,weather_code")
            }
            .addParam("timezone", "auto")
            .build()
    }

    /**
     * Builds URL for Google current conditions.
     */
    fun googleCurrentConditions(
        latitude: Float,
        longitude: Float,
        apiKey: String
    ): String {
        return SafeUrlBuilder(WeatherApiConfig.Endpoints.GOOGLE_CURRENT_CONDITIONS)
            .addParam("key", apiKey)
            .addParam(PARAM_LATITUDE, latitude)
            .addParam(PARAM_LONGITUDE, longitude)
            .build()
    }

    /**
     * Builds URL for Google forecast (hours).
     */
    fun googleForecastHours(
        latitude: Float,
        longitude: Float,
        apiKey: String,
        hours: Int = 12
    ): String {
        return SafeUrlBuilder(WeatherApiConfig.Endpoints.GOOGLE_FORECAST_HOURS)
            .addParam("key", apiKey)
            .addParam(PARAM_LATITUDE, latitude)
            .addParam(PARAM_LONGITUDE, longitude)
            .addParam("forecast_hours", hours)
            .build()
    }

    /**
     * Builds URL for Google forecast (days).
     */
    fun googleForecastDays(
        latitude: Float,
        longitude: Float,
        apiKey: String,
        days: Int = 14
    ): String {
        return SafeUrlBuilder(WeatherApiConfig.Endpoints.GOOGLE_FORECAST_DAYS)
            .addParam("key", apiKey)
            .addParam(PARAM_LATITUDE, latitude)
            .addParam(PARAM_LONGITUDE, longitude)
            .addParam("forecast_days", days)
            .build()
    }

    /**
     * Builds URL for Google weather alerts.
     */
    fun googleAlerts(
        latitude: Float,
        longitude: Float,
        apiKey: String
    ): String {
        return SafeUrlBuilder(WeatherApiConfig.Endpoints.GOOGLE_PUBLIC_ALERTS)
            .addParam("key", apiKey)
            .addParam(PARAM_LATITUDE, latitude)
            .addParam(PARAM_LONGITUDE, longitude)
            .build()
    }
}
