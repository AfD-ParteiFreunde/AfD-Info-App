package de.afd.parteiapp.ui.more

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import de.afd.parteiapp.ui.glass.GlassChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import de.afd.parteiapp.BuildConfig
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.LocalGlassNavPadding
import de.afd.parteiapp.data.FeedSources
import de.afd.parteiapp.data.Links
import de.afd.parteiapp.data.Prefs
import de.afd.parteiapp.ui.components.BrandLinkCard
import de.afd.parteiapp.ui.components.BrandTopBar
import de.afd.parteiapp.ui.components.LinkCard
import de.afd.parteiapp.ui.components.SectionCard
import de.afd.parteiapp.ui.components.rememberBottomShadow
import de.afd.parteiapp.ui.components.openUrl
import de.afd.parteiapp.ui.components.openUrlInPreferredBrowser
import de.afd.parteiapp.ui.news.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    newsViewModel: NewsViewModel,
    contactsCount: Int,
    glassNav: Boolean,
    onGlassNavChange: (Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenProgram: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val sources = remember { FeedSources.load(context) }
    var enabledIds by remember {
        mutableStateOf(prefs.enabledFeeds ?: sources.filter { it.enabledByDefault }.map { it.id }.toSet())
    }
    val currentLanguage = prefs.language ?: "de"
    var mapProvider by remember { mutableStateOf(prefs.mapProvider) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrandTopBar(title = stringResource(R.string.more_title))
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .then(rememberBottomShadow()),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 16.dp + LocalGlassNavPadding.current),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionCard(stringResource(R.string.more_language)) {
                    Text(
                        stringResource(R.string.more_language_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(
                            selected = currentLanguage == "de",
                            onClick = { switchLanguage(prefs, "de") },
                            label = { Text(stringResource(R.string.lang_de)) },
                        )
                        GlassChip(
                            selected = currentLanguage == "en",
                            onClick = { switchLanguage(prefs, "en") },
                            label = { Text(stringResource(R.string.lang_en)) },
                        )
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.more_settings)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.more_glass_nav),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(R.string.more_glass_nav_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = glassNav,
                            onCheckedChange = onGlassNavChange,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.more_map_provider),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.more_map_provider_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(
                            selected = mapProvider == "esri",
                            onClick = { mapProvider = "esri"; prefs.mapProvider = "esri" },
                            label = { Text(stringResource(R.string.map_provider_esri)) },
                        )
                        GlassChip(
                            selected = mapProvider == "osm",
                            onClick = { mapProvider = "osm"; prefs.mapProvider = "osm" },
                            label = { Text(stringResource(R.string.map_provider_osm)) },
                        )
                        GlassChip(
                            selected = mapProvider == "wikimedia",
                            onClick = { mapProvider = "wikimedia"; prefs.mapProvider = "wikimedia" },
                            label = { Text(stringResource(R.string.map_provider_wikimedia)) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip(
                            selected = mapProvider == "google",
                            onClick = {
                                mapProvider = "google"
                                prefs.mapProvider = "google"
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.map_provider_google_open),
                                    Toast.LENGTH_LONG,
                                ).show()
                                openUrlInPreferredBrowser(
                                    context,
                                    "https://www.google.com/maps/@51.163,10.447,7z",
                                )
                            },
                            label = { Text(stringResource(R.string.map_provider_google)) },
                        )
                        GlassChip(
                            selected = mapProvider == "apple",
                            onClick = {
                                mapProvider = "apple"
                                prefs.mapProvider = "apple"
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.map_provider_apple_open),
                                    Toast.LENGTH_LONG,
                                ).show()
                                openUrlInPreferredBrowser(
                                    context,
                                    "https://maps.apple.com/?ll=51.163,10.447&q=51.163,10.447",
                                )
                            },
                            label = { Text(stringResource(R.string.map_provider_apple)) },
                        )
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.more_sources)) {
                    Text(
                        stringResource(R.string.more_sources_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    sources.forEach { source ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(source.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    source.url.substringAfter("://").substringBefore("/"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = enabledIds.contains(source.id),
                                onCheckedChange = { checked ->
                                    enabledIds = if (checked) {
                                        enabledIds + source.id
                                    } else {
                                        enabledIds - source.id
                                    }
                                    prefs.enabledFeeds = enabledIds
                                    newsViewModel.reloadSources()
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.more_feeds_block_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                LinkCard(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.more_program),
                    subtitle = "afd.de",
                ) { onOpenProgram() }
            }
            item {
                LinkCard(
                    icon = Icons.Outlined.PersonAdd,
                    title = stringResource(R.string.more_join),
                    subtitle = "afd.de",
                ) { openUrl(context, Links.JOIN) }
            }
            item {
                LinkCard(
                    icon = Icons.Outlined.Favorite,
                    title = stringResource(R.string.more_donate_link),
                    subtitle = "afd.de",
                ) { openUrl(context, Links.DONATE) }
            }
            item {
                LinkCard(
                    icon = Icons.Outlined.Storefront,
                    title = stringResource(R.string.more_shop),
                    subtitle = "wir-lieben-deutschland.de",
                ) { openUrl(context, Links.SHOP) }
            }

            item {
                Text(
                    stringResource(R.string.more_social),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_x,
                    tint = if (isSystemInDarkTheme()) Color(0xFFE9E9E9) else Color(0xFF10141A),
                    title = "X (Twitter)",
                    subtitle = "@AfD",
                ) { openUrl(context, "https://twitter.com/AfD") }
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_youtube,
                    tint = Color(0xFFFF0000),
                    title = "YouTube",
                    subtitle = "AfD",
                ) { openUrl(context, "https://www.youtube.com/channel/UCq2rogaxLtQFrYG3X3KYNww") }
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_instagram,
                    tint = Color(0xFFE1306C),
                    title = "Instagram",
                    subtitle = "@afd.bund",
                ) { openUrl(context, "https://www.instagram.com/afd.bund") }
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_facebook,
                    tint = Color(0xFF1877F2),
                    title = "Facebook",
                    subtitle = "alternativefuerde",
                ) { openUrl(context, "https://de-de.facebook.com/alternativefuerde/") }
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_telegram,
                    tint = Color(0xFF2AABEE),
                    title = "Telegram",
                    subtitle = "@afdbrennpunkt (AfD)",
                ) { openUrl(context, "https://t.me/afdbrennpunkt") }
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_telegram,
                    tint = Color(0xFF2AABEE),
                    title = "Telegram",
                    subtitle = "@afdfraktionimbundestag (Fraktion)",
                ) { openUrl(context, "https://t.me/afdfraktionimbundestag") }
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_gettr,
                    tint = Color(0xFFDA2626),
                    title = "Gettr",
                    subtitle = "@AfD",
                ) { openUrl(context, "https://gettr.com/user/afd") }
            }
            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_social_tiktok,
                    tint = if (isSystemInDarkTheme()) Color(0xFFE9E9E9) else Color(0xFF10141A),
                    title = "TikTok",
                    subtitle = "@afdfraktionimbundestag (Fraktion)",
                ) { openUrl(context, "https://www.tiktok.com/@afdfraktionimbundestag") }
            }

            item {
                SectionCard(stringResource(R.string.more_about)) {
                    Text(
                        stringResource(R.string.more_about_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.more_stats, sources.size, contactsCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.more_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenPrivacy)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.PrivacyTip,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.more_privacy),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                stringResource(R.string.more_privacy_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            prefs.clearNewsCache()
                            Toast.makeText(
                                context,
                                context.getString(R.string.more_cache_cleared),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }) {
                            Icon(
                                Icons.Outlined.Description,
                                null,
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.more_clear_cache),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun switchLanguage(prefs: Prefs, code: String) {
    prefs.language = code
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
}
