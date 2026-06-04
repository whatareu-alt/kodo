package com.example.aiweathermonitor

import android.app.Application
import com.example.aiweathermonitor.ui.main.MainScreenViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    // Provide OkHttpClient as a singleton
    single { OkHttpClient() }
    
    // Provide Json parser as a singleton
    single { Json { ignoreUnknownKeys = true } }
    
    // Provide MainScreenViewModel with injected client and parser
    viewModel { MainScreenViewModel(get(), get()) }
}

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WeatherApplication)
            modules(appModule)
        }
    }
}
