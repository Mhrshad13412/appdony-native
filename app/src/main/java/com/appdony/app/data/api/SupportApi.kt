package com.appdony.app.data.api

import com.appdony.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** یه پیام توی چت پشتیبانی. sender: "user" یا "support" */
data class SupportMessage(
    val id: Long,
    val thread: String,
    val sender: String,
    val userName: String,
    val body: String,
    val createdAt: String
)

/** یه گفتگو (برای لیست کاربران پشتیبان) */
data class SupportThread(val thread: String, val userName: String, val lastBody: String, val lastId: Long)

/** یه کاربر ثبت‌نام‌شده (برای لیست پشتیبان) */
data class SupportUser(val thread: String, val userName: String)

/**
 * ارتباط با Supabase از طریق REST (بدون کتابخونه‌ی اضافه).
 * thread = شناسه‌ی حساب وردپرسی کاربر، مثلاً "u12".
 */
object SupportApi {

    /** ایمیل پشتیبان. هر کی با این ایمیل وارد شه، پنل پشتیبانی رو می‌بینه */
    const val ADMIN_EMAIL = "mhrshadshaary@gmail.com"

    fun isAdmin(email: String): Boolean = email.trim().equals(ADMIN_EMAIL, ignoreCase = true)

    fun threadOf(userId: Int): String = "u$userId"

    val configured: Boolean get() = BuildConfig.SUPA_URL.isNotBlank() && BuildConfig.SUPA_KEY.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    private fun base() = BuildConfig.SUPA_URL.trim().trimEnd('/') + "/rest/v1/messages"

    private fun req(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("apikey", BuildConfig.SUPA_KEY)
        .header("Authorization", "Bearer " + BuildConfig.SUPA_KEY)

    private fun parse(arr: JSONArray): List<SupportMessage> = List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        SupportMessage(
            id = o.getLong("id"),
            thread = o.getString("thread"),
            sender = o.getString("sender"),
            userName = o.optString("user_name"),
            body = o.getString("body"),
            createdAt = o.optString("created_at")
        )
    }

    /** ارسال پیام. sender باید "user" یا "support" باشه */
    suspend fun send(thread: String, sender: String, userName: String, body: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configured) error("پشتیبانی هنوز تنظیم نشده.")
                val payload = JSONObject()
                    .put("thread", thread).put("sender", sender)
                    .put("user_name", userName).put("body", body.take(2000))
                client.newCall(
                    req(base()).header("Prefer", "return=minimal")
                        .post(payload.toString().toRequestBody(json)).build()
                ).execute().use { if (!it.isSuccessful) error("ارسال نشد (${it.code})") }
            }
        }

    /** پیام‌های جدیدتر از afterId برای یک گفتگو */
    suspend fun fetch(thread: String, afterId: Long): Result<List<SupportMessage>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configured) error("پشتیبانی هنوز تنظیم نشده.")
                val url = base() + "?thread=eq." + java.net.URLEncoder.encode(thread, "UTF-8") +
                    "&id=gt.$afterId&order=id.asc&limit=200"
                client.newCall(req(url).get().build()).execute().use { r ->
                    if (!r.isSuccessful) error("دریافت نشد (${r.code})")
                    parse(JSONArray(r.body?.string().orEmpty()))
                }
            }
        }

    /** برای پشتیبان: پیام‌های تازه‌ی همه‌ی گفتگوها، از روی اون‌ها لیست کاربرها ساخته می‌شه */
    suspend fun threads(): Result<List<SupportThread>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configured) error("پشتیبانی هنوز تنظیم نشده.")
                val url = base() + "?order=id.desc&limit=500"
                client.newCall(req(url).get().build()).execute().use { r ->
                    if (!r.isSuccessful) error("دریافت نشد (${r.code})")
                    val msgs = parse(JSONArray(r.body?.string().orEmpty()))
                    msgs.groupBy { it.thread }.map { (t, list) ->
                        val last = list.first()
                        val name = list.firstOrNull { it.sender == "user" && it.userName.isNotBlank() }?.userName ?: t
                        SupportThread(t, name, last.body, last.id)
                    }.sortedByDescending { it.lastId }
                }
            }
        }

    private fun usersBase() = BuildConfig.SUPA_URL.trim().trimEnd('/') + "/rest/v1/support_users"

    /** ثبت (یا به‌روزرسانی) کاربر توی لیست پشتیبان. ایمیل ذخیره نمی‌شه، فقط اسم */
    suspend fun registerUser(thread: String, userName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configured) error("پشتیبانی هنوز تنظیم نشده.")
                val payload = JSONObject().put("thread", thread).put("user_name", userName.take(80))
                client.newCall(
                    req(usersBase() + "?on_conflict=thread")
                        .header("Prefer", "resolution=merge-duplicates,return=minimal")
                        .post(payload.toString().toRequestBody(json)).build()
                ).execute().use { if (!it.isSuccessful) error("ثبت نشد (${it.code})") }
            }
        }

    /** برای پشتیبان: همه‌ی کاربرهای ثبت‌نام‌شده */
    suspend fun users(): Result<List<SupportUser>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configured) error("پشتیبانی هنوز تنظیم نشده.")
                val url = usersBase() + "?order=created_at.desc&limit=1000"
                client.newCall(req(url).get().build()).execute().use { r ->
                    if (!r.isSuccessful) error("دریافت نشد (${r.code})")
                    val arr = JSONArray(r.body?.string().orEmpty())
                    List(arr.length()) { i ->
                        val o = arr.getJSONObject(i)
                        SupportUser(o.getString("thread"), o.optString("user_name"))
                    }
                }
            }
        }
}
