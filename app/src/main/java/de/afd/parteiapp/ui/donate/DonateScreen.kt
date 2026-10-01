package de.afd.parteiapp.ui.donate

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import de.afd.parteiapp.ui.glass.GlassChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.glass.GlassButton
import de.afd.parteiapp.ui.glass.LocalGlassNavPadding
import de.afd.parteiapp.data.DonationsRepository
import de.afd.parteiapp.data.BankApp
import de.afd.parteiapp.data.BankApps
import de.afd.parteiapp.data.Links
import de.afd.parteiapp.data.epcPayload
import de.afd.parteiapp.ui.components.BrandLinkCard
import de.afd.parteiapp.ui.components.CopyRow
import de.afd.parteiapp.ui.components.QrCode
import de.afd.parteiapp.ui.components.SectionCard
import de.afd.parteiapp.ui.components.openUrl
import de.afd.parteiapp.ui.theme.AfDBlue
import de.afd.parteiapp.ui.theme.AfDNavy

@Composable
fun DonateContent() {
    val context = LocalContext.current
    val accounts = remember { DonationsRepository.load(context) }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selected = accounts.getOrNull(selectedIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 16.dp + LocalGlassNavPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                                stringResource(R.string.donate_hero_title),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.donate_hero_text),
                                color = Color(0xFFD7EAF7),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassButton(
                                    onClick = { openUrl(context, Links.JOIN) },
                                    tint = Color(0xCCFFFFFF),
                                    contentColor = AfDNavy,
                                ) {
                                    Text(
                                        stringResource(R.string.donate_join),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                GlassButton(
                                    onClick = { openUrl(context, Links.DONATE) },
                                    tint = Color(0x2EFFFFFF),
                                    contentColor = Color.White,
                                ) {
                                    Text(
                                        stringResource(R.string.donate_official),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                BrandLinkCard(
                    iconRes = R.drawable.ic_paypal,
                    tint = Color(0xFF0070BA),
                    title = stringResource(R.string.donate_paypal),
                    subtitle = stringResource(R.string.donate_paypal_sub),
                ) { openUrl(context, Links.PAYPAL) }
            }

            item {
                SectionCard(stringResource(R.string.donate_bank)) {
                    Text(
                        stringResource(R.string.donate_choose),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(accounts) { index, account ->
                            GlassChip(
                                selected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                                label = { Text(account.name) },
                            )
                        }
                    }
                    if (selected != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.donate_account_for, selected.name),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        if (selected.recipient.isNotBlank()) {
                            CopyRow(stringResource(R.string.donate_recipient), selected.recipient)
                        }
                        if (selected.iban.isNotBlank()) {
                            CopyRow(stringResource(R.string.donate_iban), selected.iban)
                        }
                        if (selected.bic.isNotBlank()) {
                            CopyRow(stringResource(R.string.donate_bic), selected.bic)
                        }
                        if (selected.bank.isNotBlank()) {
                            CopyRow(stringResource(R.string.donate_bank_name), selected.bank)
                        }
                        CopyRow(
                            stringResource(R.string.donate_purpose),
                            stringResource(R.string.donate_purpose_value),
                        )
                        val epc = remember(selected) { selected.epcPayload() }
                        if (epc != null) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                stringResource(R.string.donate_qr_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            QrCode(epc, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.donate_qr_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected.website.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            GlassButton(
                                onClick = { openUrl(context, selected.website) },
                                tint = AfDBlue.copy(alpha = 0.55f),
                                contentColor = Color.White,
                            ) {
                                Text(
                                    stringResource(R.string.donate_official),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            item {
                BankAppSection()
            }

            item {
                SectionCard(stringResource(R.string.donate_tax_title)) {
                    Text(
                        stringResource(R.string.donate_tax_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                SectionCard(stringResource(R.string.donate_legal_title)) {
                    Text(
                        stringResource(R.string.donate_legal_text),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.donate_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
    }
}

@Composable
private fun BankAppSection() {
    val context = LocalContext.current
    val installed by produceState(initialValue = emptyList<Pair<de.afd.parteiapp.data.BankApp, String>>(), context) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            BankApps.detectInstalled(context)
        }
    }

    SectionCard(stringResource(R.string.donate_app_bank_title)) {
        if (installed.isEmpty()) {
            Text(
                stringResource(R.string.donate_app_bank_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                stringResource(R.string.donate_app_bank_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(installed) { (app, pkg) ->
                    BankAppTile(app = app, onClick = { BankApps.launch(context, pkg) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.donate_app_bank_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BankAppTile(app: BankApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(78.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(app.color).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            if (app.iconRes != null) {
                Image(
                    painterResource(app.iconRes),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(15.dp)),
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_bank_generic),
                    contentDescription = app.label,
                    tint = Color(app.color),
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
