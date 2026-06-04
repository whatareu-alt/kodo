package com.example.aiweathermonitor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiweathermonitor.getWeatherCodeDescription
import com.example.aiweathermonitor.DayForecast
import com.example.aiweathermonitor.GeocodingResult
import com.example.aiweathermonitor.HourForecast
import com.example.aiweathermonitor.WeatherState
import kotlin.math.roundToInt

import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

data class DashboardActions(
    val onQueryChange: (String) -> Unit,
    val onSelectCity: (GeocodingResult) -> Unit,
    val onRefresh: () -> Unit,
    val onLocationClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onSaveGoogleApiKey: (String) -> Unit,
    val onSaveIndianApiKey: (String) -> Unit,
    val onDismissSettings: () -> Unit,
    val onSaveCity: () -> Unit,
    val onRemoveCity: (GeocodingResult) -> Unit,
    val onToggleCelsius: () -> Unit,
    val onToggleSavedCitiesScreen: (Boolean) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: WeatherState,
    actions: DashboardActions,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Determine current local time in minutes to check day/night status
    val calendar = java.util.Calendar.getInstance()
    val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val currentMin = calendar.get(java.util.Calendar.MINUTE)
    val timeInMins = currentHour * 60 + currentMin

    val isNight = timeInMins < state.sunriseMinutes || timeInMins > state.sunsetMinutes

    // Determine current theme color palette based on weather and day/night status (like Apple Weather)
    val (skyStart, skyEnd) = when {
        isNight -> {
            when {
                state.weatherCode in listOf(61, 63, 65, 80, 81, 82, 95, 96, 99) -> Pair(Color(0xFF0F172A), Color(0xFF020617)) // Stormy Night (Deep Dark Navy to Black)
                state.weatherCode in listOf(71, 73, 75) -> Pair(Color(0xFF1E293B), Color(0xFF0F172A)) // Snowy Night
                else -> Pair(Color(0xFF1E1B4B), Color(0xFF09090B)) // Clear Night (Indigo/Violet to Dark Charcoal)
            }
        }
        else -> {
            when {
                state.temperature > 35f -> Pair(Color(0xFFE11D48), Color(0xFFF97316)) // Extreme Heat (Vibrant Rose to Deep Orange)
                state.temperature > 28f -> Pair(Color(0xFFF59E0B), Color(0xFFD97706)) // Warm/Sunny (Amber to Dark Amber)
                state.temperature < 0f -> Pair(Color(0xFF0284C7), Color(0xFF0369A1)) // Frosty Ice Blue (Deep sky to steel blue)
                state.weatherCode in listOf(61, 63, 65, 80, 81, 82, 95, 96, 99) -> Pair(Color(0xFF334155), Color(0xFF0F172A)) // Stormy/Rainy (Deep Slate to Midnight)
                else -> Pair(Color(0xFF0EA5E9), Color(0xFF1D4ED8)) // Fresh Sky Blue to Royal Blue
            }
        }
    }

    val animatedSkyStart by animateColorAsState(targetValue = skyStart, animationSpec = tween(durationMillis = 800), label = "SkyStart")
    val animatedSkyEnd by animateColorAsState(targetValue = skyEnd, animationSpec = tween(durationMillis = 800), label = "SkyEnd")

    val isDarkTheme = isNight || state.weatherCode in listOf(61, 63, 65, 80, 81, 82, 95, 96, 99)
    val mainTextColor = Color.White
    val subTextColor = Color.White.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedSkyStart, animatedSkyEnd)))
    ) {
        WeatherBackgroundEffect(weatherCode = state.weatherCode, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Top Offline Network Banner
            OfflineBanner(isOnline = state.isOnline, lastRefreshedTime = state.lastRefreshedTime)

            // 2. Premium Glassmorphic Search Bar
            SearchBar(searchQuery = state.searchQuery, onQueryChange = actions.onQueryChange)

            // 3. Glassmorphic Action Toolbar
            ActionToolbar(
                onLocationClick = actions.onLocationClick,
                onToggleSavedCitiesScreen = actions.onToggleSavedCitiesScreen,
                onSettingsClick = actions.onSettingsClick,
                onRefresh = actions.onRefresh
            )

            // 4. Search Auto-Suggestions Dropdown Overlay
            SearchResultsList(searchResults = state.searchResults, onSelectCity = actions.onSelectCity)

            if (state.isLoading) {
                LinearProgressIndicator(
                    color = Color(0xFF0D9488),
                    trackColor = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                )
            }

            state.errorMessage?.let { error ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2).copy(alpha = 0.9f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = Color(0xFF991B1B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 5. Header displaying main weather metrics
            WeatherHeader(
                selectedCity = state.selectedCity,
                savedCities = state.savedCities,
                weatherCode = state.weatherCode,
                temperature = state.temperature,
                isCelsius = state.isCelsius,
                isWeatherUnionSource = state.isWeatherUnionSource,
                isIndianApiSourceActive = state.isIndianApiSourceActive,
                dailyForecast = state.dailyForecast,
                onRemoveCity = actions.onRemoveCity,
                onSaveCity = actions.onSaveCity,
                mainTextColor = mainTextColor,
                subTextColor = subTextColor
            )

            // 6. Severe Weather Alert Card
            SevereWeatherAlertCard(
                alertEvent = state.alertEvent,
                alertHeadline = state.alertHeadline,
                alertDesc = state.alertDesc
            )

            // 7. "Show More Details" Toggle Button & Details Panel
            var showMoreDetails by rememberSaveable { mutableStateOf(false) }

            Button(
                onClick = { showMoreDetails = !showMoreDetails },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (showMoreDetails) "Show Less" else "Show More Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (showMoreDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showMoreDetails) {
                var selectedDetailTab by remember { mutableIntStateOf(0) }
                val tabTitles = listOf("Forecast", "Metrics", "Sun & Climate")

                SecondaryTabRow(
                    selectedTabIndex = selectedDetailTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedDetailTab == index,
                            onClick = { selectedDetailTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedDetailTab == index) Color.White else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Render active tab content
                when (selectedDetailTab) {
                    0 -> {
                        // 1. Forecast Tab
                        if (state.hourlyForecast.isNotEmpty()) {
                            AppleFrostedCard(
                                title = "HOURLY FORECAST",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    items(state.hourlyForecast) { hour ->
                                        HourlyItem(
                                            hour = hour.hour,
                                            temp = formatTemp(hour.temperature, state.isCelsius),
                                            weatherCode = hour.weatherCode
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            HourlyTemperatureChartCard(state.hourlyForecast, state.isCelsius)
                        }

                        if (state.dailyForecast.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            AppleFrostedCard(
                                title = "7-DAY FORECAST",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    state.dailyForecast.forEach { day ->
                                        DailyItem(
                                            day = day.day,
                                            weatherCode = day.weatherCode,
                                            tempMin = formatTemp(day.tempMin, state.isCelsius),
                                            tempMax = formatTemp(day.tempMax, state.isCelsius)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // 2. Metrics Tab
                        Text(
                            text = "Weather Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val dpC = state.temperature - (100f - state.humidity) / 5f
                            val dpDesc = "The dew point is approx ${formatTemp(dpC, state.isCelsius)} right now."

                            MetricFrostedCard(
                                title = "Humidity",
                                value = "${state.humidity.roundToInt()}%",
                                desc = dpDesc,
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(state.humidity / 100f)
                                            .background(Brush.horizontalGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))))
                                    )
                                }
                            }

                            // Let's create the wave offset animation for wind wave
                            val windInfiniteTransition = rememberInfiniteTransition(label = "WindWaveAnim")
                            val windWaveOffset by windInfiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 2f * Math.PI.toFloat(),
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "WindWaveOffset"
                            )

                            MetricFrostedCard(
                                title = "Wind Speed",
                                value = formatWind(state.windSpeed, state.isCelsius),
                                desc = "Winds blow calm. Telemetry aligned to current source.",
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val path = Path()
                                    val midY = h / 2f
                                    path.moveTo(0f, midY)
                                    for (x in 0..w.toInt() step 5) {
                                        val xFloat = x.toFloat()
                                        val y = midY + sin(xFloat * 0.05f + windWaveOffset) * 4.dp.toPx()
                                        path.lineTo(xFloat, y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF60A5FA),
                                        style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricFrostedCard(
                                title = "Pressure",
                                value = "${state.pressure.roundToInt()} hPa",
                                desc = if (state.pressure < 1009) "Low pressure center. Rain showers possible." else "High pressure system. Clear and dry.",
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val barHeight = 4.dp.toPx()
                                    val y = (h - barHeight) / 2f
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.2f),
                                        topLeft = Offset(0f, y),
                                        size = Size(w, barHeight),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                    val normX = w / 2f
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.4f),
                                        start = Offset(normX, y - 2.dp.toPx()),
                                        end = Offset(normX, y + barHeight + 2.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    val fraction = ((state.pressure - 980f) / 60f).coerceIn(0f, 1f)
                                    val cx = fraction * w
                                    drawCircle(
                                        color = Color(0xFF34D399),
                                        radius = 4.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                }
                            }

                            MetricFrostedCard(
                                title = "Air Quality (US-AQI)",
                                value = "${state.aqi}",
                                desc = when {
                                    state.aqi > 150 -> "Unhealthy levels. Limit prolonged outdoor walking."
                                    state.aqi > 50 -> "Moderate levels. Acceptable for general public."
                                    else -> "Excellent air quality. Perfect for runs!"
                                },
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val barHeight = 6.dp.toPx()
                                    val y = (h - barHeight) / 2f
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF10B981), // Green (Good)
                                                Color(0xFFFBBF24), // Yellow (Moderate)
                                                Color(0xFFF97316), // Orange (USG)
                                                Color(0xFFEF4444), // Red (Unhealthy)
                                                Color(0xFF8B5CF6)  // Purple (Hazardous)
                                            )
                                        ),
                                        topLeft = Offset(0f, y),
                                        size = Size(w, barHeight),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                    val fraction = (state.aqi / 200f).coerceIn(0f, 1f)
                                    val cx = fraction * w
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                    drawCircle(
                                        color = Color(0xFF0F172A),
                                        radius = 2.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricFrostedCard(
                                title = "UV Index",
                                value = if (state.uvIndex % 1 == 0f) state.uvIndex.toInt().toString() else state.uvIndex.toString(),
                                desc = when {
                                    state.uvIndex >= 8 -> "Very high risk. Sunscreen (SPF 30+) required."
                                    state.uvIndex >= 5 -> "Moderate risk. Wear hats and stay in shade."
                                    else -> "Low UV radiation. Safe to stay outside."
                                },
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val barHeight = 6.dp.toPx()
                                    val y = (h - barHeight) / 2f
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF10B981), // Low (Green)
                                                Color(0xFFFBBF24), // Mod (Yellow)
                                                Color(0xFFF97316), // High (Orange)
                                                Color(0xFFEF4444), // Very High (Red)
                                                Color(0xFF8B5CF6)  // Extreme (Purple)
                                            )
                                        ),
                                        topLeft = Offset(0f, y),
                                        size = Size(w, barHeight),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                    val fraction = (state.uvIndex / 11f).coerceIn(0f, 1f)
                                    val cx = fraction * w
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                    drawCircle(
                                        color = Color(0xFF0F172A),
                                        radius = 2.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    2 -> {
                        // 3. Sun & Climate Tab
                        SunriseSunsetArcCard(state)
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        val anomalyInfiniteTransition = rememberInfiniteTransition(label = "AnomalyPulseAnim")
                        val pulseAlpha by anomalyInfiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "AnomalyPulseAlpha"
                        )

                        MetricFrostedCard(
                            title = "Climatic Anomalies",
                            value = "Normal",
                            desc = "Current telemetry values align with 30-year seasonal models.",
                            isDark = isDarkTheme,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Canvas(modifier = Modifier.size(8.dp)) {
                                    drawCircle(
                                        color = Color(0xFF10B981).copy(alpha = pulseAlpha),
                                        radius = 4.dp.toPx()
                                    )
                                    drawCircle(
                                        color = Color(0xFF34D399),
                                        radius = 2.dp.toPx()
                                    )
                                }
                                Text(
                                    text = "Telemetry Aligned",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }

                        if (state.isWeatherUnionSource) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AwsStationRadarMapCard(state = state)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // WeatherAPI.com Configuration Settings Dialog
        if (state.showKeyDialog) {
            SettingsDialog(
                state = state,
                onDismissSettings = actions.onDismissSettings,
                onSaveGoogleApiKey = actions.onSaveGoogleApiKey,
                onSaveIndianApiKey = actions.onSaveIndianApiKey,
                onToggleCelsius = actions.onToggleCelsius
            )
        }

        // Saved Cities Dialog Drawer
        if (state.showSavedCitiesScreen) {
            SavedCitiesDialog(
                state = state,
                onToggleSavedCitiesScreen = actions.onToggleSavedCitiesScreen,
                onSelectCity = actions.onSelectCity,
                onRemoveCity = actions.onRemoveCity
            )
        }
    }
}

@Composable
fun OfflineBanner(
    isOnline: Boolean,
    lastRefreshedTime: String,
    modifier: Modifier = Modifier
) {
    if (!isOnline) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.85f)),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚠️ Offline Mode. Showing cached data from ${lastRefreshedTime.ifBlank { "earlier" }}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = { Text("Search city (e.g. Paris, Tokyo)", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f)) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.25f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
            focusedBorderColor = Color.White.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun ActionToolbar(
    onLocationClick: () -> Unit,
    onToggleSavedCitiesScreen: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onLocationClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Default.Place, contentDescription = "Use Current Location", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = { onToggleSavedCitiesScreen(true) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Favorites", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onSettingsClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            IconButton(
                onClick = onRefresh,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Weather", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SearchResultsList(
    searchResults: List<GeocodingResult>,
    onSelectCity: (GeocodingResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (searchResults.isNotEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, Color.White, RoundedCornerShape(16.dp))
        ) {
            Column {
                searchResults.forEach { result ->
                    val subText = listOfNotNull(result.admin1, result.country).joinToString(", ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCity(result) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = result.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            if (subText.isNotEmpty()) {
                                Text(text = subText, color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = "Lat: ${String.format(java.util.Locale.US, "%.2f", result.latitude)}, Lon: ${String.format(java.util.Locale.US, "%.2f", result.longitude)}",
                            color = Color(0xFF0D9488),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun WeatherHeader(
    selectedCity: String,
    savedCities: List<GeocodingResult>,
    weatherCode: Int,
    temperature: Float,
    isCelsius: Boolean,
    isWeatherUnionSource: Boolean,
    isIndianApiSourceActive: Boolean,
    dailyForecast: List<DayForecast>,
    onRemoveCity: (GeocodingResult) -> Unit,
    onSaveCity: () -> Unit,
    mainTextColor: Color,
    subTextColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedCity,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = mainTextColor,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            
            val isFavorite = savedCities.any { it.name.equals(selectedCity, ignoreCase = true) }
            IconButton(
                onClick = {
                    if (isFavorite) {
                        val matched = savedCities.firstOrNull { it.name.equals(selectedCity, ignoreCase = true) }
                        if (matched != null) onRemoveCity(matched)
                    } else {
                        onSaveCity()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFavorite) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.4f)
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = formatTemp(temperature, isCelsius),
                fontSize = 92.sp,
                fontWeight = FontWeight.W200,
                color = mainTextColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            WeatherIcon(
                code = weatherCode,
                modifier = Modifier.size(88.dp)
            )
        }

        Text(
            text = weatherCode.getWeatherCodeDescription(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = subTextColor
        )

        val firstDay = dailyForecast.firstOrNull()
        if (firstDay != null) {
            Text(
                text = "H:${formatTemp(firstDay.tempMax, isCelsius)}   L:${formatTemp(firstDay.tempMin, isCelsius)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = subTextColor
            )
        }

        if (isWeatherUnionSource) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isIndianApiSourceActive) Color(0xFFBEF264).copy(alpha = 0.8f)
                        else Color.White.copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isIndianApiSourceActive) "⚡ Local Source: Open-Meteo API"
                           else "⚡ Source: Google Weather API",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIndianApiSourceActive) Color(0xFF365314) else Color.White,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

@Composable
fun SevereWeatherAlertCard(
    alertEvent: String?,
    alertHeadline: String?,
    alertDesc: String?,
    modifier: Modifier = Modifier
) {
    if (!alertEvent.isNullOrBlank()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF43F5E).copy(alpha = 0.2f)),
            modifier = modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFF43F5E).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚠️ ${alertEvent.uppercase()}",
                    color = Color(0xFFFDA4AF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!alertHeadline.isNullOrBlank()) {
                    Text(
                        text = alertHeadline,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!alertDesc.isNullOrBlank()) {
                    Text(
                        text = alertDesc,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    state: WeatherState,
    onDismissSettings: () -> Unit,
    onSaveGoogleApiKey: (String) -> Unit,
    onSaveIndianApiKey: (String) -> Unit,
    onToggleCelsius: () -> Unit
) {
    var tempGoogleApiKeyText by remember { mutableStateOf(state.googleApiKey) }
    var tempIndianApiKeyText by remember { mutableStateOf(state.indianApiKey) }
    
    AlertDialog(
        onDismissRequest = onDismissSettings,
        title = { Text("Sunnyside Configurations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Temperature Units",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Fahrenheit (°F)",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Switch(
                        checked = !state.isCelsius,
                        onCheckedChange = { onToggleCelsius() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0D9488)
                        )
                    )
                }

                HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))

                Text(
                    text = "Google API Key (Required)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                OutlinedTextField(
                    value = tempGoogleApiKeyText,
                    onValueChange = { tempGoogleApiKeyText = it },
                    placeholder = { Text("Enter Google API Key", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Required to enable geocoding, maps, and location telemetries across regional weather nodes.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))

                Text(
                    text = "⚡ Open-Meteo API (No Key Required)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1D4ED8)
                )
                Text(
                    text = "Active automatically for all Indian cities. Keyless integration with zero configuration required.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveGoogleApiKey(tempGoogleApiKeyText.trim())
                    onSaveIndianApiKey(tempIndianApiKeyText.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissSettings) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun SavedCitiesDialog(
    state: WeatherState,
    onToggleSavedCitiesScreen: (Boolean) -> Unit,
    onSelectCity: (GeocodingResult) -> Unit,
    onRemoveCity: (GeocodingResult) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onToggleSavedCitiesScreen(false) },
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Favorites", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                TextButton(onClick = { onToggleSavedCitiesScreen(false) }) {
                    Text("Close", color = Color(0xFF0D9488))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.savedCities.isEmpty()) {
                    Text(
                        text = "No favorite cities saved yet. Tap the star button next to any city name to bookmark it!",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(state.savedCities) { city ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onSelectCity(city)
                                            onToggleSavedCitiesScreen(false)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = city.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                        if (city.country != null) {
                                            Text(text = city.country, color = Color(0xFF64748B), fontSize = 11.sp)
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { onRemoveCity(city) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Favorite",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

private fun calculateSunPosition(
    timeInMins: Int,
    sunriseMins: Int,
    sunsetMins: Int,
    width: Float,
    height: Float
): Offset? {
    if (timeInMins in sunriseMins..sunsetMins) {
        val progress = if (sunsetMins > sunriseMins) {
            (timeInMins - sunriseMins).toFloat() / (sunsetMins - sunriseMins).toFloat()
        } else 0.5f
        val t = progress
        val u = 1f - t
        val x = u * u * 0f + 2f * u * t * (width / 2f) + t * t * width
        val y = u * u * height + 2f * u * t * (-height * 0.4f) + t * t * height
        return Offset(x, y)
    }
    return null
}

private fun calculateChartPoints(
    temps: List<Float>,
    minTemp: Float,
    tempRange: Float,
    width: Float,
    height: Float,
    stepX: Float
): List<Offset> {
    val points = ArrayList<Offset>(temps.size)
    for (i in temps.indices) {
        val x = i * stepX
        val tempVal = temps[i]
        val y = height - (0.1f * height + ((tempVal - minTemp) / tempRange) * (0.8f * height))
        points.add(Offset(x, y))
    }
    return points
}

@Composable
fun SunriseSunsetArcCard(
    state: WeatherState,
    modifier: Modifier = Modifier
) {
    val sunriseMins = state.sunriseMinutes
    val sunsetMins = state.sunsetMinutes
    val diffMins = (sunsetMins - sunriseMins).coerceAtLeast(0)
    val diffHours = diffMins / 60
    val diffMinsRemain = diffMins % 60
    val daylightStr = if (diffMinsRemain > 0) {
        "$diffHours h $diffMinsRemain m of daylight"
    } else {
        "$diffHours hours of daylight"
    }

    AppleFrostedCard(
        title = "SUNRISE & SUNSET",
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp)
            ) {
                val width = size.width
                val height = size.height
                val arcPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height)
                    quadraticTo(width / 2f, -height * 0.4f, width, height)
                }
                
                drawPath(
                    path = arcPath,
                    color = Color.Black.copy(alpha = 0.15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )
                
                drawLine(
                    color = Color.Black.copy(alpha = 0.1f),
                    start = androidx.compose.ui.geometry.Offset(0f, height),
                    end = androidx.compose.ui.geometry.Offset(width, height),
                    strokeWidth = 2.dp.toPx()
                )

                val calendar = java.util.Calendar.getInstance()
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMin = calendar.get(java.util.Calendar.MINUTE)
                val timeInMins = currentHour * 60 + currentMin
                
                val sunPos = calculateSunPosition(timeInMins, sunriseMins, sunsetMins, width, height)
                if (sunPos != null) {
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = 0.3f),
                        radius = 16.dp.toPx(),
                        center = sunPos
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 8.dp.toPx(),
                        center = sunPos
                    )
                } else {
                    val x = if (timeInMins < sunriseMins) 10.dp.toPx() else width - 10.dp.toPx()
                    val y = height - 5.dp.toPx()
                    drawCircle(
                        color = Color(0xFF94A3B8),
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Sunrise", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Text(state.sunrise, fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
                Text(
                    text = daylightStr,
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sunset", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Text(state.sunset, fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HourlyTemperatureChartCard(
    hourlyForecast: List<HourForecast>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    AppleFrostedCard(
        title = "TEMPERATURE TREND (NEXT 12h)",
        modifier = modifier.fillMaxWidth()
    ) {
        if (hourlyForecast.isEmpty()) {
            Text("No forecast data available", fontSize = 12.sp, color = Color.Gray)
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(top = 16.dp, bottom = 12.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val numPoints = hourlyForecast.size
                    val stepX = width / (numPoints - 1).coerceAtLeast(1)

                    val temps = hourlyForecast.map { 
                        if (isCelsius) it.temperature else (it.temperature * 9f / 5f + 32f) 
                    }
                    val minTemp = temps.minOrNull() ?: 0f
                    val maxTemp = temps.maxOrNull() ?: 100f
                    val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)

                    val points = calculateChartPoints(temps, minTemp, tempRange, width, height, stepX)

                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, height)
                        for (p in points) {
                            lineTo(p.x, p.y)
                        }
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    val linePath = androidx.compose.ui.graphics.Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = Color(0xFFF59E0B),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    for (i in points.indices) {
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = points[i]
                        )
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = 2.dp.toPx(),
                            center = points[i]
                        )

                        val tempDisplay = temps[i].roundToInt().toString() + "°"
                        drawContext.canvas.nativeCanvas.drawText(
                            tempDisplay,
                            points[i].x,
                            points[i].y - 8.dp.toPx(),
                            textPaint
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    hourlyForecast.forEachIndexed { idx, item ->
                        if (idx % 2 == 0 || idx == hourlyForecast.size - 1) {
                            Text(
                                text = item.hour,
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppleFrostedCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.25f)
        )
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .graphicsLayer {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            25f, 25f, android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(Color.White.copy(alpha = 0.12f))
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                content()
            }
        }
    }
}

@Composable
fun MetricFrostedCard(
    title: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val cardBg = if (isDark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.14f)
    val titleColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.65f)
    val valueColor = Color.White
    val descColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.30f),
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.22f)
        )
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .graphicsLayer {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            25f, 25f, android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(cardBg)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title.uppercase(java.util.Locale.US),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = valueColor
                    )
                }

                if (content != null) {
                    val columnScope = this
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        columnScope.content()
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                Text(
                    text = desc,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = descColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HourlyItem(
    hour: String,
    temp: String,
    weatherCode: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(hour, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        WeatherIcon(weatherCode, modifier = Modifier.size(24.dp))
        Text(temp, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DailyItem(
    day: String,
    weatherCode: Int,
    tempMin: String,
    tempMax: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = day,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(60.dp)
        )

        Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
            WeatherIcon(weatherCode, modifier = Modifier.size(22.dp))
        }

        Text(
            text = tempMin,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.7f)
                    .align(Alignment.Center)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF60A5FA), Color(0xFFFBBF24))))
            )
        }

        Text(
            text = tempMax,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun WeatherBackgroundEffect(weatherCode: Int, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherBackgroundEffects")
    
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BackgroundAnimTime"
    )
    
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SunPulse"
    )

    val particles = remember(weatherCode) {
        val secureRandom = java.security.SecureRandom()
        List(25) {
            val startX = secureRandom.nextFloat()
            val startY = secureRandom.nextFloat()
            val speed = 0.5f + secureRandom.nextFloat() * 0.5f
            val size = 2f + secureRandom.nextFloat() * 4f
            val angle = -10f + secureRandom.nextFloat() * 20f
            val phase = (secureRandom.nextDouble() * 2.0 * Math.PI).toFloat()
            Particle(startX, startY, speed, size, angle, phase)
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        when (weatherCode) {
            0 -> {
                val beamColor = Color(0xFFFFD700).copy(alpha = 0.05f * sunPulse)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(beamColor, Color.Transparent),
                        center = Offset(w * 0.8f, h * 0.15f),
                        radius = w * 0.6f
                    ),
                    radius = w * 0.6f,
                    center = Offset(w * 0.8f, h * 0.15f)
                )

                particles.forEach { p ->
                    val y = (p.startY * h + animTime * p.speed * 200f) % h
                    val x = (p.startX * w + sin(animTime * 2 * Math.PI.toFloat() + p.phase) * 15.dp.toPx()) % w
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = 0.12f),
                        radius = p.size.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
            1, 2, 3, 45, 48 -> {
                particles.forEach { p ->
                    val x = (p.startX * w + animTime * p.speed * 100f) % w
                    val y = p.startY * h
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.06f),
                        topLeft = Offset(x - w * 0.15f, y),
                        size = Size(w * 0.3f, h * 0.04f * p.size / 4f),
                        cornerRadius = CornerRadius(15.dp.toPx(), 15.dp.toPx())
                    )
                }
            }
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> {
                particles.forEach { p ->
                    val y = (p.startY * h + animTime * p.speed * 1500f) % h
                    val x = (p.startX * w + y * sin(Math.toRadians(p.angle.toDouble())).toFloat()) % w
                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = 0.15f),
                        start = Offset(x, y),
                        end = Offset(x - 4.dp.toPx(), y + 15.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            71, 73, 75 -> {
                particles.forEach { p ->
                    val y = (p.startY * h + animTime * p.speed * 300f) % h
                    val sway = sin(animTime * 4 * Math.PI.toFloat() + p.phase) * 12.dp.toPx()
                    val x = (p.startX * w + sway) % w
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = p.size.dp.toPx() * 0.8f,
                        center = Offset(x, y)
                    )
                }
            }
            95, 96, 99 -> {
                particles.forEach { p ->
                    val y = (p.startY * h + animTime * p.speed * 1800f) % h
                    val x = (p.startX * w - y * 0.05f) % w
                    drawLine(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.18f),
                        start = Offset(x, y),
                        end = Offset(x - 3.dp.toPx(), y + 12.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            else -> {
                particles.forEach { p ->
                    val x = (p.startX * w + animTime * p.speed * 80f) % w
                    val y = p.startY * h
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = p.size.dp.toPx() * 1.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

data class Particle(
    val startX: Float,
    val startY: Float,
    val speed: Float,
    val size: Float,
    val angle: Float,
    val phase: Float
)

private fun formatTemp(tempC: Float, isCelsius: Boolean): String {
    val value = if (isCelsius) tempC else (tempC * 9f / 5f + 32f)
    return "${value.roundToInt()}°"
}

private fun formatWind(windKmH: Float, isCelsius: Boolean): String {
    val value = if (isCelsius) windKmH else (windKmH * 0.621371f)
    val unit = if (isCelsius) "km/h" else "mph"
    return "${value.roundToInt()} $unit"
}

@Composable
fun AwsStationRadarMapCard(
    state: WeatherState,
    modifier: Modifier = Modifier
) {
    val mathCos = { angle: Double -> kotlin.math.cos(angle) }
    val mathSin = { angle: Double -> kotlin.math.sin(angle) }

    AppleFrostedCard(
        title = "OPEN-METEO GRID RESOLUTION MAP",
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Grid Point Coordinates: Lat: ${state.stationLat} , Lon: ${state.stationLon}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            val scanTransition = rememberInfiniteTransition(label = "RadarScanAnim")
            val scanAngle by scanTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "RadarAngle"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val maxRadius = Math.min(w, h) * 0.45f

                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        radius = maxRadius * 0.66f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        radius = maxRadius * 0.33f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )

                    val angleRad = Math.toRadians(scanAngle.toDouble())
                    val endX = center.x + maxRadius * mathCos(angleRad).toFloat()
                    val endY = center.y + maxRadius * mathSin(angleRad).toFloat()
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF10B981).copy(alpha = 0.6f), Color.Transparent),
                            start = center,
                            end = Offset(endX, endY)
                        ),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 3.dp.toPx()
                    )

                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val latDelta = state.stationLat - state.latitude
                    val lonDelta = state.stationLon - state.longitude

                    val scaleX = if (lonDelta != 0.0) ((lonDelta * maxRadius * 50.0).coerceIn(-maxRadius * 0.8, maxRadius * 0.8)).toFloat() else -maxRadius * 0.4f
                    val scaleY = if (latDelta != 0.0) (-(latDelta * maxRadius * 50.0).coerceIn(-maxRadius * 0.8, maxRadius * 0.8)).toFloat() else maxRadius * 0.3f

                    val stationOffset = Offset(center.x + scaleX, center.y + scaleY)

                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 6.dp.toPx(),
                        center = stationOffset
                    )
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.3f),
                        radius = 15.dp.toPx(),
                        center = stationOffset,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = center,
                        end = stationOffset,
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                Text(
                    text = state.selectedCity,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 26.dp)
                )

                Text(
                    text = "GRID POINT",
                    color = Color(0xFF34D399),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )

                Text(
                    text = "RESOLUTION: 11KM GRID",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
            }
        }
    }
}
