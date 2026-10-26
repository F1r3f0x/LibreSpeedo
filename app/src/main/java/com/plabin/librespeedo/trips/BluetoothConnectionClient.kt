package com.plabin.librespeedo.trips

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class CarConnection { CONNECTED, DISCONNECTED, UNKNOWN }
data class PairedCar(val address: String, val name: String)

/** Observes public Bluetooth connection broadcasts and connected audio/headset profiles only. */
class BluetoothConnectionClient(private val context: Context) {
    private val adapter: BluetoothAdapter? get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun pairedCars(): List<PairedCar> {
        if (!hasConnectPermission()) return emptyList()
        return try {
            adapter?.bondedDevices.orEmpty().map { PairedCar(it.address, it.name ?: "Bluetooth device") }
                .sortedBy { it.name.lowercase() }
        } catch (_: SecurityException) { emptyList() }
    }

    fun observe(address: String): Flow<CarConnection> = callbackFlow {
        if (!hasConnectPermission()) {
            trySend(CarConnection.UNKNOWN)
            return@callbackFlow
        }
        val btAdapter = adapter
        if (btAdapter == null) {
            trySend(CarConnection.UNKNOWN)
            return@callbackFlow
        }
        try {
            if (!btAdapter.isEnabled) {
                trySend(CarConnection.DISCONNECTED)
                return@callbackFlow
            }
        } catch (_: SecurityException) {
            trySend(CarConnection.UNKNOWN)
            return@callbackFlow
        }

        var receiverRegistered = false
        val proxies = mutableMapOf<Int, BluetoothProfile>()
        var profileChecks = 0
        var anyProfileConnected = false
        var completedProfileChecks = 0
        fun checkProfile(profile: BluetoothProfile) {
            try {
                if (profile.connectedDevices.any { it.address == address }) anyProfileConnected = true
                completedProfileChecks++
                if (anyProfileConnected) trySend(CarConnection.CONNECTED)
                else if (completedProfileChecks >= profileChecks) trySend(CarConnection.DISCONNECTED)
            } catch (_: SecurityException) {
                trySend(CarConnection.UNKNOWN)
            }
        }
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                proxies[profile] = proxy
                checkProfile(proxy)
            }
            override fun onServiceDisconnected(profile: Int) {
                proxies.remove(profile)
                trySend(CarConnection.UNKNOWN)
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED &&
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_TURNING_OFF
                ) {
                    trySend(CarConnection.DISCONNECTED)
                    return
                }
                val device = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device?.address != address) return
                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> trySend(CarConnection.CONNECTED)
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> trySend(CarConnection.DISCONNECTED)
                }
            }
        }
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            receiverRegistered = true
            // Receiver is registered before the initial profile checks to close the setup race.
            trySend(CarConnection.UNKNOWN)
            profileChecks = 2
            btAdapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)
            btAdapter.getProfileProxy(context, listener, BluetoothProfile.HEADSET)
        } catch (_: SecurityException) {
            trySend(CarConnection.UNKNOWN)
        } catch (_: RuntimeException) {
            trySend(CarConnection.UNKNOWN)
        }
        awaitClose {
            if (receiverRegistered) runCatching { context.unregisterReceiver(receiver) }
            proxies.forEach { (profile, proxy) -> runCatching { btAdapter.closeProfileProxy(profile, proxy) } }
        }
    }

    private fun hasConnectPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
}
