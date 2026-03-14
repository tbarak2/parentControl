package com.parentcontrol.child.model

import android.os.Build

data class DeviceInfo(
    val model: String,
    val androidVersion: String
) {
    companion object {
        fun current() = DeviceInfo(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }
}
