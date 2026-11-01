package com.appdony.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** بعد از روشن شدن گوشی یا آپدیت اپ، پاپیِ بیرونی رو دوباره بالا میاره */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> CatOverlayService.startIfEnabled(context)
        }
    }
}
