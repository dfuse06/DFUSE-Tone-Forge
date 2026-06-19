package com.example.dfusetoneforge

import android.os.Build

object DeviceSupport {
    val isSamsung: Boolean
        get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    val deviceLabel: String
        get() = if (isSamsung) "Samsung Galaxy" else Build.MANUFACTURER
}