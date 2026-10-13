package com.appdony.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object DownloadHelper {

    fun startDownload(context: Context, url: String, fileName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription("در حال دانلود از اپدونی...")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(
                context,
                "دانلود شروع شد ✓",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "خطا در شروع دانلود: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
