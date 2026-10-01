package de.afd.parteiapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

data class ShopProduct(
    val name: String,
    val price: String,
    val imageUrl: String?,
    val url: String,
)

data class ShopResult(
    val products: List<ShopProduct>,
    val categories: List<String>,
    val live: Boolean,
    val fetchedAt: Long,
    val error: String? = null,
)

object ShopRepository {

    private const val BASE = Links.SHOP

    fun loadBundled(context: Context): ShopResult = runCatching { parseBundled(context) }
        .getOrDefault(ShopResult(emptyList(), emptyList(), live = false, fetchedAt = 0L))

    private fun parseBundled(context: Context): ShopResult {
        val text = context.assets.open("merch.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(text)
        val products = root.optJSONArray("products")?.let { array ->
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                ShopProduct(
                    name = obj.optString("name"),
                    price = obj.optString("price"),
                    imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                    url = obj.optString("url"),
                )
            }
        } ?: emptyList()
        val categories = root.optJSONArray("categories")?.let { array ->
            (0 until array.length()).map { array.optString(it) }
        } ?: emptyList()
        return ShopResult(products, categories, live = false, fetchedAt = 0L)
    }

    suspend fun refresh(context: Context): ShopResult = withContext(Dispatchers.IO) {
        val fallback = loadBundled(context)
        try {
            val html = Http.get(BASE)
            if (html.contains("Just a moment", true) || html.contains("Attention Required", true)) {
                throw IOException("Blocked by shop protection")
            }
            val doc = Jsoup.parse(html, BASE)

            val productElements = doc.select("li.product-item, .product-item, .product-item-info")
            val products = productElements.mapNotNull { element ->
                val link = element.selectFirst("a.product-item-link")
                    ?: element.selectFirst(".product-name a")
                    ?: element.selectFirst("a")
                val name = link?.text()?.trim().orEmpty()
                    .ifBlank { link?.attr("title")?.trim().orEmpty() }
                if (name.length < 3) return@mapNotNull null
                val price = element.selectFirst(".special-price .price, .price")
                    ?.text()?.trim().orEmpty()
                val img = element.selectFirst("img")?.let { image ->
                    image.attr("src").ifBlank { image.attr("data-src") }
                }.orEmpty()
                ShopProduct(
                    name = name,
                    price = price,
                    imageUrl = img.ifBlank { null },
                    url = link?.absUrl("href").orEmpty().ifBlank { Links.SHOP },
                )
            }.distinctBy { it.name }.take(10)

            val categories = (
                doc.select("nav.navigation li.level0 > a, .nav-sections li > a").map { it.text().trim() } +
                    doc.select("nav.navigation a, .nav-sections a").map { it.text().trim() }
                )
                .filter { it.length in 3..30 }
                .distinct()
                .take(12)

            if (products.isEmpty() && categories.isEmpty()) throw IOException("No content parsed")
            ShopResult(
                products = products.ifEmpty { fallback.products },
                categories = categories.ifEmpty { fallback.categories },
                live = true,
                fetchedAt = System.currentTimeMillis(),
            )
        } catch (t: Throwable) {
            fallback.copy(error = t.message ?: "error")
        }
    }
}
