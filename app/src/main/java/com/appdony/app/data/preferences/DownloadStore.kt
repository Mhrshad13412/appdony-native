package com.appdony.app.data.preferences

import android.content.Context
import com.google.gson.Gson

data class DownloadRecord(
    val id: Long,
    val title: String,
    val iconUrl: String? = null,
    val fileName: String,
    val sourceUrl: String,
    val createdAt: Long,
    val completed: Boolean = false,
    val localUri: String? = null,
    val totalBytes: Long = 0L,
    // وضعیت خودمون (همون عددهای DownloadManager.STATUS_*): 1=صف 2=در حال دانلود 4=متوقف 8=کامل 16=خطا
    val status: Int = 0,
    val bytes: Long = 0L,
    val message: String? = null,
    val filePath: String? = null,
    val wifiOnly: Boolean = false
)

class DownloadStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("downloads_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<DownloadRecord> = synchronized(LOCK) { readAll() }

    fun add(record: DownloadRecord) = synchronized(LOCK) {
        save(listOf(record) + readAll().filter { it.id != record.id })
    }

    fun update(record: DownloadRecord) = synchronized(LOCK) {
        save(readAll().map { if (it.id == record.id) record else it })
    }

    fun remove(id: Long) = synchronized(LOCK) {
        save(readAll().filter { it.id != id })
    }

    private fun readAll(): List<DownloadRecord> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            gson.fromJson(json, Array<DownloadRecord>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(list: List<DownloadRecord>) {
        prefs.edit().putString(KEY, gson.toJson(list.take(200))).apply()
    }

    private companion object {
        const val KEY = "records"
        val LOCK = Any()
    }
}
