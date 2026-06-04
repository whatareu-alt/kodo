package com.example.aiweathermonitor

import kotlinx.serialization.Serializable

fun Int.getWeatherCodeDescription(): String {
    return when (this) {
        0 -> "Clear Sky"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Fog / Rime Fog"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rainy"
        71, 73, 75 -> "Snowy"
        80, 81, 82 -> "Rain Showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unstable / Varied"
    }
}

@Serializable
data class GeocodingResult(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    val admin1: String? = null
)

/**
 * Factory functions for creating GeocodingResult from API responses
 */
fun createGeocodingResult(
    name: String?,
    latitude: Double?,
    longitude: Double?,
    country: String? = null,
    admin1: String? = null
): GeocodingResult = GeocodingResult(
    name = name ?: "",
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
    country = country,
    admin1 = admin1
)

@Serializable
data class HourForecast(
    val hour: String,
    val temperature: Float,
    val weatherCode: Int
)

@Serializable
data class DayForecast(
    val day: String,
    val tempMin: Float,
    val tempMax: Float,
    val weatherCode: Int
)

@Serializable
data class WeatherState(
    // Geocoding & City states
    val searchQuery: String = "",
    val selectedCity: String = "New York",
    val latitude: Double = 40.7128,
    val longitude: Double = -74.0060,
    val searchResults: List<GeocodingResult> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,

    // Google API Key
    val googleApiKey: String = "",
    val showKeyDialog: Boolean = false,

    // Saved Cities & Unit Configurations
    val savedCities: List<GeocodingResult> = emptyList(),
    val isCelsius: Boolean = true,
    val showSavedCitiesScreen: Boolean = false,

    // Indian API Configuration
    val indianApiKey: String = "",
    val isIndianApiSourceActive: Boolean = false,

    // Live Weather Metrics from API
    val temperature: Float = 22.0f,
    val humidity: Float = 50.0f,
    val pressure: Float = 1013.0f,
    val windSpeed: Float = 12.0f,
    val aqi: Int = 45,
    val uvIndex: Float = 3f,
    val weatherCode: Int = 0,
    val sunrise: String = "6:00 AM",
    val sunset: String = "6:00 PM",
    val sunriseMinutes: Int = 360,
    val sunsetMinutes: Int = 1080,
    
    // Hourly & Daily Forecast Arrays (Apple Weather Style)
    val hourlyForecast: List<HourForecast> = emptyList(),
    val dailyForecast: List<DayForecast> = emptyList(),

    // Hyperlocal Weather Union source indicator
    val isWeatherUnionSource: Boolean = false,

    // Connectivity & Cache metadata
    val isOnline: Boolean = true,
    val lastRefreshedTime: String = "",

    // Severe Weather Alerts
    val alertEvent: String? = null,
    val alertHeadline: String? = null,
    val alertDesc: String? = null,

    // Weather Union AWS station coordinates metadata
    val stationLat: Double = 0.0,
    val stationLon: Double = 0.0
)
