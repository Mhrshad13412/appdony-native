package com.appdony.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * لینک «نصب کردن» رو از صفحه‌ی اصلی پست توی سایت پیدا می‌کنه.
 * - لینک واقعی: DownloadLink با url پر
 * - دکمه‌ی قفل: DownloadLink با url خالی (و عنوان LOCKED_TITLE)
 */
object InstallLinkFinder {

    const val LOCKED_TITLE = "نصب کردن"

    data class FindResult(
        val links: List<DownloadLink>,
        val debug: String
    )

    private const val SITE = "https://appdony.ir/"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .followRedirects(true)
            .build()
    }

    private val FILE_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z", "obb")
    private val BLOCKED_DOMAINS = listOf(
        "google.com", "doubleclick.net", "googlesyndication.com", "google-analytics.com", "facebook.com",
        "t.me", "telegram.me", "telegram.org", "twitter.com", "x.com", "pinterest.com",
        "youtube.com", "youtu.be", "aparat.com", "instagram.com", "whatsapp.com", "wa.me",
        "linkedin.com", "tiktok.com", "apple.com", "cafebazaar.ir", "myket.ir",
        "wikipedia.org", "reddit.com"
    )
    private val ALLOWED_HOSTS = setOf("drive.google.com", "docs.google.com")
    private val INSTALL_TEXT = Regex("نصب|دانلود|download|دریافت", RegexOption.IGNORE_CASE)
    private val DL_PATH = Regex("(^|/)(dl|download|downloads|getfile|file|files)(/|$)")
    private val SIZE_REGEX = Regex(
        "([0-9]+(?:\\.[0-9]+)?)\\s*(MB|GB|KB|مگابایت|گیگابایت|کیلوبایت)",
        RegexOption.IGNORE_CASE
    )
    private val SKIP_ANCESTORS = setOf("header", "footer", "nav", "aside")

    suspend fun find(postId: Int, postLink: String?): Result<FindResult> =
        withContext(Dispatchers.IO) {
            try {
                val pageUrl = postLink?.takeIf { it.toHttpUrlOrNull() != null } ?: "${SITE}?p=$postId"
                val request = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", UA)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "fa,en;q=0.8")
                    .build()
                val html = client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("کد ${resp.code}")
                    resp.body?.string().orEmpty()
                }
                Result.success(parse(html, pageUrl))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
