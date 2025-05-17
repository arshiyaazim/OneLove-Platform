package com.kilagee.onelove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Component for displaying premium subscription badges
 */
@Composable
fun PremiumBadge(
    tier: String,
    modifier: Modifier = Modifier
) {
    val (gradient, textColor) = when (tier.lowercase()) {
        "basic" -> Pair(
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF81C784),
                    Color(0xFF66BB6A)
                )
            ),
            Color.White
        )
        "plus" -> Pair(
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF42A5F5),
                    Color(0xFF2196F3)
                )
            ),
            Color.White
        )
        "gold" -> Pair(
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFFD54F),
                    Color(0xFFFFB300)
                )
            ),
            Color.Black
        )
        "platinum" -> Pair(
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFE0E0E0),
                    Color(0xFFBDBDBD)
                )
            ),
            Color.Black
        )
        "premium" -> Pair(
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFE57373),
                    Color(0xFFEF5350)
                )
            ),
            Color.White
        )
        else -> Pair(
            Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary
                )
            ),
            MaterialTheme.colorScheme.onPrimary
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = tier.uppercase(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}