/*
 * Copyright (C) 2026 Patricio Labin Correa (f1r3f0x)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plabin.librespeedo.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A client wrapper around the native Android [LocationManager].
 *
 * It is responsible for determining the best available location provider
 * (prioritizing the native [LocationManager.FUSED_PROVIDER] when available on Android 12+),
 * and converting the callback-based location updates into a continuous Kotlin [Flow].
 *
 * @param context The application or activity context needed to access system services.
 */
open class LocationClient(private val context: Context) {

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    /**
     * Subscribes to location updates and emits them as a flow.
     *
     * @param intervalMs The minimum time interval between location updates, in milliseconds.
     * @return A [Flow] emitting [Location] objects as they arrive.
     * Waits for a provider when location is disabled and reselects it as availability changes.
     */
    @SuppressLint("MissingPermission")
    open fun getLocationUpdates(intervalMs: Long): Flow<Location> = callbackFlow {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        var activeProvider: String? = null
        fun selectProvider() {
            try {
                val providers = buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
                    add(LocationManager.GPS_PROVIDER)
                    add(LocationManager.NETWORK_PROVIDER)
                }
                val provider = providers.firstOrNull { locationManager.isProviderEnabled(it) }
                if (provider == activeProvider) return
                locationManager.removeUpdates(locationListener)
                activeProvider = null
                if (provider != null) {
                    locationManager.requestLocationUpdates(
                        provider, intervalMs, 0f, locationListener, Looper.getMainLooper()
                    )
                    activeProvider = provider
                }
            } catch (e: SecurityException) {
                close(e)
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = selectProvider()
        }
        ContextCompat.registerReceiver(
            context, receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION).apply {
                addAction(LocationManager.MODE_CHANGED_ACTION)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        selectProvider()
        awaitClose {
            context.unregisterReceiver(receiver)
            locationManager.removeUpdates(locationListener)
        }
    }
}
