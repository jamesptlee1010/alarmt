package com.james.mathwakealarm

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * The universal TAZALARM mark supplied by the user: sneezing cat with droplets
 * and the TAZALARM wordmark. The source PNG is transparent and monochrome, so
 * the same exact artwork can be tinted navy on light screens and white on the
 * live alarm screen.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    color: Color,
    fullLogo: Boolean = true
) {
    Image(
        painter = painterResource(
            if (fullLogo) R.drawable.tazalarm_logo_full else R.drawable.tazalarm_cat_only
        ),
        contentDescription = if (fullLogo) "TAZALARM sneezing cat logo" else "Sneezing cat logo",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color)
    )
}

@Composable
fun SneezingCatLogo(
    modifier: Modifier = Modifier,
    color: Color
) {
    BrandMark(modifier = modifier, color = color, fullLogo = false)
}

@Composable
fun BrandHeader(modifier: Modifier = Modifier, compact: Boolean = false) {
    BrandMark(
        modifier = modifier
            .width(if (compact) 142.dp else 205.dp)
            .height(if (compact) 82.dp else 132.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        fullLogo = true
    )
}
