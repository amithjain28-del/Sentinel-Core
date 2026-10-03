package com.sentinel.core.orchestrator

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class DeviceContextState(private val context: Context) {

    fun getHardwareContextString(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                  status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }

        val sb = StringBuilder("Device Battery: $batteryPct% (${if (isCharging) "Charging" else "Discharging"})")

        if (batteryPct in 0..15 && !isCharging) {
            sb.append("\nCRITICAL: Device battery is low. Decline heavy asynchronous tasks and respond concisely.")
        }

        return sb.toString()
    }
}
