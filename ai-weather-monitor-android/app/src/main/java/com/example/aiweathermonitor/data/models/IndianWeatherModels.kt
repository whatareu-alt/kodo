package com.example.aiweathermonitor.data.models

import kotlinx.serialization.Serializable

/**
 * Weather Union (Indian) API response models
 * Separated for better organization and maintainability
 */

@Serializable
data class IndianWeatherData(
    val temperature: Float? = null,
    val feelsLike: Float? = null,
    val humidity: Int? = null,
    val windSpeed: Float? = null,
    val windDirection: Int? = null,
    val pressure: Float? = null,
    val visibility: Int? = null,
    val weatherCondition: String? = null,
    val weatherCode: Int? = null,
    val rainfall: Float? = null,
    val snowfall: Float? = null,
    val cloudCover: Int? = null,
    val uvIndex: Float? = null,
    val aqi: Int? = null
)

@Serializable
data class IndianWeatherResponse(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
    val data: IndianWeatherData? = null,
    val timestamp: Long? = null
)

@Serializable
data class IndianAlertData(
    val eventId: String? = null,
    val eventName: String? = null,
    val description: String? = null,
    val severity: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class IndianAlertsResponse(
    val status: String? = null,
    val alerts: List<IndianAlertData>? = null
)
