package de.afd.parteiapp.ui.program

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.afd.parteiapp.R
import de.afd.parteiapp.data.PdfHighlighter
import de.afd.parteiapp.data.WahlprogrammRepository
import de.afd.parteiapp.ui.glass.GlassChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WahlprogrammScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var data by remember { mutableStateOf<de.afd.parteiapp.data.WahlprogrammData?>(null) }
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    var highlights by remember { mutableStateOf<Map<Int, List<FloatArray>>>(emptyMap()) }
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        data = withContext(Dispatchers.IO) { WahlprogrammRepository.loadBest(context) }
        refreshing = true
        data = withContext(Dispatchers.IO) { WahlprogrammRepository.refresh(context) }
        refreshing = false
    }

    // Debounce typed input into the searched term.
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(400)
        submitted = query.trim()
    }

    // Recompute highlight rectangles whenever a new term is submitted.
    LaunchedEffect(submitted, data?.pdfFile) {
        val pdf = data?.pdfFile ?: return@LaunchedEffect
        highlights = if (submitted.length < 4) {
            emptyMap()
        } else {
            withContext(Dispatchers.IO) {
                PdfHighlighter.compute(context, pdf.absolutePath, submitted)
                    .associate { it.page to it.rects }
            }
        }
    }

    val hitPages = highlights.keys.sorted()

    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val iconRowHeight = with(density) { 30.dp.roundToPx() }
    val topPadding = with(density) { minOf(statusBarTop, iconRowHeight).toDp() + 8.dp }

    val doc = data

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = topPadding)
                        .height(46.dp)
                        .padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.program_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.program_search_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = ""; submitted = "" }) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WahlprogrammRepository.TOPICS.keys.toList()) { key ->
                        GlassChip(
                            selected = submitted.equals(key, ignoreCase = true),
                            onClick = {
                                query = key
                                submitted = key
                            },
                            label = { Text(key) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (refreshing) {
                        stringResource(R.string.program_loading)
                    } else {
                        stringResource(R.string.program_source, doc?.source.orEmpty())
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
            }

            if (doc == null) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (submitted.length >= 4) {
                    Text(
                        text = if (hitPages.isEmpty()) {
                            stringResource(R.string.program_no_results, submitted)
                        } else {
                            stringResource(R.string.program_results, hitPages.size, submitted)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    if (hitPages.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            items(hitPages) { page ->
                                GlassChip(
                                    selected = false,
                                    onClick = {
                                        val last = doc.pages.lastIndex
                                        if (last >= 0) {
                                            scope.launch {
                                                listState.animateScrollToItem(page.coerceIn(0, last))
                                            }
                                        }
                                    },
                                    label = { Text(stringResource(R.string.program_page, page + 1)) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(doc.pages.size) { index ->
                        ProgramPage(
                            pdfPath = doc.pdfFile.absolutePath,
                            index = index,
                            rects = highlights[index].orEmpty(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramPage(pdfPath: String, index: Int, rects: List<FloatArray>) {
    var bitmap by remember(pdfPath, index) {
        mutableStateOf(PageCache.get(pdfPath, index))
    }
    LaunchedEffect(pdfPath, index) {
        if (bitmap == null) {
            val bmp = withContext(Dispatchers.IO) { renderPage(pdfPath, index) }
            if (bmp != null) {
                PageCache.put(pdfPath, index, bmp)
                bitmap = bmp
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Surface(
            color = if (rects.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.program_page, index + 1),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        val base = bitmap
        if (base != null) {
            Box(Modifier.fillMaxWidth()) {
                Image(
                    bitmap = base.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (rects.isNotEmpty()) {
                    Canvas(Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        rects.forEach { r ->
                            drawRect(
                                color = Color(0x80FFEB3B),
                                topLeft = Offset(r[0] * w, r[1] * h),
                                size = Size(
                                    ((r[2] - r[0]) * w).coerceAtLeast(2f),
                                    ((r[3] - r[1]) * h).coerceAtLeast(2f),
                                ),
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private object PageCache {
    private const val MAX = 6
    private val map = object : LinkedHashMap<String, Bitmap>(MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > MAX
    }

    @Synchronized
    fun get(pdfPath: String, index: Int): Bitmap? = map["$pdfPath#$index"]

    @Synchronized
    fun put(pdfPath: String, index: Int, bitmap: Bitmap) {
        map["$pdfPath#$index"] = bitmap
    }
}

private fun renderPage(pdfPath: String, index: Int): Bitmap? {
    return runCatching {
        val pfd = ParcelFileDescriptor.open(
            java.io.File(pdfPath),
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
        PdfRenderer(pfd).use { renderer ->
            if (index !in 0 until renderer.pageCount) return@runCatching null
            renderer.openPage(index).use { page ->
                val width = 1000
                val height = (width.toFloat() / page.width * page.height).toInt()
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(AndroidColor.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        }
    }.getOrNull()
}
