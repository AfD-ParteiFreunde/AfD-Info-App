package de.afd.parteiapp.data

import android.content.Context
import org.json.JSONArray

data class DonationAccount(
    val id: String,
    val name: String,
    val recipient: String,
    val iban: String,
    val bic: String,
    val bank: String,
    val website: String,
)

fun DonationAccount.epcPayload(purpose: String = "Spende"): String? {
    if (recipient.isBlank() || iban.isBlank()) return null
    return listOf(
        "BCD",
        "002",
        "1",
        "SCT",
        bic.replace(" ", ""),
        recipient.take(70),
        iban.replace(" ", ""),
        "EUR",
        "",
        purpose.take(140),
        "",
    ).joinToString("\n")
}

object DonationsRepository {

    fun load(context: Context): List<DonationAccount> = runCatching {
        val text = context.assets.open("donations.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(text)
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            DonationAccount(
                id = obj.optString("id"),
                name = obj.optString("name"),
                recipient = obj.optString("recipient"),
                iban = obj.optString("iban"),
                bic = obj.optString("bic"),
                bank = obj.optString("bank"),
                website = obj.optString("website"),
            )
        }
    }.getOrDefault(emptyList())
}
