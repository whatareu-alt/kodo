package com.example.aiweathermonitor.util

import android.util.Log
import com.example.aiweathermonitor.exception.WeatherException

/**
 * Utility for structured logging throughout the application.
 * Provides type-safe logging methods for different severity levels.
 */
object WeatherLogger {
    
    /**
     * Logs a debug message.
     */
    fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    /**
     * Logs an info message.
     */
    fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    /**
     * Logs a warning message.
     */
    fun warn(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) {
            Log.w(tag, message, exception)
        } else {
            Log.w(tag, message)
        }
    }

    /**
     * Logs an error message with exception.
     */
    fun error(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) {
            Log.e(tag, message, exception)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Logs a weather-related exception with structured information.
     */
    fun logException(tag: String, message: String, exception: WeatherException) {
        val errorDetails = """
            |Error: $message
            |Type: ${exception.javaClass.simpleName}
            |Cause: ${exception.cause?.javaClass?.simpleName ?: "None"}
            |Message: ${exception.message}
        """.trimMargin()
        Log.e(tag, errorDetails, exception)
    }

    /**
     * Logs network request details for debugging.
     */
    fun logNetworkRequest(tag: String, url: String, method: String = "GET") {
        Log.d(tag, "→ $method $url")
    }

    /**
     * Logs network response details for debugging.
     */
    fun logNetworkResponse(tag: String, statusCode: Int, responseTime: Long) {
        Log.d(tag, "← HTTP $statusCode (${responseTime}ms)")
    }

    /**
     * Logs data parsing operations.
     */
    fun logDataParsing(tag: String, dataType: String, successful: Boolean) {
        val status = if (successful) "✓" else "✗"
        Log.d(tag, "$status Parsing $dataType")
    }
}
