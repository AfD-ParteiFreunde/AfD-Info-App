package de.afd.parteiapp.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import de.afd.parteiapp.R

data class BankApp(
    val id: String,
    val label: String,
    val packageNames: List<String>,
    val website: String,
    @DrawableRes val iconRes: Int? = null,
    val color: Long = 0xFF009EE0,
)

object BankApps {

    val all: List<BankApp> = listOf(
        BankApp(
            id = "sparkasse",
            label = "Sparkasse",
            packageNames = listOf(
                "com.starfinanz.smob.android.sfinanzstatus",
            ),
            website = "https://www.sparkasse.de/",
            color = 0xFFE60000,
        ),
        BankApp(
            id = "vr",
            label = "Volksbank",
            packageNames = listOf(
                "de.fiduciagad.banking.vr",
                "de.fiduciagad.android.vrbanking",
            ),
            website = "https://www.vr.de/",
            iconRes = R.drawable.bank_icon_vr,
            color = 0xFF004E9E,
        ),
        BankApp(
            id = "commerzbank",
            label = "Commerzbank",
            packageNames = listOf(
                "de.commerzbanking.mobil",
            ),
            website = "https://www.commerzbank.de/",
            color = 0xFFFFCC00,
        ),
        BankApp(
            id = "deutschebank",
            label = "Deutsche Bank",
            packageNames = listOf(
                "com.db.pwcc.dbmobile",
            ),
            website = "https://www.deutsche-bank.de/",
            color = 0xFF0018A8,
        ),
        BankApp(
            id = "postbank",
            label = "Postbank",
            packageNames = listOf(
                "de.postbank.banking",
                "de.postbank.finanzassistent",
            ),
            website = "https://www.postbank.de/",
            color = 0xFF004994,
        ),
        BankApp(
            id = "norisbank",
            label = "norisbank",
            packageNames = listOf(
                "com.db.mm.norisbank",
            ),
            website = "https://www.norisbank.de/",
            color = 0xFFE2001A,
        ),
        BankApp(
            id = "dkb",
            label = "DKB",
            packageNames = listOf(
                "com.dkbcodefactory.banking",
                "de.dkb.portalapp",
            ),
            website = "https://www.dkb.de/",
            color = 0xFF0B2E4F,
        ),
        BankApp(
            id = "ing",
            label = "ING",
            packageNames = listOf(
                "de.ingdiba.bankingapp",
            ),
            website = "https://www.ing.de/",
            color = 0xFFFF6600,
        ),
        BankApp(
            id = "n26",
            label = "N26",
            packageNames = listOf(
                "de.number26.android",
            ),
            website = "https://n26.com/",
            iconRes = R.drawable.bank_icon_n26,
            color = 0xFF2C6E63,
        ),
        BankApp(
            id = "comdirect",
            label = "comdirect",
            packageNames = listOf(
                "de.comdirect.app",
            ),
            website = "https://www.comdirect.de/",
            color = 0xFFFFCC00,
        ),
        BankApp(
            id = "targobank",
            label = "Targobank",
            packageNames = listOf(
                "com.targo_prod.bad",
            ),
            website = "https://www.targobank.de/",
            color = 0xFF004C94,
        ),
        BankApp(
            id = "hvb",
            label = "HypoVereinsbank",
            packageNames = listOf(
                "eu.unicreditgroup.hvbapptan",
            ),
            website = "https://www.hypovereinsbank.de/",
            color = 0xFFB01B2E,
        ),
        BankApp(
            id = "santander",
            label = "Santander",
            packageNames = listOf(
                "de.santander.presentation",
                "de.santander.banking",
            ),
            website = "https://www.santander.de/",
            color = 0xFFEC0000,
        ),
        BankApp(
            id = "gls",
            label = "GLS Bank",
            packageNames = listOf(
                "de.gls.banking",
                "de.gls.mbank",
            ),
            website = "https://www.gls.de/",
            color = 0xFF1E9E4A,
        ),
        BankApp(
            id = "sparda",
            label = "Sparda-Bank",
            packageNames = listOf(
                "de.sdvrz.ihb.mobile.app",
                "de.sparda.banking.app",
            ),
            website = "https://www.sparda.de/",
            color = 0xFFE30613,
        ),
        BankApp(
            id = "apobank",
            label = "apoBank",
            packageNames = listOf(
                "de.apobank.nsa.apoBankingPlus",
            ),
            website = "https://www.apobank.de/",
            color = 0xFF0067A3,
        ),
        BankApp(
            id = "consorsbank",
            label = "Consorsbank",
            packageNames = listOf(
                "de.consorsbank",
            ),
            website = "https://www.consorsbank.de/",
            color = 0xFFFF8C00,
        ),
        BankApp(
            id = "traderepublic",
            label = "Trade Republic",
            packageNames = listOf(
                "de.traderepublic.app",
            ),
            website = "https://traderepublic.com/",
            iconRes = R.drawable.bank_icon_traderepublic,
            color = 0xFF111111,
        ),
    )

    fun installedPackages(context: Context, app: BankApp): List<String> {
        val pm = context.packageManager
        return app.packageNames.filter { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
    }

    fun detectInstalled(context: Context): List<Pair<BankApp, String>> =
        all.mapNotNull { app ->
            installedPackages(context, app).firstOrNull()?.let { app to it }
        }

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun playStoreUrl(packageName: String): String =
        "https://play.google.com/store/apps/details?id=$packageName"

    fun sepaUri(name: String, iban: String, bic: String, amount: String = "", reason: String = "Spende"): android.net.Uri {
        val builder = StringBuilder("bank://singlepaymentsepa?")
        builder.append("name=").append(android.net.Uri.encode(name))
        builder.append("&iban=").append(iban.replace(" ", ""))
        if (bic.isNotBlank()) builder.append("&bic=").append(bic.replace(" ", ""))
        if (amount.isNotBlank()) builder.append("&amount=").append(android.net.Uri.encode(amount))
        if (reason.isNotBlank()) builder.append("&reason=").append(android.net.Uri.encode(reason))
        return builder.toString().toUri()
    }

    fun openSepaIntent(context: Context, uri: android.net.Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
