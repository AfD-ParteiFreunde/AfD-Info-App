package de.afd.parteiapp.ui.contacts

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import de.afd.parteiapp.ui.glass.GlassButton
import de.afd.parteiapp.ui.glass.GlassChip
import de.afd.parteiapp.ui.glass.GlassIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.LocalGlassNavPadding
import de.afd.parteiapp.data.ContactMember
import de.afd.parteiapp.data.ContactsData
import de.afd.parteiapp.data.ContactsRepository
import de.afd.parteiapp.data.FraktionInfo
import de.afd.parteiapp.data.LandtagMember
import de.afd.parteiapp.data.LandtagRepository
import de.afd.parteiapp.data.LandtagState
import de.afd.parteiapp.data.Landesverband
import de.afd.parteiapp.data.Links
import de.afd.parteiapp.data.Prefs
import de.afd.parteiapp.ui.components.BrandTopBar
import de.afd.parteiapp.ui.components.rememberBottomShadow
import de.afd.parteiapp.ui.components.dialNumber
import de.afd.parteiapp.ui.components.openUrl
import de.afd.parteiapp.ui.components.relativeTime
import de.afd.parteiapp.ui.components.sendEmail
import de.afd.parteiapp.ui.theme.AfDBlue
import de.afd.parteiapp.ui.theme.AfDNavy
import de.afd.parteiapp.ui.theme.AfDRed
import kotlinx.coroutines.launch

private val landPalette = listOf(
    Color(0xFF0B6FA8),
    Color(0xFF12708A),
    Color(0xFF1D5C6E),
    Color(0xFF2A5D9F),
    Color(0xFF0E7C5A),
    Color(0xFF6A4C93),
    Color(0xFF9C4221),
    Color(0xFF37505C),
)

private fun landColor(land: String): Color =
    landPalette[(land.hashCode() and 0x7FFFFFFF) % landPalette.size]

private fun stateLogoRes(land: String): Int? = when {
    land.contains("Baden") -> R.drawable.state_logo_bw
    land.contains("Bayern") -> R.drawable.state_logo_by
    land.contains("Berlin") -> R.drawable.state_logo_be
    land.contains("Brandenburg") -> R.drawable.state_logo_bb
    land.contains("Bremen") -> R.drawable.state_logo_hb
    land.contains("Hamburg") -> R.drawable.state_logo_hh
    land.contains("Hessen") -> R.drawable.state_logo_he
    land.contains("Mecklenburg") -> R.drawable.state_logo_mv
    land.contains("Niedersachsen") -> R.drawable.state_logo_ni
    land.contains("Nordrhein") -> R.drawable.state_logo_nw
    land.contains("Rheinland") -> R.drawable.state_logo_rp
    land.contains("Saarland") -> R.drawable.state_logo_sl
    land.contains("Sachsen-Anhalt") -> R.drawable.state_logo_st
    land.contains("Sachsen") -> R.drawable.state_logo_sn
    land.contains("Schleswig") -> R.drawable.state_logo_sh
    land.contains("Thüringen") -> R.drawable.state_logo_th
    else -> null
}

