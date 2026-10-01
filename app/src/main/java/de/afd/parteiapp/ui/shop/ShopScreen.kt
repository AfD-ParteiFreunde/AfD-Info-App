package de.afd.parteiapp.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.LocalGlassNavPadding
import de.afd.parteiapp.data.Links
import de.afd.parteiapp.data.ShopRepository
import de.afd.parteiapp.data.ShopResult
import de.afd.parteiapp.ui.components.SectionCard
import de.afd.parteiapp.ui.components.openUrl
import de.afd.parteiapp.ui.components.relativeTime
import de.afd.parteiapp.ui.theme.AfDRed
import kotlinx.coroutines.launch

@Composable
fun ShopContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = remember { ShopRepository.loadBundled(context) }
    var result: ShopResult by remember { mutableStateOf(initial) }
    var refreshing by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            refreshing = true
            result = ShopRepository.refresh(context)
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 16.dp + LocalGlassNavPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Box(
                    Modifier.background(
                        Brush.linearGradient(listOf(Color(0xFFE2001A), Color(0xFF8A0010))),
                    ),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            stringResource(R.string.shop_hero_title),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.shop_hero_text),
                            color = Color(0xFFFFE0E2),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { openUrl(context, Links.SHOP) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = AfDRed,
                            ),
                        ) {
                            Icon(Icons.Outlined.Storefront, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.shop_open))
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (result.live) {
                        stringResource(R.string.shop_live, relativeTime(result.fetchedAt))
                    } else {
                        stringResource(R.string.shop_offline)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (result.error != null) {
                    Text(
                        stringResource(R.string.common_stale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(2f),
                    )
                }
                IconButton(onClick = { refresh() }) {
                    Icon(
                        Icons.Outlined.Refresh,
                        stringResource(R.string.news_refresh),
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (result.categories.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.shop_highlights)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(result.categories) { category ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text(
                                    category,
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (result.products.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.shop_products)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.products.forEach { product ->
                            ProductRow(
                                name = product.name,
                                price = product.price,
                                imageUrl = product.imageUrl,
                                onClick = {
                                    openUrl(context, product.url.ifBlank { Links.SHOP })
                                },
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Text(
                    stringResource(R.string.shop_external_note),
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                stringResource(R.string.shop_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductRow(
    name: String,
    price: String,
    imageUrl: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Icon(
                    Icons.Outlined.Storefront,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (price.isNotBlank()) {
                Text(
                    price,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
