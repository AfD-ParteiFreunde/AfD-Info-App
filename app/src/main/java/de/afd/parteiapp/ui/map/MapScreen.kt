package de.afd.parteiapp.ui.map

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import de.afd.parteiapp.R
import de.afd.parteiapp.data.AppleMapSnapshot
import de.afd.parteiapp.data.EventGeoRepository
import de.afd.parteiapp.data.FraktionEvent
import de.afd.parteiapp.data.FraktionEventsRepository
import de.afd.parteiapp.data.Prefs
import de.afd.parteiapp.ui.components.openUrl
import de.afd.parteiapp.ui.components.openUrlInPreferredBrowser
import de.afd.parteiapp.ui.glass.GlassButton
import de.afd.parteiapp.ui.glass.GlassIconButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

private data class NearestEvent(val event: FraktionEvent, val km: Double)

@Composable
fun MapScreen(initial: FraktionEvent, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()
    val density = LocalDensity.current

    val events = remember { FraktionEventsRepository.loadBest(context).events }
    val mapProvider = remember { Prefs(context).mapProvider }
    var geo by remember { mutableStateOf(EventGeoRepository.loadCache(context)) }
    var geocodeDone by remember { mutableStateOf(false) }
    var pageReady by remember { mutableStateOf(false) }
    val webHolder = remember { arrayOfNulls<WebView>(1) }

    var plz by rememberSaveable { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var plzError by remember { mutableStateOf(false) }
    var nearest by remember { mutableStateOf<NearestEvent?>(null) }
    var plzPoint by remember { mutableStateOf<de.afd.parteiapp.data.GeoPoint?>(null) }

    var appleCenter by remember { mutableStateOf(51.163 to 10.447) }
    var appleSpan by remember { mutableStateOf(6.0) }
    var appleUrl by remember { mutableStateOf<String?>(null) }
    var appleLoading by remember { mutableStateOf(false) }

    LaunchedEffect(mapProvider, appleCenter, appleSpan) {
        if (mapProvider != "apple") return@LaunchedEffect
        appleLoading = true
        appleUrl = AppleMapSnapshot.resolve(appleCenter.first, appleCenter.second, appleSpan)
        appleLoading = false
    }

    LaunchedEffect(Unit) {
        val venues = events.map { it.venue }.filter { it.isNotBlank() }
        val updated = EventGeoRepository.ensure(context, venues) { venue, point ->
            geo = geo + (venue to point)
        }
        geo = updated
        geocodeDone = true
    }

    LaunchedEffect(geo, geocodeDone) {
        if (mapProvider != "apple") return@LaunchedEffect
        val point = geo[initial.venue]?.takeIf { it.valid } ?: return@LaunchedEffect
        appleCenter = point.lat to point.lon
        appleSpan = 4.5
    }

    LaunchedEffect(pageReady, geocodeDone) {
        if (!pageReady) return@LaunchedEffect
        if (mapProvider == "apple") return@LaunchedEffect
        val webView = webHolder[0] ?: return@LaunchedEffect
        val array = JSONArray()
        events.forEach { event ->
            val point = geo[event.venue]
            if (point != null && point.valid) {
                array.put(
                    JSONObject().apply {
                        put("k", event.venue)
                        put("t", event.title)
                        put("d", event.datetime)
                        put("v", event.venue)
                        put("lat", point.lat)
                        put("lon", point.lon)
                    }
                )
            }
        }
        webView.evaluateJavascript(
            "setEvents($array, ${JSONObject.quote(initial.venue)}, $geocodeDone)",
            null,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webHolder[0]?.let { wv ->
                (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
            webHolder[0] = null
        }
    }

    fun searchPlz() {
        val query = plz.trim()
        if (query.length != 5 || searching) return
        scope.launch {
            searching = true
            plzError = false
            val result = EventGeoRepository.geocodePlz(query)
            if (result == null) {
                plzError = true
                nearest = null
                plzPoint = null
            } else {
                val (point, label) = result
                plzPoint = point
                val best = events.mapNotNull { event ->
                    val eventPoint = geo[event.venue]
                    if (eventPoint != null && eventPoint.valid) {
                        event to EventGeoRepository.distanceKm(point.lat, point.lon, eventPoint.lat, eventPoint.lon)
                    } else {
                        null
                    }
                }.minByOrNull { it.second }
                if (best == null) {
                    plzError = true
                    nearest = null
                } else {
                    nearest = NearestEvent(best.first, best.second)
                    if (mapProvider == "apple") {
                        appleSpan = 0.36
                        appleCenter = point.lat to point.lon
                    } else {
                        webHolder[0]?.evaluateJavascript(
                            "selectNearest(${point.lat}, ${point.lon}, ${JSONObject.quote(label)}, ${JSONObject.quote(best.first.venue)})",
                            null,
                        )
                    }
                }
            }
            searching = false
        }
    }

    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val iconRowHeight = with(density) { 30.dp.roundToPx() }
    val topPadding = with(density) { minOf(statusBarTop, iconRowHeight).toDp() + 8.dp }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = topPadding)
                        .height(46.dp)
                        .padding(start = 4.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.wahlkampf_map_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = plz,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() }) plz = value
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.wahlkampf_map_plz_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.width(10.dp))
                    GlassButton(
                        onClick = { if (plz.length == 5 && !searching) searchPlz() },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(stringResource(R.string.wahlkampf_map_search))
                    }
                }
                val status = when {
                    searching -> stringResource(R.string.wahlkampf_map_searching)
                    plzError -> stringResource(R.string.wahlkampf_map_plz_invalid)
                    nearest != null -> stringResource(R.string.wahlkampf_map_nearest, nearest!!.event.title) +
                        " · " + stringResource(
                        R.string.wahlkampf_map_distance,
                        formatKm(nearest!!.km),
                    )
                    else -> null
                }
                if (status != null) {
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (plzError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val target = plzPoint ?: nearest?.event?.venue?.let { geo[it] }?.takeIf { it.valid }
                if (target != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(
                            onClick = {
                                openUrlInPreferredBrowser(
                                    context,
                                    "https://www.google.com/maps/search/?api=1&query=${target.lat},${target.lon}",
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.wahlkampf_map_open_google))
                        }
                        GlassButton(
                            onClick = {
                                openUrlInPreferredBrowser(
                                    context,
                                    "https://maps.apple.com/?ll=${target.lat},${target.lon}&q=${target.lat},${target.lon}",
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.wahlkampf_map_open_apple))
                        }
                    }
                }
            }
            if (mapProvider == "apple") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val placeholderColor = if (dark) 0xFF061A2B.toInt() else 0xFFDCEDFB.toInt()
                    if (appleUrl != null) {
                        AsyncImage(
                            model = appleUrl,
                            contentDescription = stringResource(R.string.wahlkampf_map_title),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color(placeholderColor)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (appleLoading) CircularProgressIndicator()
                        }
                    }
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GlassIconButton(
                            onClick = { appleSpan = (appleSpan / 1.6).coerceAtLeast(0.02) },
                            content = {
                                Icon(Icons.Outlined.Add, stringResource(R.string.wahlkampf_map_zoom_in))
                            },
                        )
                        GlassIconButton(
                            onClick = { appleSpan = (appleSpan * 1.6).coerceAtMost(400.0) },
                            content = {
                                Icon(Icons.Outlined.Remove, stringResource(R.string.wahlkampf_map_zoom_out))
                            },
                        )
                    }
                    if (appleUrl == null && !appleLoading) {
                        Text(
                            stringResource(R.string.wahlkampf_map_load_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                        )
                    }
                    GlassButton(
                        onClick = {
                            val c = plzPoint ?: nearest?.event?.venue?.let { geo[it] }?.takeIf { it.valid }
                            val lat = c?.lat ?: appleCenter.first
                            val lon = c?.lon ?: appleCenter.second
                            openUrlInPreferredBrowser(
                                context,
                                "https://maps.apple.com/?ll=$lat,$lon&q=$lat,$lon",
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp, start = 12.dp, end = 12.dp),
                    ) {
                        Text(stringResource(R.string.wahlkampf_map_open_apple))
                    }
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(if (dark) 0xFF061A2B.toInt() else 0xFFDCEDFB.toInt())
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String) {
                                    pageReady = true
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: android.webkit.WebResourceRequest,
                                    error: android.webkit.WebResourceError,
                                ) {
                                    Log.d("MapWebView", "ERR ${request.url}: ${error.description}")
                                }

                                override fun onReceivedHttpError(
                                    view: WebView,
                                    request: android.webkit.WebResourceRequest,
                                    errorResponse: android.webkit.WebResourceResponse,
                                ) {
                                    Log.d("MapWebView", "HTTP ${errorResponse.statusCode} ${request.url}")
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                                    Log.d("MapWebView", msg.message() + " @" + msg.lineNumber())
                                    return true
                                }
                            }
                            webHolder[0] = this
                            loadUrl(
                                "file:///android_asset/map.html?dark=" + (if (dark) "1" else "0") +
                                    "&provider=" + mapProvider,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

private fun formatKm(km: Double): String =
    if (km < 10.0) String.format(Locale.GERMANY, "%.1f", km) else km.roundToInt().toString()
