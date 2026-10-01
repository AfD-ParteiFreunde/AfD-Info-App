package de.afd.parteiapp.ui.polls

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.afd.parteiapp.R
import de.afd.parteiapp.data.StatePoll
import de.afd.parteiapp.data.StatePollsRepository
import de.afd.parteiapp.ui.glass.GlassButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StatePollsScreen(onBack: () -> Unit) {
    val density = LocalDensity.current
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
                        stringResource(R.string.polls_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StatePollsContent()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StatePollsContent(scroll: Boolean = true) {
    val context = LocalContext.current
    var data by remember { mutableStateOf(StatePollsRepository.loadBest(context)) }
    var selected by remember { mutableStateOf<StatePoll?>(null) }
    val webHolder = remember { arrayOfNulls<WebView>(1) }
    val currentPolls = rememberUpdatedState(data.polls)

    LaunchedEffect(Unit) {
        data = withContext(Dispatchers.IO) { StatePollsRepository.refresh(context) }
    }

    DisposableEffect(Unit) {
        onDispose {
            webHolder[0]?.let { wv ->
                (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                wv.removeJavascriptInterface("StateMap")
                wv.destroy()
            }
            webHolder[0] = null
        }
    }

    fun formatPct(value: Double): String =
        String.format(java.util.Locale.GERMANY, "%.1f", value) + " %"

    val base = Modifier
        .fillMaxWidth()
        .padding(horizontal = if (scroll) 12.dp else 0.dp)
    Column(
        if (scroll) base.verticalScroll(rememberScrollState()) else base,
    ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            webViewClient = WebViewClient()
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onSelect(name: String) {
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            selected = currentPolls.value.firstOrNull { it.land == name }
                                        }
                                    }
                                },
                                "StateMap",
                            )
                            webHolder[0] = this
                            loadUrl("file:///android_asset/statemap.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp),
                )

                selected?.let { poll ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                poll.land,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    formatPct(poll.afd),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.polls_afd),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (poll.type == "election") {
                                    stringResource(R.string.polls_meta_election, poll.date)
                                } else {
                                    stringResource(R.string.polls_meta, poll.date, poll.next)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.polls_all_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                data.polls.sortedByDescending { it.afd }.forEach { poll ->
                    PollRow(poll, formatPct(poll.afd)) {
                        selected = poll
                        webHolder[0]?.evaluateJavascript(
                            "selectState(${org.json.JSONObject.quote(poll.land)})",
                            null,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.polls_source, data.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PollRow(poll: StatePoll, pct: String, onClick: () -> Unit) {
    GlassButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    poll.land,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        if (poll.type == "election") R.string.polls_tag_election else R.string.polls_tag_poll,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                pct,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
