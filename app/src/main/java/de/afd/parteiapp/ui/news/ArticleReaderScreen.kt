package de.afd.parteiapp.ui.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.afd.parteiapp.R
import de.afd.parteiapp.data.Article
import de.afd.parteiapp.data.ArticleExtractor
import de.afd.parteiapp.data.ReaderContent
import de.afd.parteiapp.ui.components.SourceBadge
import de.afd.parteiapp.ui.components.openUrl
import de.afd.parteiapp.ui.components.relativeTime
import de.afd.parteiapp.ui.components.shareText
import de.afd.parteiapp.ui.theme.sourceFonts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(article: Article, onBack: () -> Unit) {
    val context = LocalContext.current
    val fonts = sourceFonts(article.sourceId)
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf<ReaderContent?>(null) }

    LaunchedEffect(article.link) {
        loading = true
        failed = false
        runCatching { ArticleExtractor.load(article.link) }
            .onSuccess { content = it }
            .onFailure { failed = true }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        article.sourceName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareText(context, article.title, article.link) }) {
                        Icon(Icons.Outlined.Share, stringResource(R.string.reader_share))
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            failed -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.reader_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { openUrl(context, article.link) }) {
                    Text(stringResource(R.string.reader_open_original))
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val loaded = content
                if (loaded != null) {
                    if (!loaded.imageUrl.isNullOrBlank()) {
                        item {
                            AsyncImage(
                                model = loaded.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                            )
                        }
                    }
                    item {
                        Text(
                            loaded.title.ifBlank { article.title },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = fonts.title,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    item {
                        Column {
                            SourceBadge(article.sourceId, article.sourceName, article.sourceColor)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                relativeTime(article.publishedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                stringResource(R.string.reader_preview_note),
                                Modifier.padding(10.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(loaded.paragraphs) { paragraph ->
                        Text(
                            paragraph,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fonts.body),
                            lineHeight = 26.sp,
                        )
                    }
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 24.dp),
                        ) {
                            Button(
                                onClick = { openUrl(context, article.link) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.reader_open_original))
                            }
                        }
                    }
                }
            }
        }
    }
}
