/*
 * Copyright (C) 2026 MaxxOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager

object DeviceInfoUtil {

    private const val BYTES_PER_MB = 1024L * 1024L
    private const val BYTES_PER_GB = 1024L * 1024L * 1024L

    fun getTotalRam(): String {
        return try {
            val text = java.io.File("/proc/meminfo").readText()
            val match = Regex("MemTotal:\\s+(\\d+) kB").find(text)
            val kb = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            "${kb / (1024 * 1024)} GB"
        } catch (e: Exception) {
            "--"
        }
    }

    fun getStorageTotal(context: Context): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockSizeLong * stat.blockCountLong
            "${total / BYTES_PER_GB} GB"
        } catch (e: Exception) {
            "--"
        }
    }

    fun getBatteryCapacity(context: Context): String {
        return try {
            val intent = context.registerReceiver(null,
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val chargeFull = intent?.getIntExtra("charge_full", -1) ?: -1
            if (chargeFull > 0) {
                "${chargeFull} mAh"
            } else {
                val capacity = intent?.getIntExtra(android.os.BatteryManager.EXTRA_CAPACITY, -1)
                if (capacity != null && capacity > 0) "${capacity}%" else "--"
            }
        } catch (e: Exception) {
            "--"
        }
    }

    fun getScreenResolution(context: Context): String {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bounds = wm.maximumWindowMetrics.bounds
            "${bounds.width()}x${bounds.height()}"
        } catch (e: Exception) {
            "--"
        }
    }
}
