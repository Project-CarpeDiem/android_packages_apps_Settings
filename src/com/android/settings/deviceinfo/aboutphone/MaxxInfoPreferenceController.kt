/*
 * Copyright (C) 2023 the MaxxOS Android Project
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
package com.android.settings.deviceinfo.aboutphone

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.SystemProperties
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference
import com.android.settings.utils.DeviceInfoUtil

class OutExpoInterpolator : TimeInterpolator {
    override fun getInterpolation(t: Float): Float {
        return if (t == 1f) 1f
        else (1 - Math.pow(2.0, (-10 * t).toDouble())).toFloat()
    }
}

class MaxxInfoPreferenceController(context: Context) : AbstractPreferenceController(context) {

    private val defaultFallback = mContext.getString(R.string.device_info_default)
    private var versionTextView1: TextView? = null
    private var versionTextView2: TextView? = null
    private var isTextView1Visible = true

    private val handler = Handler()
    private val updateTextRunnable = object : Runnable {
        override fun run() {
            animateTextChange()
            handler.postDelayed(this, 3000)
        }
    }
    private var currentMessageIndex = 0

    private val versionMessages = listOf(
        "#${getProp(PROP_MAXX_CODE)}",
        "v ${getRomVersion()}"
    )

    private fun getProp(propName: String): String {
        return SystemProperties.get(propName, defaultFallback)
    }

    private fun getProp(propName: String, customFallback: String): String {
        val propValue = SystemProperties.get(propName)
        return if (propValue.isNotEmpty()) propValue else SystemProperties.get(customFallback, "Unknown")
    }

    private fun getFirmwareChipset(): String {
        return getProp(PROP_MAXX_CHIPSET, "ro.board.platform")
    }

    private fun getDeviceName(): String {
        val deviceName = "${Build.DEVICE}"
        return deviceName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun getBuildVersion(): String {
        return getProp(PROP_MAXX_BUILD_VERSION)
    }

    private fun getSecurityPatch(): String {
        return getProp(PROP_MAXX_SECURITY)
    }

    private fun getRomVersion(): String {
        return SystemProperties.get(PROP_MAXX_VERSION, "2.0")
    }

    private fun getBuildStatus(releaseType: String): String {
        return mContext.getString(if (releaseType == "official") R.string.build_is_official_title else R.string.build_is_community_title)
    }

    private fun getMaintainer(releaseType: String): String {
        val firmwareMaintainer = getProp(PROP_MAXX_MAINTAINER)
        return if (firmwareMaintainer.equals("Unknown", ignoreCase = true)) {
            mContext.getString(R.string.unknown_maintainer)
        } else {
            mContext.getString(R.string.maintainer_summary, firmwareMaintainer)
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val releaseType = getProp(PROP_MAXX_RELEASETYPE).lowercase()
        val firmwareMaintainer = getMaintainer(releaseType)
        val isOfficial = releaseType == "official"

        val hwInfoPreference = screen.findPreference<LayoutPreference>(KEY_HW_INFO)
        val swInfoPreference = screen.findPreference<LayoutPreference>(KEY_SW_INFO)

        swInfoPreference?.let { swPref ->
            val maintainerTextView: TextView? = swPref.findViewById(R.id.firmware_maintainer)
            maintainerTextView?.text = firmwareMaintainer
            maintainerTextView?.isSelected = true

            versionTextView1 = swPref.findViewById(R.id.firmware_version_1)
            versionTextView2 = swPref.findViewById(R.id.firmware_version_2)

            // Set the initial text and visibility
            versionTextView1?.text = versionMessages[currentMessageIndex]
            versionTextView1?.visibility = View.VISIBLE
            versionTextView2?.visibility = View.GONE

            swPref.findViewById<TextView>(R.id.firmware_status)?.text = getBuildStatus(releaseType).lowercase()
            swPref.findViewById<ImageView>(R.id.firmware_status_icon)?.setImageResource(
                if (isOfficial) R.drawable.verified else R.drawable.unverified
            )
        }

        hwInfoPreference?.apply {
            findViewById<TextView>(R.id.device_chipset)?.text = getFirmwareChipset()
            findViewById<TextView>(R.id.device_storage)?.text =
                "${DeviceInfoUtil.getTotalRam()} | ${DeviceInfoUtil.getStorageTotal(mContext)}"
            findViewById<TextView>(R.id.device_battery_capacity)?.text = DeviceInfoUtil.getBatteryCapacity(mContext)
            findViewById<TextView>(R.id.device_resolution)?.text = DeviceInfoUtil.getScreenResolution(mContext)
            findViewById<TextView>(R.id.device_showcase)?.text = getDeviceName()
        }

        handler.post(updateTextRunnable)
    }

    private fun animateTextChange() {
        val outView = if (isTextView1Visible) versionTextView1 else versionTextView2
        val inView = if (isTextView1Visible) versionTextView2 else versionTextView1
        val outExpoInterpolator = OutExpoInterpolator()

        outView?.let { outTv ->
            inView?.let { inTv ->
                val width = outTv.width.takeIf { it > 0 }?.toFloat() ?: 200f // fallback width if not measured yet
                currentMessageIndex = (currentMessageIndex + 1) % versionMessages.size
                inTv.text = versionMessages[currentMessageIndex]

                // Prepare the incoming view
                inTv.alpha = 0f
                inTv.translationX = width
                inTv.visibility = View.VISIBLE

                // Animate outgoing view: fade out and move left
                outTv.animate()
                .alpha(0f)
                .translationX(-width / 2)
                .setDuration(600)
                .setInterpolator(outExpoInterpolator)
                .start()

                // Animate incoming view: fade in and move to center from right
                inTv.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(600)
                .setInterpolator(outExpoInterpolator)
                .withEndAction {
                    outTv.visibility = View.GONE
                    outTv.alpha = 1f
                    outTv.translationX = 0f
                    isTextView1Visible = !isTextView1Visible
                }
                .start()
            }
        }
    }

    override fun isAvailable(): Boolean {
        return true
    }

    override fun getPreferenceKey(): String {
        return KEY_DEVICE_INFO
    }

    companion object {
        private const val FIRMWARE_NAME = "MaxxOS"
        private const val KEY_KERNEL_INFO = "kernel_version_sw"
        private const val KEY_SW_INFO = "my_device_sw_header"
        private const val KEY_HW_INFO = "my_device_hw_header"
        private const val KEY_DEVICE_INFO = "my_device_info_header"
        private const val KEY_BUILD_BANNER = "banner_logo"

        private const val PROP_MAXX_CODE = "ro.maxx.code"
        private const val PROP_MAXX_VERSION = "ro.maxx.version"
        private const val PROP_MAXX_RELEASETYPE = "ro.maxx.releasetype"
        private const val PROP_MAXX_MAINTAINER = "ro.maxx.maintainer"
        private const val PROP_MAXX_BUILD_VERSION = "ro.maxx.build.version"
        private const val PROP_MAXX_CHIPSET = "ro.maxx.chipset"
        private const val PROP_MAXX_SECURITY = "ro.build.version.security_patch"
    }
}
