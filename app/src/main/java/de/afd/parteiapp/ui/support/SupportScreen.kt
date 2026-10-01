package de.afd.parteiapp.ui.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import de.afd.parteiapp.ui.glass.GlassChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.afd.parteiapp.R
import de.afd.parteiapp.ui.components.BrandTopBar
import de.afd.parteiapp.ui.components.rememberBottomShadow
import de.afd.parteiapp.ui.donate.DonateContent
import de.afd.parteiapp.ui.shop.ShopContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen() {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            BrandTopBar(title = stringResource(R.string.support_title))
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
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text(stringResource(R.string.support_tab_donate)) },
                )
                GlassChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text(stringResource(R.string.support_tab_shop)) },
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (tab == 0) DonateContent() else ShopContent()
            }
        }
    }
}
