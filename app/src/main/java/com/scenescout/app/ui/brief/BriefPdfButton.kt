package com.scenescout.app.ui.brief

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import com.scenescout.app.data.ScenicScorer
import com.scenescout.app.data.Spot
import com.scenescout.app.data.SpotBriefBuilder
import com.scenescout.app.data.SpotReview
import com.scenescout.app.data.SunTimes
import com.scenescout.app.data.imagery.BestImagery
import com.scenescout.app.data.imagery.SpotImage
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Client one-pager" button: builds a PDF location brief for the spot and
 * shares it through any app. Shows a loading state while rendering.
 */
@Composable
fun BriefPdfButton(
    spot: Spot,
    reviews: List<SpotReview>,
    images: List<SpotImage>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var building by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (building) return@Button
            building = true
            scope.launch {
                try {
                    val score = ScenicScorer.scoreSpot(spot)
                    val sun = SunTimes.forDate(
                        spot.latitude, spot.longitude,
                        LocalDate.now(), ZoneId.of("America/New_York"),
                    )
                    val brief = SpotBriefBuilder.build(spot, score, sun, reviews)
                    val heroUrl = BestImagery.forDisplay(images)?.url
                    val hero = withContext(Dispatchers.IO) {
                        loadBitmap(context, heroUrl)
                    }
                    val file = withContext(Dispatchers.IO) {
                        SpotBriefPdf.render(context, brief, hero)
                    }
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file,
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_SUBJECT, "Location brief: ${spot.name}")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Send client one-pager"),
                    )
                } finally {
                    building = false
                }
            }
        },
        enabled = !building,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
        Text(if (building) "Building one-pager…" else "Client one-pager (PDF)")
    }
}

private suspend fun loadBitmap(
    context: android.content.Context,
    url: String?,
): android.graphics.Bitmap? {
    if (url.isNullOrBlank()) return null
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        (result.drawable as? BitmapDrawable)?.bitmap
    } catch (e: Exception) {
        null
    }
}
