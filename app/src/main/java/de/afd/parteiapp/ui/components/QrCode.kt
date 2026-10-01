package de.afd.parteiapp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, size: Dp = 176.dp) {
    val density = LocalDensity.current
    val bitmap by produceState<Bitmap?>(initialValue = null, content, size) {
        value = withContext(Dispatchers.Default) { generate(content, size, density) }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = content,
                modifier = Modifier.size(size),
            )
        }
    }
}

private fun generate(content: String, size: Dp, density: Density): Bitmap? = runCatching {
    val pixels = with(density) { size.roundToPx() }.coerceAtLeast(64)
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
    )
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, pixels, pixels, hints)
    val data = IntArray(pixels * pixels)
    for (y in 0 until pixels) {
        for (x in 0 until pixels) {
            data[y * pixels + x] = if (matrix.get(x, y)) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
        }
    }
    Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888).apply {
        setPixels(data, 0, pixels, 0, 0, pixels, pixels)
    }
}.getOrNull()
