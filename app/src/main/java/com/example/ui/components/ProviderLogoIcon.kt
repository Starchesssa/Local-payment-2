package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandAirtel
import com.example.ui.theme.BrandHalotel
import com.example.ui.theme.BrandMpesa
import com.example.ui.theme.BrandYas

@Composable
fun ProviderLogoIcon(
    providerId: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (bgColor, letter) = when (providerId) {
        "mpesa" -> BrandMpesa to "M"
        "airtel_money" -> BrandAirtel to "A"
        "yas_tigo" -> BrandYas to "Y"
        "halopesa" -> BrandHalotel to "H"
        else -> Color(0xFF00875A) to "P"
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp
        )
    }
}
