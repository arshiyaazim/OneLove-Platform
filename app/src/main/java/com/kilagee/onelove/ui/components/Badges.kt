package com.kilagee.onelove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilagee.onelove.ui.theme.OneLoveTheme

/**
 * Verification badge component
 * 
 * @param modifier Modifier for customizing the component
 */
@Composable
fun VerificationBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "Verified",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Premium badge component
 * 
 * @param modifier Modifier for customizing the component
 */
@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Premium",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "Premium",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * VIP badge component
 * 
 * @param modifier Modifier for customizing the component
 */
@Composable
fun VIPBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFA000).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "VIP",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "VIP",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Custom status badge component
 * 
 * @param text Status text
 * @param icon Icon to display
 * @param backgroundColor Background color
 * @param textColor Text color
 * @param modifier Modifier for customizing the component
 */
@Composable
fun StatusBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    backgroundColor: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Online status indicator
 * 
 * @param isOnline Whether the user is online
 * @param modifier Modifier for customizing the component
 */
@Composable
fun OnlineStatusIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
                if (isOnline) Color(0xFF4CAF50) 
                else Color(0xFFBDBDBD)
            )
    )
}

@Preview(showBackground = true)
@Composable
fun BadgesPreview() {
    OneLoveTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.White)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerificationBadge()
                
                Spacer(modifier = Modifier.width(8.dp))
                
                PremiumBadge()
                
                Spacer(modifier = Modifier.width(8.dp))
                
                VIPBadge()
                
                Spacer(modifier = Modifier.width(8.dp))
                
                StatusBadge(
                    text = "New",
                    backgroundColor = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OnlineStatusIndicator(isOnline = true)
            }
        }
    }
}