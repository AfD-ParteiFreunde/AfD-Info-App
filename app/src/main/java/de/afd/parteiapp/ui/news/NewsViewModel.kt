package de.afd.parteiapp.ui.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.afd.parteiapp.data.Article
import de.afd.parteiapp.data.FeedSource
import de.afd.parteiapp.data.FeedSources
import de.afd.parteiapp.data.NewsRepository
import de.afd.parteiapp.data.Prefs
import de.afd.parteiapp.data.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class NewsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val articles: List<Article> = emptyList(),
    val sources: List<FeedSource> = emptyList(),
    val failedSources: List<String> = emptyList(),
    val lastUpdated: Long = 0L,
    val selectedSourceId: String? = null,
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository()
    private val prefs = Prefs(application)
    private val _state = MutableStateFlow(NewsUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { FeedSources.load(application) }
            val enabled = prefs.enabledFeeds
                ?: all.filter { it.enabledByDefault }.map { it.id }.toSet()
            val active = all.filter { enabled.contains(it.id) }
            val (cached, cachedTime) = withContext(Dispatchers.IO) { readCache() }
            _state.value = NewsUiState(
                loading = cached.isEmpty(),
                refreshing = cached.isNotEmpty(),
                articles = cached,
                sources = active,
                lastUpdated = cachedTime,
            )
            load(showSpinner = cached.isEmpty())
        }
    }

    fun refresh() = load(showSpinner = false)

    fun selectSource(id: String?) = _state.update { it.copy(selectedSourceId = id) }

    fun reloadSources() {
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { FeedSources.load(getApplication()) }
            val enabled = prefs.enabledFeeds
                ?: all.filter { it.enabledByDefault }.map { it.id }.toSet()
            val active = all.filter { enabled.contains(it.id) }
            _state.update { current ->
                val selected = current.selectedSourceId?.takeIf { id -> active.any { it.id == id } }
                current.copy(sources = active, selectedSourceId = selected)
            }
            load(showSpinner = false)
        }
    }

    private fun load(showSpinner: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = showSpinner && it.articles.isEmpty(),
                    refreshing = !showSpinner || it.articles.isNotEmpty(),
                )
            }
            val failures = mutableListOf<String>()
            val failedIds = mutableListOf<String>()
            val results = _state.value.sources.map { source ->
                async {
                    runCatching { repository.fetch(source) }
                        .onFailure {
                            failures += source.name
                            failedIds += source.id
                            android.util.Log.w("NewsVM", "feed failed: ${source.id} ${source.url}", it)
                        }
                        .getOrNull()
                }
            }.awaitAll().filterNotNull()

            val keepFromFailed = _state.value.articles.filter { it.sourceId in failedIds }
            val merged = (results.flatten() + keepFromFailed)
                .distinctBy { it.link }
                .sortedByDescending { it.publishedAt }

            if (merged.isNotEmpty()) withContext(Dispatchers.IO) { writeCache(merged) }

            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    failedSources = failures,
                    articles = if (merged.isNotEmpty()) merged else it.articles,
                    lastUpdated = if (merged.isNotEmpty()) System.currentTimeMillis() else it.lastUpdated,
                )
            }
        }
    }

    private fun readCache(): Pair<List<Article>, Long> {
        val raw = prefs.newsCache ?: return emptyList<Article>() to 0L
        return runCatching {
            val array = JSONArray(raw)
            val list = (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                Article(
                    title = obj.optString("title"),
                    link = obj.optString("link"),
                    summary = obj.optString("summary"),
                    imageUrl = RssParser.normalizeImageUrl(obj.optString("imageUrl").takeIf { it.isNotBlank() })
                        ?: RssParser.youTubeThumbFor(obj.optString("link")),
                    publishedAt = obj.optLong("publishedAt"),
                    sourceId = obj.optString("sourceId"),
                    sourceName = obj.optString("sourceName"),
                    sourceColor = obj.optString("sourceColor"),
                )
            }
            list to prefs.newsCacheTime
        }.getOrDefault(emptyList<Article>() to 0L)
    }

    private fun writeCache(articles: List<Article>) {
        val array = JSONArray()
        articles.take(500).forEach { article ->
            array.put(
                JSONObject().apply {
                    put("title", article.title)
                    put("link", article.link)
                    put("summary", article.summary)
                    put("imageUrl", article.imageUrl ?: "")
                    put("publishedAt", article.publishedAt)
                    put("sourceId", article.sourceId)
                    put("sourceName", article.sourceName)
                    put("sourceColor", article.sourceColor)
                }
            )
        }
        prefs.newsCache = array.toString()
        prefs.newsCacheTime = System.currentTimeMillis()
    }
}
