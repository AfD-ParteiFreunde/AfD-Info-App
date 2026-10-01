package de.afd.parteiapp.ui.wahlkampf

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.GlassButton
import de.afd.parteiapp.ui.polls.StatePollsContent
import de.afd.parteiapp.ui.glass.GlassIconButton
import de.afd.parteiapp.ui.glass.LocalGlassNavPadding
import de.afd.parteiapp.data.ElectionDate
import de.afd.parteiapp.data.ElectionsRepository
import de.afd.parteiapp.data.ElectionsResult
import de.afd.parteiapp.data.EventGeoRepository
import de.afd.parteiapp.data.EventsResult
import de.afd.parteiapp.data.FraktionEvent
import de.afd.parteiapp.data.FraktionEventsRepository
import de.afd.parteiapp.data.Links
import de.afd.parteiapp.data.PartyPoll
import de.afd.parteiapp.data.PollResult
import de.afd.parteiapp.data.PollsRepository
import de.afd.parteiapp.data.upcoming
import de.afd.parteiapp.ui.components.BrandTopBar
import de.afd.parteiapp.ui.components.LinkCard
import de.afd.parteiapp.ui.components.SectionCard
import de.afd.parteiapp.ui.components.rememberBottomShadow
import de.afd.parteiapp.ui.components.countdownText
import de.afd.parteiapp.ui.components.formatDateLong
import de.afd.parteiapp.ui.components.openUrl
import de.afd.parteiapp.ui.components.relativeTime
import de.afd.parteiapp.ui.theme.AfDBlue
import de.afd.parteiapp.ui.theme.AfDDisplayFont
import de.afd.parteiapp.ui.theme.AfDNavy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private fun typeColor(type: String): Color = when (type) {
    "bund" -> Color(0xFF0B6FA8)
    "land" -> Color(0xFF009EE0)
    "europa" -> Color(0xFFE2001A)
    else -> Color(0xFF2A9D8F)
}

