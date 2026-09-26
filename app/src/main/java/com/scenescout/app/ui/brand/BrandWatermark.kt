package com.scenescout.app.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.scenescout.app.R

/**
 * The Shotbyx logo as a faint full-screen watermark — the recurring brand
 * mark across the app. Decorative only; kept subtle so content stays readable.
 */
@Composable
fun BrandWatermark(modifier: Modifier = Modifier, alpha: Float = 0.06f) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.shotbyx_logo),
            contentDescription = null,
            alpha = alpha,
            modifier = Modifier.fillMaxWidth(0.85f),
        )
    }
}
