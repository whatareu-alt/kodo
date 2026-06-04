package com.example.aiweathermonitor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.serialization.encodeToString
import com.example.aiweathermonitor.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit
import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.util.AppLogger
import com.example.aiweathermonitor.util.ErrorHandler
import com.example.aiweathermonitor.util.UrlBuilder
import com.example.aiweathermonitor.data.models.*

// Use AppLogger for logging errors
private const val TAG = "MainScreenViewModel"
private const val TIME_DEFAULT_SUNRISE = "6:00 AM"
private const val TIME_DEFAULT_SUNSET = "6:00 PM"

private data class GoogleWeatherData(
    val current: GoogleCurrentConditionsResponse,
    val hours: GoogleForecastHoursResponse,
    val days: GoogleForecastDaysResponse,
    val alerts: GooglePublicAlertsResponse?
)

class MainScreenViewModel(
    private val client: OkHttpClient = OkHttpClient(),
    private val jsonParser: Json = Json { ignoreUnknownKeys = true },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private var searchJob: Job? = null
    private var isCacheInitialized = false

    fun initializeCache(context: android.content.Context) {
        if (isCacheInitialized) return
        isCacheInitialized = true

        val connectivityObserver = com.example.aiweathermonitor.NetworkConnectivityObserver(context)
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { online ->
                _weatherState.value = _weatherState.value.copy(isOnline = online)
            }
        }

        val cached = loadStateFromPrefs(context)
        if (cached != null) {
            _weatherState.value = cached
            refreshWeather(context)
        } else {
            fetchWeatherForCoordinates(
                cityName = "New York",
                latitude = 40.7128f,
                longitude = -74.0060f,
                isIndia = false,
                context = context
            )
        }

        viewModelScope.launch {
            _weatherState.collect { state ->
                withContext(ioDispatcher) {
                    saveStateToPrefs(context, state)
                }
            }
        }
    }

    private fun saveStateToPrefs(context: android.content.Context, state: WeatherState) {
        try {
            val sharedPrefs = context.getSharedPreferences(WeatherApiConfig.SharedPrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val jsonStr = jsonParser.encodeToString(WeatherState.serializer(), state)
            sharedPrefs.edit(commit = false) {
                putString(WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY, jsonStr)
            }

            val intent = android.content.Intent(context, WeatherWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(
                    android.content.ComponentName(context, WeatherWidgetProvider::class.java)
                )
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent) // NOSONAR
        } catch (e: Exception) {
            AppLogger.error("Failed to save weather state or broadcast widget update", TAG, e)
        }
    }

    private fun loadStateFromPrefs(context: android.content.Context): WeatherState? {
        return try {
            val sharedPrefs = context.getSharedPreferences(WeatherApiConfig.SharedPrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val jsonStr = sharedPrefs.getString("cached_weather_state", null)
            if (!jsonStr.isNullOrBlank()) {
                val decoded = jsonParser.decodeFromString(WeatherState.serializer(), jsonStr)
                decoded.copy(
                    isLoading = false,
                    isSearching = false,
                    searchQuery = "",
                    searchResults = emptyList(),
                    showKeyDialog = false,
                    showSavedCitiesScreen = false,
                    errorMessage = null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun updateWeatherState(newState: WeatherState) {
        _weatherState.value = newState
    }

    fun updateSearchQuery(query: String) {
        _weatherState.value = _weatherState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.trim().length >= WeatherApiConfig.SEARCH_MIN_CHARS) {
            searchJob = viewModelScope.launch {
                delay(WeatherApiConfig.SEARCH_DEBOUNCE_MS.toLong())
                searchCities(query.trim())
            }
        } else {
            _weatherState.value = _weatherState.value.copy(searchResults = emptyList())
        }
    }

    private fun searchCities(query: String) {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isSearching = true)
            try {
                val results = withContext(ioDispatcher) {
                    val url = "https://geocoding-api.open-meteo.com/v1/search?name=$query&count=10"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Search failed: ${response.code}")
                        val bodyString = response.body?.string() ?: throw Exception("Empty search response")
                        val decoded = jsonParser.decodeFromString<OpenMeteoGeocodingResponse>(bodyString)
                        val apiResults = decoded.results?.map {
                            createGeocodingResult(
                                name = it.name,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                country = it.country,
                                admin1 = it.admin1
                            )
                        } ?: emptyList()
                        apiResults
                    }
                }
                _weatherState.value = _weatherState.value.copy(
                    searchResults = results,
                    isSearching = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val friendlyError = if (ErrorHandler.isNetworkError(e)) {
                    WeatherApiConfig.ErrorMessages.ERROR_NO_INTERNET
                } else {
                    ErrorHandler.getFriendlyErrorMessage(e)
                }
                AppLogger.error("Failed to search cities", TAG, e)
                _weatherState.value = _weatherState.value.copy(
                    isSearching = false,
                    errorMessage = friendlyError
                )
            }
        }
    }

    fun selectCity(result: GeocodingResult, context: android.content.Context? = null) {
        val isIndianApiCity = result.country == "India (IMD)"
        if (isIndianApiCity) {
            val cityId = result.admin1?.substringAfter("Station ID: ") ?: ""
            fetchWeatherByIndianCityId(result.name, cityId, context)
        } else {
            val isIndia = result.country?.contains("India", ignoreCase = true) == true
            _weatherState.value = _weatherState.value.copy(
                searchQuery = "",
                searchResults = emptyList(),
                selectedCity = result.name,
                latitude = result.latitude,
                longitude = result.longitude,
                isWeatherUnionSource = isIndia
            )
            fetchWeatherForCoordinates(result.name, result.latitude.toFloat(), result.longitude.toFloat(), isIndia, context)
        }
    }

    fun refreshWeather(context: android.content.Context? = null) {
        val current = _weatherState.value
        val isIndia = current.isWeatherUnionSource
        fetchWeatherForCoordinates(current.selectedCity, current.latitude.toFloat(), current.longitude.toFloat(), isIndia, context)
    }

    private fun parseHour(timeStr: String): String {
        val sepIndex = timeStr.indexOf('T').let { if (it == -1) timeStr.indexOf(' ') else it }
        if (sepIndex != -1 && sepIndex + 6 <= timeStr.length) {
            val rawHour = timeStr.substring(sepIndex + 1, sepIndex + 6)
            val parts = rawHour.split(":")
            if (parts.size == 2) {
                val hourInt = parts[0].toIntOrNull() ?: return rawHour
                val ampm = if (hourInt >= 12) "PM" else "AM"
                val displayHour = when {
                    hourInt == 0 -> 12
                    hourInt > 12 -> hourInt - 12
                    else -> hourInt
                }
                return "$displayHour $ampm"
            }
            return rawHour
        }
        return timeStr
    }

    private fun parseDay(dateStr: String, index: Int): String {
        if (index == 0) return "Today"
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val fullDay = dayFormat.format(date)
            return when (fullDay) {
                "Monday" -> "Mon"
                "Tuesday" -> "Tue"
                "Wednesday" -> "Wed"
                "Thursday" -> "Thu"
                "Friday" -> "Fri"
                "Saturday" -> "Sat"
                "Sunday" -> "Sun"
                else -> fullDay.substring(0, 3)
            }
        } catch (e: Exception) {
            return dateStr
        }
    }

    private fun mapGoogleConditionToWmo(type: String?): Int {
        return when (type) {
            "CLEAR", "MOSTLY_CLEAR" -> 0
            "PARTLY_CLOUDY", "MOSTLY_CLOUDY" -> 2
            "CLOUDY" -> 3
            "WINDY" -> 2
            "WIND_AND_RAIN", "LIGHT_RAIN_SHOWERS", "CHANCE_OF_SHOWERS", "SCATTERED_SHOWERS" -> 80
            "RAIN_SHOWERS", "HEAVY_RAIN_SHOWERS" -> 82
            "LIGHT_RAIN", "LIGHT_TO_MODERATE_RAIN" -> 61
            "RAIN", "MODERATE_TO_HEAVY_RAIN", "RAIN_PERIODICALLY_HEAVY" -> 63
            "HEAVY_RAIN" -> 65
            "LIGHT_SNOW_SHOWERS", "CHANCE_OF_SNOW_SHOWERS", "SCATTERED_SNOW_SHOWERS", "SNOW_SHOWERS" -> 81
            "HEAVY_SNOW_SHOWERS" -> 81
            "LIGHT_SNOW", "LIGHT_TO_MODERATE_SNOW" -> 71
            "SNOW", "MODERATE_TO_HEAVY_SNOW", "SNOW_PERIODICALLY_HEAVY" -> 73
            "HEAVY_SNOW", "SNOWSTORM", "HEAVY_SNOW_STORM", "BLOWING_SNOW" -> 75
            "RAIN_AND_SNOW" -> 73
            "HAIL", "HAIL_SHOWERS" -> 82
            "THUNDERSTORM", "THUNDERSHOWER", "LIGHT_THUNDERSTORM_RAIN", "SCATTERED_THUNDERSTORMS", "HEAVY_THUNDERSTORM" -> 95
            else -> 0
        }
    }

    private fun parseFormattedTimeToMins(timeStr: String): Int {
        try {
            val cleaned = timeStr.trim().uppercase()
            val parts = cleaned.substringBefore(" ").split(":")
            if (parts.size == 2) {
                var h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                val isPm = cleaned.endsWith("PM")
                if (isPm && h < 12) {
                    h += 12
                } else if (!isPm && h == 12) {
                    h = 0
                }
                return h * 60 + m
            }
        } catch (e: Exception) {
            // Fallback
        }
        return 6 * 60
    }

    private suspend fun fetchOpenMeteoWeather(
        latitude: Float,
        longitude: Float
    ): OpenMeteoResponse = withContext(ioDispatcher) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code&hourly=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code,sunrise,sunset&timezone=auto"
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed: ${response.code}")
            val bodyString = response.body?.string() ?: throw Exception("Empty")
            jsonParser.decodeFromString<OpenMeteoResponse>(bodyString)
        }
    }

    private suspend fun fetchGoogleWeatherData(
        apiKey: String,
        latitude: Float,
        longitude: Float
    ): GoogleWeatherData = coroutineScope {
        val currentDeferred = async(ioDispatcher) {
            val url = "https://weather.googleapis.com/v1/currentConditions:lookup?key=$apiKey&location.latitude=$latitude&location.longitude=$longitude"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    throw Exception("Invalid Google API key. Please check it in Settings.")
                }
                if (!response.isSuccessful) throw Exception("Google current weather API failed: ${response.code}")
                val bodyString = response.body?.string() ?: throw Exception("Empty current weather data")
                jsonParser.decodeFromString<GoogleCurrentConditionsResponse>(bodyString)
            }
        }
        
        val hoursDeferred = async(ioDispatcher) {
            val url = "https://weather.googleapis.com/v1/forecast/hours:lookup?key=$apiKey&location.latitude=$latitude&location.longitude=$longitude&unitsSystem=METRIC"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Google hourly forecast API failed: ${response.code}")
                val bodyString = response.body?.string() ?: throw Exception("Empty hourly forecast data")
                jsonParser.decodeFromString<GoogleForecastHoursResponse>(bodyString)
            }
        }
        
        val daysDeferred = async(ioDispatcher) {
            val url = "https://weather.googleapis.com/v1/forecast/days:lookup?key=$apiKey&location.latitude=$latitude&location.longitude=$longitude&unitsSystem=METRIC"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Google daily forecast API failed: ${response.code}")
                val bodyString = response.body?.string() ?: throw Exception("Empty daily forecast data")
                jsonParser.decodeFromString<GoogleForecastDaysResponse>(bodyString)
            }
        }
        
        val alertsDeferred = async(ioDispatcher) {
            try {
                val url = "https://weather.googleapis.com/v1/publicAlerts:lookup?key=$apiKey&location.latitude=$latitude&location.longitude=$longitude"
                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        if (bodyString.isNotBlank()) {
                            jsonParser.decodeFromString<GooglePublicAlertsResponse>(bodyString)
                        } else null
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }

        GoogleWeatherData(
            current = currentDeferred.await(),
            hours = hoursDeferred.await(),
            days = daysDeferred.await(),
            alerts = alertsDeferred.await()
        )
    }

    private fun mapGoogleResponseToState(
        currentState: WeatherState,
        googleData: GoogleWeatherData,
        cityName: String,
        latitude: Float,
        longitude: Float,
        isIndia: Boolean,
        context: android.content.Context?
    ): WeatherState {
        val current = googleData.current
        val hoursResp = googleData.hours
        val daysResp = googleData.days
        val alertsResp = googleData.alerts

        val tempVal = current.temperature?.degrees ?: 0f
        val humidityVal = current.relativeHumidity?.value?.toFloat() ?: 0f
        val pressureVal = current.airPressure?.meanSeaLevelMillibars ?: 1013.0f
        val windSpeedVal = current.windSpeed?.value ?: 0f
        val uvVal = daysResp.forecastDays?.firstOrNull()?.uvIndex ?: 0f
        val mappedCode = mapGoogleConditionToWmo(current.currentConditions?.weatherCondition?.type)

        val hourlyList = mutableListOf<HourForecast>()
        val allHours = hoursResp.forecastHours ?: emptyList()
        
        val nowHourStr = SimpleDateFormat("yyyy-MM-dd HH", Locale.getDefault()).format(Date())
        var startIndex = allHours.indexOfFirst { h ->
            val timeStr = String.format(Locale.US, "%04d-%02d-%02d %02d",
                h.getYear(),
                h.getMonth(),
                h.getDay(),
                h.getHour()
            )
            timeStr.startsWith(nowHourStr)
        }
        if (startIndex == -1) startIndex = 0

        for (i in startIndex until Math.min(startIndex + 12, allHours.size)) {
            val h = allHours[i]
            val parsedHour = if (i == startIndex) "Now" else {
                val timeStr = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                    h.getYear(),
                    h.getMonth(),
                    h.getDay(),
                    h.getHour(),
                    h.getMinute()
                )
                parseHour(timeStr)
            }
            hourlyList.add(
                HourForecast(
                    hour = parsedHour,
                    temperature = h.temperature?.degrees ?: 0f,
                    weatherCode = mapGoogleConditionToWmo(h.weatherCondition?.type)
                )
            )
        }

        val dailyList = mutableListOf<DayForecast>()
        var parsedSunrise = TIME_DEFAULT_SUNRISE
        var parsedSunset = TIME_DEFAULT_SUNSET
        var parsedSunriseMinutes = 360
        var parsedSunsetMinutes = 1080

        val daysList = daysResp.forecastDays ?: emptyList()
        if (daysList.isNotEmpty()) {
            val todayForecast = daysList.getOrNull(0)
            if (todayForecast != null) {
                val sunriseIso = todayForecast.sunriseTime ?: ""
                val sunsetIso = todayForecast.sunsetTime ?: ""
                parsedSunrise = if (sunriseIso.isNotBlank()) parseIsoTime(sunriseIso) else TIME_DEFAULT_SUNRISE
                parsedSunset = if (sunsetIso.isNotBlank()) parseIsoTime(sunsetIso) else TIME_DEFAULT_SUNSET
                parsedSunriseMinutes = parseFormattedTimeToMins(parsedSunrise)
                parsedSunsetMinutes = parseFormattedTimeToMins(parsedSunset)
            }

            for (i in daysList.indices) {
                val d = daysList.getOrNull(i) ?: continue
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d",
                    d.displayDate?.year ?: 2026,
                    d.displayDate?.month ?: 6,
                    d.displayDate?.day ?: 4
                )
                val wCode = mapGoogleConditionToWmo(d.weatherCondition?.type)
                dailyList.add(
                    DayForecast(
                        day = parseDay(dateStr, i),
                        tempMin = d.minTemperature?.degrees ?: 20f,
                        tempMax = d.maxTemperature?.degrees ?: 30f,
                        weatherCode = wCode
                    )
                )
            }
        }

        val firstAlert = alertsResp?.alerts?.firstOrNull()

        if (context != null) {
            val alertMessage = when {
                uvVal >= 8 -> "🧴 Extreme UV Index warning in $cityName! UV index is $uvVal."
                mappedCode in listOf(95, 96, 99) -> "⛈️ Severe Thunderstorm warning active in $cityName!"
                else -> null
            }
            if (alertMessage != null) {
                triggerLocalNotification(context, "Sunnyside Alert: $cityName", alertMessage)
            }
        }

        return currentState.copy(
            temperature = tempVal,
            humidity = humidityVal,
            pressure = pressureVal,
            windSpeed = windSpeedVal,
            weatherCode = mappedCode,
            aqi = 0,
            uvIndex = uvVal,
            sunrise = parsedSunrise,
            sunset = parsedSunset,
            sunriseMinutes = parsedSunriseMinutes,
            sunsetMinutes = parsedSunsetMinutes,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            isWeatherUnionSource = isIndia,
            isIndianApiSourceActive = false,
            isLoading = false,
            errorMessage = null,
            alertEvent = firstAlert?.event,
            alertHeadline = firstAlert?.event,
            alertDesc = firstAlert?.description,
            stationLat = latitude.toDouble(),
            stationLon = longitude.toDouble(),
            lastRefreshedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        )
    }

    fun fetchWeatherForCoordinates(cityName: String, latitude: Float, longitude: Float, isIndia: Boolean, context: android.content.Context? = null) {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true)
            try {
                var indianApiSucceeded = false
                if (isIndia) {
                    try {
                        val data = fetchOpenMeteoWeather(latitude, longitude)
                        val tempState = mapOpenMeteoToState(_weatherState.value, data, cityName)
                        _weatherState.value = tempState.copy(
                            isIndianApiSourceActive = true,
                            isLoading = false,
                            errorMessage = null
                        )
                        indianApiSucceeded = true
                    } catch (e: Exception) {
                        // Fail silently, fall back to Google Weather API
                    }
                }

                if (!indianApiSucceeded) {
                    val apiKey = _weatherState.value.googleApiKey
                    if (apiKey.isBlank()) {
                        throw Exception("Google API key is missing. Please configure it in Settings.")
                    }
                    
                    val googleData = fetchGoogleWeatherData(apiKey, latitude, longitude)
                    _weatherState.value = mapGoogleResponseToState(
                        _weatherState.value,
                        googleData,
                        cityName,
                        latitude,
                        longitude,
                        isIndia,
                        context
                    )
                }

            } catch (e: Exception) {
                val friendlyError = ErrorHandler.getFriendlyErrorMessage(e)
                AppLogger.error("Failed to fetch weather", TAG, e)
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    errorMessage = friendlyError
                )
            }
        }
    }

    private suspend fun geocodeCoordinates(latitude: Float, longitude: Float, context: android.content.Context): String? = withContext(ioDispatcher) {
        try {
            val geocoder = android.location.Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude.toDouble(), longitude.toDouble(), 1)?.toList() ?: emptyList()
            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun fetchWeatherForCurrentLocation(context: android.content.Context) {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true, errorMessage = null)
            try {
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (!hasFine && !hasCoarse) {
                    _weatherState.value = _weatherState.value.copy(
                        isLoading = false,
                        errorMessage = "Location permission not granted."
                    )
                    return@launch
                }

                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: android.location.Location? ->
                    if (loc != null) {
                        val lat = loc.latitude
                        val lon = loc.longitude
                        viewModelScope.launch {
                            val cityName = geocodeCoordinates(lat.toFloat(), lon.toFloat(), context) ?: "Current Location"
                            _weatherState.value = _weatherState.value.copy(
                                selectedCity = cityName,
                                latitude = lat,
                                longitude = lon,
                                isWeatherUnionSource = false
                            )
                            fetchWeatherForCoordinates(cityName, lat.toFloat(), lon.toFloat(), false)
                        }
                    } else {
                        requestFreshLocation(fusedLocationClient, context)
                    }
                }.addOnFailureListener {
                    requestFreshLocation(fusedLocationClient, context)
                }
            } catch (e: SecurityException) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    errorMessage = "Security error accessing location: ${e.message}"
                )
            } catch (e: Exception) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to retrieve location: ${e.message}"
                )
            }
        }
    }

    private fun requestFreshLocation(
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
        context: android.content.Context
    ) {
        try {
            val priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
                .addOnSuccessListener { loc: android.location.Location? ->
                    if (loc != null) {
                        val lat = loc.latitude
                        val lon = loc.longitude
                        viewModelScope.launch {
                            val cityName = geocodeCoordinates(lat.toFloat(), lon.toFloat(), context) ?: "Current Location"
                            _weatherState.value = _weatherState.value.copy(
                                selectedCity = cityName,
                                latitude = lat,
                                longitude = lon,
                                isWeatherUnionSource = false
                            )
                            fetchWeatherForCoordinates(cityName, lat.toFloat(), lon.toFloat(), false)
                        }
                    } else {
                        _weatherState.value = _weatherState.value.copy(
                            isLoading = false,
                            errorMessage = "Location request timed out. Using default/last city."
                        )
                    }
                }
                .addOnFailureListener { e ->
                    _weatherState.value = _weatherState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to fetch current location: ${e.message}"
                    )
                }
        } catch (e: SecurityException) {
            _weatherState.value = _weatherState.value.copy(
                isLoading = false,
                errorMessage = "Security error: ${e.message}"
            )
        }
    }

    fun saveCurrentCity() {
        val current = _weatherState.value
        val exists = current.savedCities.any { it.name.equals(current.selectedCity, ignoreCase = true) }
        if (!exists) {
            val newCity = GeocodingResult(
                name = current.selectedCity,
                latitude = current.latitude,
                longitude = current.longitude,
                country = if (current.isWeatherUnionSource) "India" else null
            )
            _weatherState.value = current.copy(
                savedCities = current.savedCities + newCity
            )
        }
    }

    fun removeCityFromFavorites(city: GeocodingResult) {
        val current = _weatherState.value
        _weatherState.value = current.copy(
            savedCities = current.savedCities.filterNot { it.name.equals(city.name, ignoreCase = true) }
        )
    }

    fun toggleCelsius() {
        val current = _weatherState.value
        _weatherState.value = current.copy(
            isCelsius = !current.isCelsius
        )
    }

    private fun triggerLocalNotification(context: android.content.Context, title: String, content: String) {
        try {
            val channelId = "sunnyside_warnings"
            val channelName = "Sunnyside Weather Warnings"
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    channelName,
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for severe weather, high UV, or dangerous air quality"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun fetchWeatherByIndianCityId(cityName: String, cityId: String, context: android.content.Context? = null) {
        fallbackToWeatherApiByName(cityName, context)
    }

    private fun fallbackToWeatherApiByName(cityName: String, context: android.content.Context?) {
        viewModelScope.launch {
            try {
                val results = withContext(ioDispatcher) {
                    val url = "https://geocoding-api.open-meteo.com/v1/search?name=${cityName.replace(" ", "%20")}&count=1"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Fallback search failed")
                        val body = response.body?.string() ?: ""
                        val decoded = jsonParser.decodeFromString<OpenMeteoGeocodingResponse>(body)
                        decoded.results ?: emptyList()
                    }
                }
                val first = results.firstOrNull()
                if (first != null) {
                    val lat = first.latitude ?: 0.0
                    val lon = first.longitude ?: 0.0
                    _weatherState.value = _weatherState.value.copy(
                        latitude = lat,
                        longitude = lon,
                        isWeatherUnionSource = true,
                        isIndianApiSourceActive = true
                    )
                    fetchWeatherForCoordinates(cityName, lat.toFloat(), lon.toFloat(), true, context)
                } else {
                    throw Exception("City not found in fallback search")
                }
            } catch (e: Exception) {
                val friendlyError = ErrorHandler.getFriendlyErrorMessage(e)
                AppLogger.error("Geocoding and fallback failed", TAG, e)
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    errorMessage = friendlyError
                )
            }
        }
    }

    private fun parseIsoTime(isoTimeStr: String): String {
        try {
            val datePart = isoTimeStr.substringAfter('T')
            val parts = datePart.split(":")
            if (parts.size >= 2) {
                val h = parts[0].toIntOrNull() ?: 6
                val m = parts[1].toIntOrNull() ?: 0
                val ampm = if (h >= 12) "PM" else "AM"
                val displayHour = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, m, ampm)
            }
        } catch (e: Exception) {
            // Fallback
        }
        return TIME_DEFAULT_SUNRISE
    }

    private fun mapOpenMeteoToState(
        currentState: WeatherState,
        data: OpenMeteoResponse,
        cityName: String
    ): WeatherState {
        val current = data.current ?: throw Exception("Current block missing in Open-Meteo response")

        val hourlyList = mutableListOf<HourForecast>()
        val hourlyData = data.hourly
        if (hourlyData != null) {
            val timeList = hourlyData.time ?: emptyList()
            if (timeList.isNotEmpty()) {
                val nowHourStr = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.getDefault()).format(Date())
                var startIndex = timeList.indexOfFirst { it.startsWith(nowHourStr) }
                if (startIndex == -1) startIndex = 0

                for (i in startIndex until Math.min(startIndex + 12, timeList.size)) {
                    val parsedHour = if (i == startIndex) "Now" else parseHour(timeList[i].replace('T', ' '))
                    hourlyList.add(
                        HourForecast(
                            hour = parsedHour,
                            temperature = hourlyData.temperature2m?.getOrElse(i) { 20f } ?: 20f,
                            weatherCode = hourlyData.weatherCode?.getOrElse(i) { 0 } ?: 0
                        )
                    )
                }
            }
        }

        val dailyList = mutableListOf<DayForecast>()
        val dailyData = data.daily
        var parsedSunrise = TIME_DEFAULT_SUNRISE
        var parsedSunset = TIME_DEFAULT_SUNSET
        var parsedSunriseMinutes = 360
        var parsedSunsetMinutes = 1080

        if (dailyData != null) {
            val dayTimeList = dailyData.time ?: emptyList()
            if (dayTimeList.isNotEmpty()) {
                val sunriseIso = dailyData.sunrise?.firstOrNull() ?: ""
                val sunsetIso = dailyData.sunset?.firstOrNull() ?: ""
                parsedSunrise = parseIsoTime(sunriseIso)
                parsedSunset = parseIsoTime(sunsetIso)
                parsedSunriseMinutes = parseFormattedTimeToMins(parsedSunrise)
                parsedSunsetMinutes = parseFormattedTimeToMins(parsedSunset)

                for (i in dayTimeList.indices) {
                    dailyList.add(
                        DayForecast(
                            day = parseDay(dayTimeList[i], i),
                            tempMin = dailyData.temperature2mMin?.getOrElse(i) { 20f } ?: 20f,
                            tempMax = dailyData.temperature2mMax?.getOrElse(i) { 30f } ?: 30f,
                            weatherCode = dailyData.weatherCode?.getOrElse(i) { 0 } ?: 0
                        )
                    )
                }
            }
        }

        return currentState.copy(
            selectedCity = cityName,
            temperature = current.temperature2m ?: 0f,
            humidity = current.relativeHumidity2m ?: 0f,
            pressure = current.surfacePressure ?: 1013f,
            windSpeed = current.windSpeed10m ?: 0f,
            sunrise = parsedSunrise,
            sunset = parsedSunset,
            sunriseMinutes = parsedSunriseMinutes,
            sunsetMinutes = parsedSunsetMinutes,
            dailyForecast = dailyList,
            hourlyForecast = hourlyList,
            weatherCode = current.weatherCode ?: 0,
            isWeatherUnionSource = true,
            isIndianApiSourceActive = true,
            stationLat = data.latitude ?: 0.0,
            stationLon = data.longitude ?: 0.0,
            lastRefreshedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        )
    }
}
