package de.afd.parteiapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.afd.parteiapp.data.Prefs
import de.afd.parteiapp.ui.AppShell
import de.afd.parteiapp.ui.theme.AfDTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLanguage()
        enableEdgeToEdge()
        setContent {
            AfDTheme {
                AppShell()
            }
        }
    }

    private fun applyLanguage() {
        val prefs = Prefs(this)
        val stored = prefs.language ?: "de".also { prefs.language = it }
        val applied = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
        if (applied != stored) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(stored))
        }
    }
}