private fun initials(member: ContactMember): String {
    val first = member.firstName.firstOrNull()?.uppercaseChar() ?: ' '
    val last = member.lastName.firstOrNull()?.uppercaseChar() ?: ' '
    return "$first$last"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = remember { ContactsRepository.loadBest(context) }
    var data by remember { mutableStateOf(initial.first) }
    var fetchedAt by remember { mutableStateOf(initial.second) }
    var live by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var landFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val prefs = remember { Prefs(context) }
    val initialLandtag = remember { LandtagRepository.loadBest(context) }
    var landtag by remember { mutableStateOf(initialLandtag.first) }
    var landtagLive by remember { mutableStateOf(false) }
    var landtagFetchedAt by remember { mutableStateOf(initialLandtag.second) }
    var selectedLand by rememberSaveable { mutableStateOf(prefs.landtagState) }

    fun refresh(force: Boolean) {
        scope.launch {
            refreshing = true
            val result = ContactsRepository.refresh(context, force)
            data = result.data
            fetchedAt = result.fetchedAt
            live = result.live
            error = result.error
            refreshing = false
        }
        scope.launch {
            val result = LandtagRepository.refresh(context, force)
            landtag = result.data
            landtagLive = result.live
            landtagFetchedAt = result.fetchedAt
        }
    }

    LaunchedEffect(Unit) { refresh(false) }

    val lands = remember(data) {
        data.members.map { it.land }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filtered = remember(data, query, landFilter) {
        data.members
            .filter { member ->
                (landFilter == null || member.land == landFilter) &&
                    (query.isBlank() ||
                        member.name.contains(query, ignoreCase = true) ||
                        member.land.contains(query, ignoreCase = true) ||
                        member.mandate.contains(query, ignoreCase = true) ||
                        member.role.contains(query, ignoreCase = true))
            }
            .sortedBy { it.lastName }
    }

    val statusText = if (live) {
        stringResource(R.string.contacts_source_live, relativeTime(fetchedAt))
    } else {
        stringResource(
            R.string.contacts_source_offline,
            if (fetchedAt > 0L) relativeTime(fetchedAt) else data.updated,
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrandTopBar(
                title = stringResource(R.string.contacts_title),
                actions = {
                    GlassIconButton(onClick = { refresh(true) }, size = 32.dp) {
                        Icon(Icons.Outlined.Refresh, stringResource(R.string.contacts_refresh), Modifier.size(20.dp))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .then(rememberBottomShadow())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text(stringResource(R.string.contacts_tab_bundestag)) },
                )
                GlassChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text(stringResource(R.string.contacts_tab_land)) },
                )
                GlassChip(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    label = { Text(stringResource(R.string.contacts_tab_mein_land)) },
                )
            }
            when (tab) {
                0 -> BundestagTab(
                    data = data,
                    lands = lands,
                    query = query,
                    onQueryChange = { query = it },
                    landFilter = landFilter,
                    onLandChange = { landFilter = it },
                    members = filtered,
                    statusText = statusText,
                    staleError = error != null,
                    modifier = Modifier.weight(1f),
                )
                1 -> LandesverbaendeTab(
                    landesverbaende = data.landesverbaende,
                    modifier = Modifier.weight(1f),
                )
                else -> LandtagTab(
                    landtag = landtag,
                    selectedLand = selectedLand,
                    onSelectLand = { state ->
                        selectedLand = state
                        prefs.landtagState = state
                    },
                    live = landtagLive,
                    fetchedAt = landtagFetchedAt,
                    refreshing = refreshing,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BundestagTab(
    data: ContactsData,
    lands: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    landFilter: String?,
    onLandChange: (String?) -> Unit,
    members: List<ContactMember>,
    statusText: String,
    staleError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FraktionCard(data.fraktion)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            placeholder = { Text(stringResource(R.string.contacts_search)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                GlassChip(
                    selected = landFilter == null,
                    onClick = { onLandChange(null) },
                    label = { Text(stringResource(R.string.contacts_all_lands)) },
                )
            }
            items(lands) { land ->
                GlassChip(
                    selected = landFilter == land,
                    onClick = { onLandChange(if (landFilter == land) null else land) },
                    label = { Text(land) },
                )
            }
        }
        Text(
            text = stringResource(R.string.contacts_members, members.size) + " · " + statusText,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (staleError) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(R.string.common_stale),
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 44.dp + LocalGlassNavPadding.current),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            itemsIndexed(members, key = { index, m -> "${m.email}|${m.name}|$index" }) { _, member ->
                ContactCard(member)
            }
        }
    }
}

@Composable
private fun FraktionCard(fraktion: FraktionInfo) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val logo = if (isSystemInDarkTheme()) R.drawable.afd_logo else R.drawable.afd_logo_light
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(logo),
                    contentDescription = null,
                    modifier = Modifier.height(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.contacts_fraktion_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (fraktion.address.isNotBlank()) {
                        Text(
                            fraktion.address,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fraktion.email.ifBlank { fraktion.website },
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (fraktion.email.isNotBlank()) {
                    IconButton(onClick = { sendEmail(context, fraktion.email) }) {
                        Icon(Icons.Outlined.Email, stringResource(R.string.contacts_email), Modifier.size(22.dp))
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(fraktion.email))
                        Toast.makeText(context, context.getString(R.string.contacts_copied), Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.ContentCopy, stringResource(R.string.contacts_copy), Modifier.size(22.dp))
                    }
                }
                if (fraktion.phone.isNotBlank()) {
                    IconButton(onClick = { dialNumber(context, fraktion.phone) }) {
                        Icon(Icons.Outlined.Phone, stringResource(R.string.contacts_email), Modifier.size(22.dp))
                    }
                }
                if (fraktion.website.isNotBlank()) {
                    IconButton(onClick = { openUrl(context, fraktion.website) }) {
                        Icon(Icons.Outlined.OpenInNew, stringResource(R.string.contacts_website), Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.contacts_email_note_short),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContactCard(member: ContactMember) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(landColor(member.land))
                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initials(member),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (member.photo.isNotBlank()) {
                        AsyncImage(
                            model = member.photo,
                            contentDescription = member.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape),
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    val subtitle = when {
                        member.mandate.isBlank() -> member.land
                        member.mandate.contains(member.land, ignoreCase = true) -> member.mandate
                        else -> listOf(member.land, member.mandate)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (member.role.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = AfDRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        member.role,
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSystemInDarkTheme()) Color(0xFFFF8A90) else AfDRed,
                    )
                }
            }
            if (member.email.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        member.email,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { sendEmail(context, member.email) }) {
                        Icon(Icons.Outlined.Email, stringResource(R.string.contacts_email), Modifier.size(24.dp))
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(member.email))
                        Toast.makeText(context, context.getString(R.string.contacts_copied), Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.ContentCopy, stringResource(R.string.contacts_copy), Modifier.size(24.dp))
                    }
                    IconButton(onClick = { openUrl(context, Links.BUNDESTAG_MEMBERS) }) {
                        Icon(Icons.Outlined.OpenInNew, stringResource(R.string.contacts_bundestag), Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LandtagTab(
    landtag: de.afd.parteiapp.data.LandtagData,
    selectedLand: String?,
    onSelectLand: (String?) -> Unit,
    live: Boolean,
    fetchedAt: Long,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = landtag.states.firstOrNull { it.name == selectedLand }
    if (state == null) {
        LandtagPicker(states = landtag.states, onSelect = { onSelectLand(it) }, modifier = modifier)
    } else {
        LandtagMembers(
            state = state,
            onChangeState = { onSelectLand(null) },
            live = live,
            fetchedAt = fetchedAt,
            refreshing = refreshing,
            modifier = modifier,
        )
    }
}

private fun landtagColor(name: String): Color =
    landPalette[(name.hashCode() and 0x7FFFFFFF) % landPalette.size]

@Composable
private fun LandtagPicker(
    states: List<LandtagState>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = 2
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 20.dp + LocalGlassNavPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    stringResource(R.string.landtag_pick_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.landtag_pick_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(states.chunked(columns)) { rowStates ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                rowStates.forEach { st ->
                    val logo = stateLogoRes(st.name)
                    val count = st.members.size
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = count > 0) { onSelect(st.name) },
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (logo != null) {
                                    Image(
                                        painter = painterResource(logo),
                                        contentDescription = st.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxSize(),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                st.name.replace("-", "-\u200B"),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.landtag_members, count),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (rowStates.size < columns) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LandtagMembers(
    state: LandtagState,
    onChangeState: () -> Unit,
    live: Boolean,
    fetchedAt: Long,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 44.dp + LocalGlassNavPadding.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val logo = stateLogoRes(state.name)
                        if (logo != null) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(logo),
                                    contentDescription = state.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .fillMaxSize(),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                state.parliament,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassButton(
                        onClick = onChangeState,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                    ) {
                        Text(
                            stringResource(R.string.landtag_change_state),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val status = when {
                        refreshing -> stringResource(R.string.landtag_status_refreshing)
                        live -> stringResource(R.string.landtag_source_live, relativeTime(fetchedAt))
                        fetchedAt > 0L -> stringResource(R.string.contacts_source_offline, relativeTime(fetchedAt))
                        else -> null
                    }
                    Text(
                        text = stringResource(R.string.landtag_members, state.members.size) +
                            (status?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.members.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Text(
                        stringResource(R.string.landtag_empty),
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            itemsIndexed(state.members, key = { index, m -> "${m.land}:${m.name}:$index" }) { _, member ->
                LandtagMemberCard(member)
            }
            item {
                Text(
                    stringResource(R.string.landtag_source, state.source.substringAfter("://")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LandtagMemberCard(member: LandtagMember) {
    val context = LocalContext.current
    val initials = buildString {
        member.firstName.firstOrNull()?.uppercaseChar()?.let { append(it) }
        member.lastName.firstOrNull()?.uppercaseChar()?.let { append(it) }
    }.trim()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(landtagColor(member.land))
                        .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (member.photo.isNotBlank()) {
                        AsyncImage(
                            model = member.photo,
                            contentDescription = member.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        member.land,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (member.role.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = AfDRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                member.role,
                                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSystemInDarkTheme()) Color(0xFFFF8A90) else AfDRed,
                            )
                        }
                    }
                }
            }
            if (member.email.isNotBlank() || member.website.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (member.website.isNotBlank()) {
                        TextButton(onClick = { openUrl(context, member.website) }) {
                            Icon(Icons.Outlined.Language, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.landtag_website))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (member.email.isNotBlank()) {
                        IconButton(onClick = { sendEmail(context, member.email) }) {
                            Icon(Icons.Outlined.Email, stringResource(R.string.contacts_email), Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun socialIconRes(label: String): Int? = when (label) {
    "Facebook" -> R.drawable.ic_social_facebook
    "Instagram" -> R.drawable.ic_social_instagram
    "X" -> R.drawable.ic_social_x
    "YouTube" -> R.drawable.ic_social_youtube
    "Telegram" -> R.drawable.ic_social_telegram
    "TikTok" -> R.drawable.ic_social_tiktok
    else -> null
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    clickable: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (clickable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LandesverbaendeTab(
    landesverbaende: List<Landesverband>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val logo = if (isSystemInDarkTheme()) R.drawable.afd_logo else R.drawable.afd_logo_light
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 20.dp + LocalGlassNavPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.contacts_land_overview_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { openUrl(context, Links.STATE_ASSOCIATIONS) }) {
                        Text(stringResource(R.string.contacts_land_overview))
                    }
                }
            }
        }
        items(landesverbaende, key = { it.name }) { verband ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Column {
                    val stateLogo = stateLogoRes(verband.name)
                    if (stateLogo != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(stateLogo),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                    .fillMaxSize(),
                            )
                        }
                    }
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(logo),
                            contentDescription = null,
                            modifier = Modifier.height(24.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                verband.name.replace("-", "-\u200B"),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.contacts_land_association),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                        if (verband.website.isNotBlank()) {
                            DetailRow(
                                icon = Icons.Outlined.Language,
                                text = verband.website.removePrefix("https://").removePrefix("www."),
                            ) { openUrl(context, verband.website) }
                        }
                        if (verband.email.isNotBlank()) {
                            DetailRow(Icons.Outlined.Email, verband.email) {
                                sendEmail(context, verband.email)
                            }
                        }
                        if (verband.phone.isNotBlank()) {
                            DetailRow(Icons.Outlined.Phone, verband.phone) {
                                dialNumber(context, verband.phone)
                            }
                        }
                        if (verband.address.isNotBlank()) {
                            DetailRow(Icons.Outlined.LocationOn, verband.address, clickable = false)
                        }

                        if (verband.fraktionName.isNotBlank() || verband.fraktionEmail.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.contacts_land_group),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            if (verband.fraktionName.isNotBlank()) {
                                Text(
                                    verband.fraktionName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (verband.fraktionWebsite.isNotBlank()) {
                                DetailRow(Icons.Outlined.Language, verband.fraktionWebsite.removePrefix("https://").removePrefix("www.")) {
                                    openUrl(context, verband.fraktionWebsite)
                                }
                            }
                            if (verband.fraktionEmail.isNotBlank()) {
                                DetailRow(Icons.Outlined.Email, verband.fraktionEmail) {
                                    sendEmail(context, verband.fraktionEmail)
                                }
                            }
                            if (verband.fraktionPhone.isNotBlank()) {
                                DetailRow(Icons.Outlined.Phone, verband.fraktionPhone) {
                                    dialNumber(context, verband.fraktionPhone)
                                }
                            }
                            if (verband.fraktionAddress.isNotBlank()) {
                                DetailRow(Icons.Outlined.LocationOn, verband.fraktionAddress, clickable = false)
                            }
                        }

                        if (verband.socials.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                verband.socials.forEach { (label, url) ->
                                    val icon = socialIconRes(label)
                                    if (icon != null) {
                                        Box(
                                            Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { openUrl(context, url) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(icon),
                                                contentDescription = label,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
