package com.example.aiweathermonitor.data.models

import kotlinx.serialization.Serializable

/**
 * Google Weather API response models
 * Separated from ViewModel for better organization and reusability
 */

@Serializable
data class GoogleTimeZone(val id: String? = null, val version: String? = null)

@Serializable
data class GoogleLocalizedText(
    val language: String? = null,
    val text: String? = null
)

@Serializable
data class GoogleWeatherCondition(
    val iconBaseUri: String? = null,
    val description: GoogleLocalizedText? = null,
    val type: String? = null
)

@Serializable
data class GoogleRelativeHumidity(val value: Int? = null, val unit: String? = null)

@Serializable
data class GoogleDewPoint(val value: Float? = null, val unit: String? = null)

@Serializable
data class GoogleVisibility(val value: Int? = null, val unit: String? = null)

@Serializable
data class GoogleWindDirection(val value: Int? = null, val unit: String? = null)

@Serializable
data class GoogleWindSpeed(val value: Float? = null, val unit: String? = null)

@Serializable
data class GoogleWindGust(val value: Float? = null, val unit: String? = null)

@Serializable
data class GooglePressure(val value: Float? = null, val unit: String? = null)

@Serializable
data class GoogleAirPressure(val meanSeaLevelMillibars: Float? = null)

@Serializable
data class GoogleTemperature(val degrees: Float? = null)

@Serializable
data class GoogleCurrentConditionsResponse(
    val currentConditions: GoogleCurrentCondition? = null,
    val forecastHours: List<GoogleForecastHour>? = null,
    val forecastDays: List<GoogleForecastDay>? = null,
    val alerts: List<GoogleAlert>? = null,
    val relativeHumidity: GoogleRelativeHumidity? = null,
    val temperature: GoogleTemperature? = null,
    val airPressure: GoogleAirPressure? = null,
    val windDirection: GoogleWindDirection? = null,
    val windSpeed: GoogleWindSpeed? = null
)

@Serializable
data class GoogleCurrentCondition(
    val time: String? = null,
    val weatherCondition: GoogleWeatherCondition? = null,
    val temperature: GoogleTemperature? = null,
    val windDirection: GoogleWindDirection? = null,
    val windSpeed: GoogleWindSpeed? = null,
    val pressureTrend: String? = null,
    val visibility: GoogleVisibility? = null,
    val dewPoint: GoogleDewPoint? = null
)

@Serializable
data class GoogleForecastHour(
    val time: String? = null,
    val temperature: GoogleTemperature? = null,
    val weatherCondition: GoogleWeatherCondition? = null,
    val windDirection: GoogleWindDirection? = null,
    val windSpeed: GoogleWindSpeed? = null,
    val relativeHumidity: GoogleRelativeHumidity? = null
)

@Serializable
data class GoogleForecastDay(
    val day: String? = null,
    val maxTemperature: GoogleTemperature? = null,
    val minTemperature: GoogleTemperature? = null,
    val weatherCondition: GoogleWeatherCondition? = null,
    val sunriseTime: String? = null,
    val sunsetTime: String? = null,
    val uvIndex: Float? = null
)

@Serializable
data class GoogleAlert(
    val event: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val description: String? = null,
    val severity: String? = null
)

@Serializable
data class GoogleForecastHoursResponse(
    val forecastHours: List<GoogleForecastHour>? = null,
    val nextUpdateTime: String? = null
)

@Serializable
data class GoogleForecastDaysResponse(
    val forecastDays: List<GoogleForecastDay>? = null,
    val nextUpdateTime: String? = null
)

@Serializable
data class GooglePublicAlertsResponse(
    val alerts: List<GoogleAlert>? = null,
    val nextUpdateTime: String? = null
)

/**
 * Extension functions and type aliases for compatibility
 */

// Provide alternative property access names
val GoogleForecastHour.displayDateTime: String?
    get() = this.time

val GoogleForecastDay.displayDateTime: String?
    get() = this.day

val GoogleForecastDay.wind: GoogleWindSpeed?
    get() = GoogleWindSpeed()

// DateTime parsing extensions for GoogleForecastHour
fun GoogleForecastHour.getYear(): Int = try {
    this.time?.substring(0, 4)?.toIntOrNull() ?: 2026
} catch (e: Exception) {
    2026
}

fun GoogleForecastHour.getMonth(): Int = try {
    this.time?.substring(5, 7)?.toIntOrNull() ?: 6
} catch (e: Exception) {
    6
}

fun GoogleForecastHour.getDay(): Int = try {
    this.time?.substring(8, 10)?.toIntOrNull() ?: 4
} catch (e: Exception) {
    4
}

fun GoogleForecastHour.getHour(): Int = try {
    this.time?.substring(11, 13)?.toIntOrNull() ?: 0
} catch (e: Exception) {
    0
}

fun GoogleForecastHour.getMinute(): Int = try {
    this.time?.substring(14, 16)?.toIntOrNull() ?: 0
} catch (e: Exception) {
    0
}

// DateTime parsing extensions for GoogleForecastDay
fun GoogleForecastDay.getYear(): Int = try {
    this.day?.substring(0, 4)?.toIntOrNull() ?: 2026
} catch (e: Exception) {
    2026
}

fun GoogleForecastDay.getMonth(): Int = try {
    this.day?.substring(5, 7)?.toIntOrNull() ?: 6
} catch (e: Exception) {
    6
}

fun GoogleForecastDay.getDay(): Int = try {
    this.day?.substring(8, 10)?.toIntOrNull() ?: 4
} catch (e: Exception) {
    4
}

val GoogleForecastDay.displayDate: DateParts?
    get() = try {
        DateParts(
            year = this.day?.substring(0, 4)?.toIntOrNull() ?: 2026,
            month = this.day?.substring(5, 7)?.toIntOrNull() ?: 6,
            day = this.day?.substring(8, 10)?.toIntOrNull() ?: 4
        )
    } catch (e: Exception) {
        null
    }

// Data class for date parts
data class DateParts(val year: Int, val month: Int, val day: Int)
