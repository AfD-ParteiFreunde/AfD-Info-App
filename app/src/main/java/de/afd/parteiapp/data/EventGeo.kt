package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lon: Double) {
    val valid: Boolean get() = lat != 0.0 || lon != 0.0
}

object EventGeoRepository {

    private const val NOMINATIM = "https://nominatim.openstreetmap.org/search"
    private const val UA = "AfD-Parteiapp/1.0 (Android)"

    fun loadCache(context: Context): Map<String, GeoPoint> {
        val file = cacheFile(context)
        if (!file.exists()) return emptyMap()
        return runCatching {
            val root = JSONObject(file.readText())
            buildMap {
                root.keys().forEach { key ->
                    val array = root.optJSONArray(key) ?: return@forEach
                    put(key, GeoPoint(array.optDouble(0), array.optDouble(1)))
                }
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun ensure(
        context: Context,
        venues: List<String>,
        onProgress: ((String, GeoPoint) -> Unit)? = null,
    ): Map<String, GeoPoint> = withContext(Dispatchers.IO) {
        var cache = loadCache(context)
        // Failed lookups are NOT stored, so a venue is retried on a later run
        // once the network recovers.
        val missing = venues.map { it.trim() }.filter { it.isNotBlank() && it !in cache }.distinct()
        for (venue in missing) {
            val point = geocodeVenue(venue)
            if (point != null) {
                cache = cache + (venue to point)
                onProgress?.invoke(venue, point)
            }
            delay(2000)
        }
        saveCache(context, cache)
        cache
    }

    private suspend fun geocodeVenue(venue: String): GeoPoint? {
        for (query in venueCandidates(venue)) {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val point = nominatimSearch("q=$encoded&format=json&limit=1&countrycodes=de")
            if (point != null) return point
            delay(2000)
        }
        return null
    }

    private fun venueCandidates(venue: String): List<String> {
        val parts = venue.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val candidates = mutableListOf<String>()
        if (parts.size >= 3) candidates += parts.drop(1).joinToString(", ")
        candidates += venue
        if (parts.size >= 2) candidates += parts.takeLast(2).joinToString(", ")
        return candidates.distinct()
    }

    suspend fun geocodePlz(plz: String): Pair<GeoPoint, String>? {
        // Nominatim first: it returns a proper postcode centroid. zippopotam.us currently
        // serves corrupted coordinates for many German PLZ (e.g. latitude "02000"), so only
        // use it as a fallback and always validate the result.
        nominatimSearch("postalcode=$plz&country=Germany&format=json&limit=1")?.let { return it to plz }
        delay(1200)
        zippopotam(plz)?.let { return it }
        return null
    }

    private fun isValid(point: GeoPoint): Boolean =
        point.lat in -90.0..90.0 && point.lon in -180.0..180.0 &&
            !(point.lat == 0.0 && point.lon == 0.0)

    private suspend fun zippopotam(plz: String): Pair<GeoPoint, String>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.zippopotam.us/de/$plz")
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .build()
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val place = JSONObject(body).optJSONArray("places")?.optJSONObject(0) ?: return@withContext null
                val lat = place.optString("latitude").toDoubleOrNull() ?: return@withContext null
                val lon = place.optString("longitude").toDoubleOrNull() ?: return@withContext null
                val point = GeoPoint(lat, lon)
                if (!isValid(point)) return@withContext null
                point to place.optString("place name", plz)
            }
        } catch (t: Throwable) {
            null
        }
    }

    private suspend fun nominatimSearch(params: String): GeoPoint? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$NOMINATIM?$params")
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .build()
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val array = JSONArray(body)
                if (array.length() == 0) return@withContext null
                val obj = array.getJSONObject(0)
                val point = GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
                if (!isValid(point)) return@withContext null
                point
            }
        } catch (t: Throwable) {
            null
        }
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * 6371.0 * asin(sqrt(a))
    }

    private fun cacheFile(context: Context) = File(context.filesDir, "event_geo.json")

    private fun saveCache(context: Context, cache: Map<String, GeoPoint>) {
        runCatching {
            val root = JSONObject()
            cache.forEach { (venue, point) ->
                root.put(venue, JSONArray().put(point.lat).put(point.lon))
            }
            cacheFile(context).writeText(root.toString())
        }
    }
}
