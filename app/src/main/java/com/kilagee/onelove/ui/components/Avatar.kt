package com.kilagee.onelove.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.ui.theme.AvatarLargeShape
import com.kilagee.onelove.ui.theme.AvatarSmallShape
import com.kilagee.onelove.ui.theme.OneLoveTheme
import com.kilagee.onelove.ui.theme.PurpleGrey40
import com.kilagee.onelove.ui.theme.VerifiedBadgeColor

/**
 * User avatar component with support for online indicators and verification badges
 *
 * @param imageUrl URL of the avatar image
 * @param size Size of the avatar
 * @param isOnline Whether the user is online
 * @param isVerified Whether the user is verified
 * @param modifier Modifier to be applied to the avatar
 */
@Composable
fun Avatar(
    imageUrl: String?,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    isVerified: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = if (size < 64.dp) AvatarSmallShape else AvatarLargeShape
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Avatar image or placeholder
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(PurpleGrey40.copy(alpha = 0.1f))
                .border(
                    width = if (isOnline) 2.dp else 0.dp,
                    color = if (isOnline) VerifiedBadgeColor else Color.Transparent,
                    shape = shape
                )
        ) {
            if (imageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(size * 0.6f)
                        .align(Alignment.Center),
                    tint = PurpleGrey40
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size)
                )
            }
        }
        
        // Verification badge
        if (isVerified) {
            VerifiedBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(size * 0.3f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AvatarPreview() {
    OneLoveTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.White)
        ) {
            Avatar(
                imageUrl = null,
                isOnline = true,
                isVerified = true,
                size = 80.dp
            )
        }
    }
}