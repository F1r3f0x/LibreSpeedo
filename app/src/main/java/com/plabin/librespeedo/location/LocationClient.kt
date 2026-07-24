package com.plabin.librespeedo.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationClient(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long): Flow<Location> = callbackFlow {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val isFusedEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isFusedEnabled) {
            locationManager.requestLocationUpdates(
                LocationManager.FUSED_PROVIDER,
                intervalMs,
                0f,
                locationListener
            )
        } else if (isGpsEnabled) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalMs,
                0f,
                locationListener
            )
        } else if (isNetworkEnabled) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                intervalMs,
                0f,
                locationListener
            )
        } else {
            close(Exception("No location providers are enabled"))
        }

        awaitClose {
            locationManager.removeUpdates(locationListener)
        }
    }
}
