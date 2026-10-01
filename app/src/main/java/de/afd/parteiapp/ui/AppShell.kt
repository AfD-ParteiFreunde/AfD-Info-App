package de.afd.parteiapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import de.afd.parteiapp.R
import de.afd.parteiapp.data.Article
import de.afd.parteiapp.data.ContactsRepository
import de.afd.parteiapp.data.FraktionEvent
import de.afd.parteiapp.data.Prefs
import de.afd.parteiapp.ui.contacts.ContactsScreen
import de.afd.parteiapp.ui.polls.StatePollsScreen
import de.afd.parteiapp.ui.program.WahlprogrammScreen
import de.afd.parteiapp.ui.glass.GlassFab
import de.afd.parteiapp.ui.glass.GlassNavBar
import de.afd.parteiapp.ui.glass.GlassScrollTopState
import de.afd.parteiapp.ui.glass.LocalGlassEnabled
import de.afd.parteiapp.ui.glass.LocalGlassNavPadding
import de.afd.parteiapp.ui.glass.LocalGlassScrollTop
import de.afd.parteiapp.ui.map.MapScreen
import de.afd.parteiapp.ui.more.MoreScreen
import de.afd.parteiapp.ui.news.ArticleReaderScreen
import de.afd.parteiapp.ui.news.NewsScreen
import de.afd.parteiapp.ui.news.NewsViewModel
import de.afd.parteiapp.ui.splash.PrivacyConsentScreen
import de.afd.parteiapp.ui.splash.PrivacyExit
import de.afd.parteiapp.ui.splash.SplashScreen
import de.afd.parteiapp.ui.support.SupportScreen
import de.afd.parteiapp.ui.wahlkampf.WahlkampfScreen

@Composable
fun AppShell() {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val newsViewModel: NewsViewModel = viewModel()
    val contactsCount = remember { ContactsRepository.load(context).members.size }
    val prefs = remember { Prefs(context) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var readerArticle by remember { mutableStateOf<Article?>(null) }
    var mapEvent by remember { mutableStateOf<FraktionEvent?>(null) }
    var showStatePolls by remember { mutableStateOf(false) }
    var showProgram by remember { mutableStateOf(false) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var privacyAccepted by rememberSaveable { mutableStateOf(prefs.privacyAccepted) }
    var showPrivacyReview by rememberSaveable { mutableStateOf(false) }
    var glassNav by rememberSaveable { mutableStateOf(prefs.glassNav) }
    val bottomInset = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val backdrop = rememberLayerBackdrop()
    val scrollTopState = remember { GlassScrollTopState() }
    val tabStateHolder = rememberSaveableStateHolder()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = if (glassNav) Modifier.layerBackdrop(backdrop) else Modifier,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (glassNav) {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(49.dp + bottomInset),
                    )
                } else {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Outlined.Newspaper, null) },
                            label = { Text(stringResource(R.string.nav_news)) },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Outlined.HowToVote, null) },
                            label = { Text(stringResource(R.string.nav_wahlkampf)) },
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Outlined.VolunteerActivism, null) },
                            label = { Text(stringResource(R.string.nav_donate)) },
                        )
                        NavigationBarItem(
                            selected = tab == 3,
                            onClick = { tab = 3 },
                            icon = { Icon(Icons.Outlined.Contacts, null) },
                            label = { Text(stringResource(R.string.nav_contacts)) },
                        )
                        NavigationBarItem(
                            selected = tab == 4,
                            onClick = { tab = 4 },
                            icon = { Icon(Icons.Outlined.MoreHoriz, null) },
                            label = { Text(stringResource(R.string.nav_more)) },
                        )
                    }
                }
            },
        ) { padding ->
            CompositionLocalProvider(
                LocalGlassNavPadding provides if (glassNav) 30.dp else 0.dp,
                LocalGlassScrollTop provides if (glassNav) scrollTopState else null,
                LocalGlassEnabled provides glassNav,
            ) {
                Box(Modifier.padding(padding)) {
                    // Preserve per-tab UI state (scroll position, filters) across switches.
                    tabStateHolder.SaveableStateProvider(tab) {
                        when (tab) {
                            0 -> NewsScreen(newsViewModel, onOpenArticle = { readerArticle = it })
                            1 -> WahlkampfScreen(
                                onOpenSupport = { tab = 2 },
                                onOpenMap = { mapEvent = it },
                                onOpenStatePolls = { showStatePolls = true },
                            )
                            2 -> SupportScreen()
                            3 -> ContactsScreen()
                            else -> MoreScreen(
                                newsViewModel,
                                contactsCount,
                                glassNav,
                                onGlassNavChange = { enabled ->
                                    glassNav = enabled
                                    prefs.glassNav = enabled
                                },
                                onOpenPrivacy = { showPrivacyReview = true },
                                onOpenProgram = { showProgram = true },
                            )
                        }
                    }
                }
            }
        }

        if (glassNav) {
            GlassNavBar(
                selected = tab,
                onSelect = { tab = it },
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 10.dp, end = 10.dp, bottom = bottomInset + 8.dp)
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }

        AnimatedVisibility(
            visible = glassNav && tab == 0 && scrollTopState.visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 88.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            GlassFab(
                backdrop = backdrop,
                onClick = { scrollTopState.action?.invoke() },
            )
        }

        readerArticle?.let { article ->
            BackHandler { readerArticle = null }
            ArticleReaderScreen(article = article, onBack = { readerArticle = null })
        }

        mapEvent?.let { event ->
            BackHandler { mapEvent = null }
            MapScreen(initial = event, onBack = { mapEvent = null })
        }

        if (showStatePolls) {
            BackHandler { showStatePolls = false }
            StatePollsScreen(onBack = { showStatePolls = false })
        }

        if (showProgram) {
            BackHandler { showProgram = false }
            WahlprogrammScreen(onBack = { showProgram = false })
        }

        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        }

        if (!showSplash && !privacyAccepted) {
            PrivacyConsentScreen(
                onAccept = {
                    prefs.privacyAccepted = true
                    privacyAccepted = true
                },
                onExit = { exit ->
                    when (exit) {
                        PrivacyExit.CLOSE -> activity?.finish()
                        PrivacyExit.REVIEW_CLOSE -> showPrivacyReview = false
                    }
                },
            )
        }

        if (showPrivacyReview && privacyAccepted && !showSplash) {
            BackHandler { showPrivacyReview = false }
            PrivacyConsentScreen(
                reviewMode = true,
                onAccept = {},
                onExit = { exit ->
                    if (exit == PrivacyExit.REVIEW_CLOSE) showPrivacyReview = false
                },
            )
        }
    }
}
