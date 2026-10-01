package de.afd.parteiapp.data

import android.content.Context
import org.json.JSONArray

object FeedSources {

    fun load(context: Context): List<FeedSource> = runCatching {
        val text = context.assets.open("news_feeds.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(text)
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            FeedSource(
                id = obj.optString("id"),
                name = obj.optString("name"),
                url = obj.optString("url"),
                color = obj.optString("color", "#009EE0"),
                enabledByDefault = obj.optBoolean("enabled", true),
            )
        }
    }.getOrDefault(emptyList())
}