private fun typeLabel(type: String): Int = when (type) {
    "bund" -> R.string.wahlkampf_type_bund
    "land" -> R.string.wahlkampf_type_land
    "europa" -> R.string.wahlkampf_type_europa
    else -> R.string.wahlkampf_type_kommunal
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WahlkampfScreen(
    onOpenSupport: () -> Unit,
    onOpenMap: (FraktionEvent) -> Unit,
    onOpenStatePolls: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialElections = remember { ElectionsRepository.loadBest(context) }
    val initialEvents = remember { FraktionEventsRepository.loadBest(context) }
    var result: ElectionsResult by remember { mutableStateOf(initialElections) }
    var events: EventsResult by remember { mutableStateOf(initialEvents) }
    var poll: PollResult? by remember { mutableStateOf(PollsRepository.loadBest(context)) }
    var refreshing by remember { mutableStateOf(false) }
    var datesExpanded by rememberSaveable { mutableStateOf(false) }
    var eventsExpanded by rememberSaveable { mutableStateOf(false) }
    var pollsTab by rememberSaveable { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val pollsVisible by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.find { it.key == "polls" }
                ?: return@derivedStateOf false
            val visible = (minOf(item.offset + item.size, info.viewportEndOffset) -
                maxOf(item.offset, info.viewportStartOffset)).coerceAtLeast(0)
            item.size > 0 && visible >= item.size * 0.5f
        }
    }
    val dark = isSystemInDarkTheme()

    fun refresh() {
        scope.launch {
            refreshing = true
            result = ElectionsRepository.refresh(context)
            events = FraktionEventsRepository.refresh(context)
            poll = PollsRepository.refresh(context)
            refreshing = false
            val venues = events.events.map { it.venue }.filter { it.isNotBlank() }
            if (venues.isNotEmpty()) {
                scope.launch { EventGeoRepository.ensure(context, venues) }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val upcomingElections = remember(result) { result.elections.upcoming() }
    val next = remember(upcomingElections) { upcomingElections.firstOrNull { it.date != null } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrandTopBar(
                title = stringResource(R.string.wahlkampf_title),
                actions = {
                    GlassIconButton(onClick = { refresh() }, size = 32.dp) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.news_refresh), Modifier.size(20.dp))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .then(rememberBottomShadow()),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 16.dp + LocalGlassNavPadding.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Box(
                        Modifier.background(
                            Brush.linearGradient(listOf(AfDBlue, AfDNavy)),
                        ),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                stringResource(R.string.wahlkampf_next).uppercase(),
                                color = Color(0xFFBFE3F7),
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.5.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            if (next != null) {
                                Text(
                                    next.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatDateLong(next.dateIso),
                                    color = Color(0xFFD7EAF7),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    countdownText(next.dateIso),
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.wahlkampf_all_dates),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(stringResource(R.string.wahlkampf_dates)) {
                    Text(
                        text = if (result.live) {
                            stringResource(R.string.wahlkampf_live, relativeTime(result.fetchedAt))
                        } else {
                            stringResource(R.string.wahlkampf_offline)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (result.error != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.common_stale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val shown = if (datesExpanded) upcomingElections else upcomingElections.take(3)
                        shown.forEach { election ->
                            ElectionRow(election, dark)
                        }
                    }
                    if (upcomingElections.size > 3) {
                        MoreToggle(expanded = datesExpanded) { datesExpanded = !datesExpanded }
                    }
                }
            }

            item {
                LinkCard(
                    icon = Icons.Outlined.OpenInNew,
                    title = stringResource(R.string.wahlkampf_all_dates),
                    subtitle = stringResource(R.string.wahlkampf_source),
                ) { openUrl(context, Links.ELECTION_DATES) }
            }

            item(key = "polls") {
                SectionCard(stringResource(R.string.wahlkampf_polls)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PollTab(
                            label = stringResource(R.string.polls_bundestag),
                            selected = pollsTab == 0,
                            modifier = Modifier.weight(1f),
                        ) { pollsTab = 0 }
                        PollTab(
                            label = stringResource(R.string.polls_landtag),
                            selected = pollsTab == 1,
                            modifier = Modifier.weight(1f),
                        ) { pollsTab = 1 }
                    }
                    Spacer(Modifier.height(14.dp))

                    if (pollsTab == 0) {
                        poll?.let { currentPoll ->
                            Text(
                                text = if (currentPoll.fetchedAt > 0L) {
                                    stringResource(R.string.wahlkampf_live, relativeTime(currentPoll.fetchedAt))
                                } else {
                                    stringResource(R.string.wahlkampf_offline)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(14.dp))
                            PollChart(currentPoll.parties, dark, pollsVisible)
                            Spacer(Modifier.height(20.dp))
                            Text(
                                stringResource(R.string.wahlkampf_seats_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(10.dp))
                            SeatDiagram(currentPoll.parties, dark, pollsVisible)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                pollSourceLine(currentPoll),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            GlassButton(
                                onClick = { openUrl(context, Links.POLLS) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.wahlkampf_polls_all))
                            }
                        }
                    } else {
                        StatePollsContent(scroll = false)
                    }
                }
            }

            if (events.events.isNotEmpty()) {
                item {
                    SectionCard(stringResource(R.string.wahlkampf_events)) {
                        Text(
                            text = if (events.live) {
                                stringResource(R.string.events_live, relativeTime(events.fetchedAt))
                            } else {
                                stringResource(R.string.events_offline)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (events.error != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.common_stale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val shown = if (eventsExpanded) events.events else events.events.take(2)
                            shown.forEach { event ->
                                FraktionEventCard(event, onOpenMap = { onOpenMap(event) })
                            }
                        }
                        if (events.events.size > 2) {
                            MoreToggle(expanded = eventsExpanded) { eventsExpanded = !eventsExpanded }
                        }
                    }
                }
            }

            item {
                LinkCard(
                    icon = Icons.Outlined.CalendarMonth,
                    title = stringResource(R.string.wahlkampf_events_all),
                    subtitle = "afdbundestag.de",
                ) { openUrl(context, Links.FRAKTION_EVENTS) }
            }
            item {
                LinkCard(
                    icon = Icons.Outlined.Campaign,
                    title = stringResource(R.string.events_fraktion),
                    subtitle = stringResource(R.string.events_fraktion_sub),
                ) { openUrl(context, Links.FRAKTION) }
            }
            item {
                LinkCard(
                    icon = Icons.Outlined.Public,
                    title = stringResource(R.string.events_land),
                    subtitle = stringResource(R.string.events_land_sub),
                ) { openUrl(context, Links.STATE_ASSOCIATIONS) }
            }

            item {
                SectionCard(stringResource(R.string.wahlkampf_support)) {
                    Text(
                        stringResource(R.string.wahlkampf_support_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton(onClick = onOpenSupport) {
                            Text(stringResource(R.string.donate_title))
                        }
                        GlassButton(
                            onClick = { openUrl(context, Links.JOIN) },
                            tint = Color(0x1FFFFFFF),
                        ) {
                            Text(stringResource(R.string.donate_join))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FraktionEventCard(event: FraktionEvent, onOpenMap: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openUrl(context, event.url) },
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!event.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (event.datetime.isNotBlank()) {
                    Text(
                        event.datetime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (event.venue.isNotBlank()) {
                    Text(
                        event.venue,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onOpenMap) {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = stringResource(R.string.wahlkampf_map_button),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ElectionRow(election: ElectionDate, dark: Boolean) {
    val color = typeColor(election.type)
    val textColor = if (dark) lerp(color, Color.White, 0.45f) else color
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = color.copy(alpha = 0.16f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(68.dp),
        ) {
            Text(
                stringResource(typeLabel(election.type)),
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                election.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val detail = if (election.date != null) {
                    formatDateLong(election.dateIso)
                } else {
                    election.period
                }
                val suffix = if (!election.confirmed) {
                    " · " + stringResource(R.string.wahlkampf_expected)
                } else {
                    ""
                }
                Text(
                    detail + suffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (election.date != null) {
                    Text(
                        " · " + countdownText(election.dateIso),
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreToggle(expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (expanded) R.string.common_show_less else R.string.common_show_more),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun partyColor(party: String): Color = when (party) {
    "AfD" -> AfDBlue
    "CDU/CSU" -> Color(0xFF15181C)
    "SPD" -> Color(0xFFE3000F)
    "GRÜNE" -> Color(0xFF46962B)
    "DIE LINKE" -> Color(0xFFBE3075)
    "BSW" -> Color(0xFF8E1B3A)
    "FDP" -> Color(0xFFFFED00)
    else -> Color(0xFF6E7B8A)
}

private fun partyDisplayName(party: String): String = when (party) {
    "CDU/CSU" -> "CDU"
    "GRÜNE" -> "Grüne"
    "DIE LINKE" -> "Linke"
    else -> party
}

private fun formatPercent(value: Double): String {
    val number = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(java.util.Locale.GERMANY, "%.1f", value)
    }
    return "$number%"
}

@Composable
private fun pollSourceLine(poll: PollResult): String {
    val year = Regex("\\d{4}").find(poll.published)?.value.orEmpty()
    val period = when {
        poll.period.isBlank() -> poll.published
        year.isBlank() -> poll.period
        else -> poll.period.trimEnd('.') + "." + year
    }
    val line = stringResource(R.string.wahlkampf_polls_line, poll.institute, period)
    return if (poll.sample.isBlank()) {
        line
    } else {
        line + " · " + stringResource(R.string.wahlkampf_polls_sample, poll.sample)
    }
}

@Composable
private fun PollChart(parties: List<PartyPoll>, dark: Boolean, visible: Boolean) {
    val top = remember(parties) {
        parties.filter { !it.party.equals("Sonstige", true) }
            .sortedByDescending { it.percent }
            .take(5)
    }
    if (top.isEmpty()) return
    val max = top.first().percent
    if (max <= 0.0) return
    val barArea = 148.dp
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        top.forEachIndexed { index, party ->
            val base = partyColor(party.party)
            val color = if (dark) lerp(base, Color.White, 0.32f) else base
            val onColor = if (color.luminance() > 0.55f) Color(0xFF11161C) else Color.White
            val isAfD = party.party.equals("AfD", true)
            val barColor = if (isAfD) color else color.copy(alpha = 0.72f)
            val fraction = (party.percent / max).toFloat().coerceIn(0.04f, 1f)
            val rise = remember { Animatable(0f) }
            LaunchedEffect(parties, visible) {
                if (!visible) return@LaunchedEffect
                rise.animateTo(
                    targetValue = fraction,
                    animationSpec = tween(
                        durationMillis = 700,
                        delayMillis = (top.size - 1 - index) * 90,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            val barHeight = rise.value
            val labelAlpha = (barHeight / fraction).coerceIn(0f, 1f)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barArea),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barColor),
                    )
                    if (fraction > 0.3f) {
                        Text(
                            formatPercent(party.percent),
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .alpha(labelAlpha),
                            color = if (fraction >= 0.99f) onColor else MaterialTheme.colorScheme.onSurface,
                            fontFamily = AfDDisplayFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                        )
                    } else {
                        Text(
                            formatPercent(party.percent),
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = barArea * fraction + 6.dp)
                                .alpha(labelAlpha),
                            color = color,
                            fontFamily = AfDDisplayFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        partyDisplayName(party.party),
                        Modifier.padding(vertical = 4.dp),
                        color = onColor,
                        fontFamily = AfDDisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private const val BUNDESTAG_SEATS = 630

private fun seatAllocation(parties: List<PartyPoll>): List<Pair<String, Int>> {
    val qualifying = parties.filter { !it.party.equals("Sonstige", true) && it.percent >= 5.0 }
    val sum = qualifying.sumOf { it.percent }
    if (qualifying.isEmpty() || sum <= 0.0) return emptyList()
    val exact = qualifying.map { it.percent / sum * BUNDESTAG_SEATS }
    val seats = exact.map { it.toInt() }.toMutableList()
    val remaining = BUNDESTAG_SEATS - seats.sum()
    if (remaining > 0) {
        exact.indices.sortedByDescending { exact[it] - seats[it] }
            .take(remaining)
            .forEach { seats[it]++ }
    }
    return qualifying.mapIndexed { index, party -> party.party to seats[index] }
}

private fun seatOrderIndex(party: String): Int = when (party) {
    "DIE LINKE" -> 0
    "BSW" -> 1
    "SPD" -> 2
    "GRÜNE" -> 3
    "FDP" -> 4
    "CDU/CSU" -> 5
    "AfD" -> 7
    else -> 4
}

@Composable
private fun PollTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tint = if (selected) AfDBlue else Color(0x1A0A2A43),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Text(
            label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class SeatDot(val x: Float, val y: Float, val isAfd: Boolean, val reveal: Float)

@Composable
private fun SeatDiagram(parties: List<PartyPoll>, dark: Boolean, visible: Boolean) {
    val allocation = remember(parties) { seatAllocation(parties) }
    if (allocation.isEmpty()) return
    val ordered = remember(allocation) { allocation.sortedBy { seatOrderIndex(it.first) } }
    val afdSeats = ordered.firstOrNull { it.first.equals("AfD", true) }?.second ?: 0
    val othersSeats = BUNDESTAG_SEATS - afdSeats
    val afdBase = partyColor("AfD")
    val afdColor = if (dark) lerp(afdBase, Color.White, 0.32f) else afdBase

    val afdReveal = remember(parties) {
        val rnd = java.util.Random(42L)
        FloatArray(BUNDESTAG_SEATS) { rnd.nextFloat() }
    }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(parties, dark, visible) {
        if (!visible) {
            pop.snapTo(0f)
            return@LaunchedEffect
        }
        pop.snapTo(0f)
        pop.animateTo(
            1f,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        )
    }
    val greyColor = if (dark) Color(0xFFA9B4BC) else Color(0xFF8D979E)
    val lineColor = greyColor.copy(alpha = 0.45f)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth()) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .drawWithCache {
                        // Geometry is computed only when the size changes, not per frame.
                        val strip = 26.dp.toPx()
                        val cy = size.height - strip
                        val cx = size.width / 2f
                        val maxR = minOf(size.width / 2f, cy) - 12f
                        val inner = maxR * 0.45f
                        var spacing = 12f
                        for (s in 40 downTo 12) {
                            val rowCount = ((maxR - inner) / s).toInt() + 1
                            val capacity = (0 until rowCount).sumOf { i -> (PI * (inner + i * s) / s).toInt() }
                            if (capacity >= BUNDESTAG_SEATS) {
                                spacing = s.toFloat()
                                break
                            }
                        }
                        val rD = spacing * 0.38f
                        val rows = ((maxR - inner) / spacing).toInt() + 1
                        val radii = FloatArray(rows) { inner + it * spacing }
                        val caps = IntArray(rows) { (PI * radii[it] / spacing).toInt() }
                        val capSum = caps.sum().coerceAtLeast(1)
                        val counts = IntArray(rows) { (caps[it].toFloat() * BUNDESTAG_SEATS / capSum).toInt() }
                        val remain = BUNDESTAG_SEATS - counts.sum()
                        if (remain > 0) {
                            (0 until rows)
                                .sortedByDescending { caps[it].toFloat() * BUNDESTAG_SEATS / capSum - counts[it] }
                                .take(remain)
                                .forEach { counts[it]++ }
                        }
                        val dots = ArrayList<FloatArray>(BUNDESTAG_SEATS)
                        radii.forEachIndexed { rowIndex, radius ->
                            val count = counts[rowIndex]
                            for (j in 0 until count) {
                                val phi = (j + 0.5f) / count
                                val theta = PI.toFloat() * (1f - phi)
                                dots += floatArrayOf(
                                    phi,
                                    cx + radius * cos(theta),
                                    cy - radius * sin(theta),
                                )
                            }
                        }
                        dots.sortBy { it[0] }
                        // Precompute the drawing plan: one entry per seat, ordered.
                        val plan = ArrayList<SeatDot>(BUNDESTAG_SEATS)
                        var idx = 0
                        ordered.forEach { (party, count) ->
                            val isAfd = party.equals("AfD", true)
                            repeat(count) {
                                val dot = dots[idx]
                                plan += SeatDot(
                                    x = dot[1],
                                    y = dot[2],
                                    isAfd = isAfd,
                                    reveal = if (isAfd) afdReveal[idx] else 0f,
                                )
                                idx++
                            }
                        }
                        val axisX = cx
                        val axisTop = cy - maxR - rD
                        onDrawBehind {
                            drawLine(
                                color = lineColor,
                                start = Offset(axisX, cy),
                                end = Offset(axisX, axisTop),
                                strokeWidth = 1.2.dp.toPx(),
                            )
                            plan.forEach { d ->
                                if (!d.isAfd) {
                                    drawCircle(greyColor, radius = rD, center = Offset(d.x, d.y))
                                } else {
                                    val local = ((pop.value - d.reveal * 0.65f) / 0.35f).coerceIn(0f, 1f)
                                    if (local > 0f) {
                                        val eased = local * local * (3f - 2f * local)
                                        val overshoot = 1f + 0.25f * (1f - local) * local * 4f
                                        val r = rD * (0.35f + 0.65f * eased) * overshoot
                                        drawCircle(
                                            afdColor.copy(alpha = 0.3f + 0.7f * eased),
                                            radius = r,
                                            center = Offset(d.x, d.y),
                                        )
                                    }
                                }
                            }
                        }
                    },
            )
            Text(
                stringResource(R.string.wahlkampf_seats_count, BUNDESTAG_SEATS),
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(afdColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "AfD · " + stringResource(R.string.wahlkampf_seats_count, afdSeats),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = afdColor,
            )
            Spacer(Modifier.width(20.dp))
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(greyColor),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.wahlkampf_seats_others) + " · " +
                    stringResource(R.string.wahlkampf_seats_count, othersSeats),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
