package de.afd.parteiapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object AppleMapSnapshot {

    private const val UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"

    private val SNAPSHOT =
        Regex("""https://snapshot\.apple-mapkit\.com/api/v1/snapshot\?[^"'\s<>]+""")

    suspend fun resolve(centerLat: Double, centerLon: Double, span: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val center = "${trimNum(centerLat)},${trimNum(centerLon)}"
                val spn = "${span},${span * 1.6}"
                val url = "https://maps.apple.com/frame?center=$center&span=$spn"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", UA)
                    .header("Accept", "text/html")
                    .build()
                Http.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    SNAPSHOT.find(body)?.value?.replace("&amp;", "&")
                }
            } catch (t: Throwable) {
                null
            }
        }

    private fun trimNum(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
