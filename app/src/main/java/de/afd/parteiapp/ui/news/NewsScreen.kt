package de.afd.parteiapp.ui.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import de.afd.parteiapp.ui.glass.GlassChip
import de.afd.parteiapp.ui.glass.GlassIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.LocalGlassNavPadding
import de.afd.parteiapp.ui.glass.LocalGlassScrollTop
import de.afd.parteiapp.data.Article
import de.afd.parteiapp.ui.components.BrandTopBar
import de.afd.parteiapp.ui.components.SourceBadge
import de.afd.parteiapp.ui.components.rememberBottomShadow
import de.afd.parteiapp.ui.components.isVideoSource
import de.afd.parteiapp.ui.components.relativeTime
import de.afd.parteiapp.ui.theme.sourceFonts
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(viewModel: NewsViewModel, onOpenArticle: (Article) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtered = remember(state.articles, state.selectedSourceId) {
        val id = state.selectedSourceId
        if (id == null) state.articles else state.articles.filter { it.sourceId == id }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    val glassScrollTop = LocalGlassScrollTop.current

    LaunchedEffect(glassScrollTop, listState) {
        glassScrollTop?.action = { scope.launch { listState.animateScrollToItem(0) } }
    }
    LaunchedEffect(glassScrollTop, showScrollTop) {
        glassScrollTop?.visible = showScrollTop
    }
    DisposableEffect(glassScrollTop) {
        onDispose { glassScrollTop?.visible = false }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrandTopBar(
                title = stringResource(R.string.news_title),
                actions = {
                    GlassIconButton(onClick = viewModel::refresh, size = 32.dp) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.news_refresh), Modifier.size(20.dp))
                    }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (glassScrollTop == null) {
                AnimatedVisibility(
                    visible = showScrollTop,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    ) {
                        Icon(
                            Icons.Outlined.KeyboardArrowUp,
                            stringResource(R.string.news_to_top),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    GlassChip(
                        selected = state.selectedSourceId == null,
                        onClick = { viewModel.selectSource(null) },
                        label = { Text(stringResource(R.string.news_all)) },
                    )
                }
                items(state.sources, key = { it.id }) { source ->
                    GlassChip(
                        selected = state.selectedSourceId == source.id,
                        onClick = { viewModel.selectSource(source.id) },
                        label = { Text(source.name) },
                    )
                }
            }

            if (state.failedSources.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.news_sources_error, state.failedSources.size) +
                                " " + state.failedSources.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    de.afd.parteiapp.ui.components.EmptyState(
                        title = stringResource(R.string.news_empty),
                        hint = stringResource(R.string.news_empty_hint),
                    )
                }

                else -> PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(rememberBottomShadow()),
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp + LocalGlassNavPadding.current),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filtered, key = { it.link }) { article ->
                            NewsCard(article) { onOpenArticle(article) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(article: Article, onClick: () -> Unit) {
    val fonts = sourceFonts(article.sourceId)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column {
            if (!article.imageUrl.isNullOrBlank()) {
                Box {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                    if (isVideoSource(article.sourceId)) {
                        Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.PlayCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(14.dp)) {
                SourceBadge(article.sourceId, article.sourceName, article.sourceColor)
                Spacer(Modifier.height(8.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = fonts.title,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (article.summary.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        article.summary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fonts.body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    relativeTime(article.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
