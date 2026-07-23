package com.james.mathwakealarm

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    color: Color,
    fullLogo: Boolean = true
) {
    if (!fullLogo) {
        Image(
            painter = painterResource(R.drawable.tazalarm_cat_only),
            contentDescription = "Sneezing cat logo",
            modifier = modifier,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(color)
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.tazalarm_cat_only),
            contentDescription = "TAZLARM sneezing cat logo",
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(color)
        )
        Text(
            "TAZLARM",
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            letterSpacing = 2.sp
        )
    }
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
            .width(if (compact) 180.dp else 220.dp)
            .height(if (compact) 56.dp else 80.dp),
        color = MaterialTheme.colorScheme.onSurface,
        fullLogo = true
    )
}
