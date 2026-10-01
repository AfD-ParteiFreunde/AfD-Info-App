package de.afd.parteiapp.data

import android.content.Context

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("afd_app", Context.MODE_PRIVATE)

    var language: String?
        get() = sp.getString("language", null)
        set(value) {
            sp.edit().putString("language", value).apply()
        }

    var glassNav: Boolean
        get() = sp.getBoolean("glass_nav", true)
        set(value) {
            sp.edit().putBoolean("glass_nav", value).apply()
        }

    var privacyAccepted: Boolean
        get() = sp.getBoolean("privacy_accepted", false)
        set(value) {
            sp.edit().putBoolean("privacy_accepted", value).apply()
        }

    /** Map basemap provider: "esri" (default), "osm", "carto". */
    var mapProvider: String
        get() = sp.getString("map_provider", "esri") ?: "esri"
        set(value) {
            sp.edit().putString("map_provider", value).apply()
        }

    var landtagState: String?
        get() = sp.getString("landtag_state", null)
        set(value) {
            sp.edit().putString("landtag_state", value).apply()
        }

    var enabledFeeds: Set<String>?
        get() = sp.getStringSet("enabled_feeds", null)?.toSet()
        set(value) {
            sp.edit().putStringSet("enabled_feeds", value).apply()
        }

    var newsCache: String?
        get() = sp.getString("news_cache", null)
        set(value) {
            sp.edit().putString("news_cache", value).apply()
        }

    var newsCacheTime: Long
        get() = sp.getLong("news_cache_time", 0L)
        set(value) {
            sp.edit().putLong("news_cache_time", value).apply()
        }

    fun clearNewsCache() {
        sp.edit().remove("news_cache").remove("news_cache_time").apply()
    }
}
