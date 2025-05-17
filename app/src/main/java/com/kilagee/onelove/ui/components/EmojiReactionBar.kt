package com.kilagee.onelove.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kilagee.onelove.data.model.ReactionSummary
import com.kilagee.onelove.data.model.ReactionType

/**
 * A bar displaying emoji reactions for users to select
 */
@Composable
fun EmojiReactionBar(
    selectedReaction: ReactionType?,
    onReactionSelected: (ReactionType) -> Unit,
    onReactionRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    reactions: List<ReactionType> = ReactionType.getCommonReactions(),
    isCompact: Boolean = true
) {
    Card(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        // If compact, show all reactions in a row
        if (isCompact) {
            LazyRow(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(reactions) { reaction ->
                    val isSelected = selectedReaction == reaction
                    
                    EmojiReaction(
                        emoji = reaction.emoji,
                        label = if (isSelected) reaction.label else null,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                onReactionRemoved()
                            } else {
                                onReactionSelected(reaction)
                            }
                        },
                        modifier = Modifier.animateContentSize()
                    )
                }
            }
        } else {
            // Not compact, show in a grid
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .animateContentSize()
            ) {
                // Show reactions in rows of 4
                for (i in reactions.indices step 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (j in 0 until 4) {
                            val index = i + j
                            if (index < reactions.size) {
                                val reaction = reactions[index]
                                val isSelected = selectedReaction == reaction
                                
                                EmojiReaction(
                                    emoji = reaction.emoji,
                                    label = reaction.label,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            onReactionRemoved()
                                        } else {
                                            onReactionSelected(reaction)
                                        }
                                    }
                                )
                            } else {
                                // Empty space to maintain grid alignment
                                Spacer(modifier = Modifier.width(40.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single emoji reaction item
 */
@Composable
fun EmojiReaction(
    emoji: String,
    label: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    } else {
        Color.Transparent
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            color = backgroundColor
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        
        // Optional label
        if (label != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A summary bar showing the count of reactions on an item
 */
@Composable
fun ReactionSummaryBar(
    summary: ReactionSummary,
    onReactionClicked: (ReactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary.total > 0) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show the most common reactions (up to 3)
            val topReactions = summary.counts.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }
            
            for (reaction in topReactions) {
                val count = summary.counts[reaction] ?: 0
                val isUserReaction = summary.userReaction == reaction
                
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onReactionClicked(reaction) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    color = if (isUserReaction) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        Color.Transparent
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = reaction.emoji,
                            fontSize = 14.sp
                        )
                        
                        if (count > 1) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUserReaction) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
            
            // If there are more reactions than shown
            if (summary.counts.size > topReactions.size) {
                val remaining = summary.total - topReactions.sumOf { summary.counts[it] ?: 0 }
                if (remaining > 0) {
                    Text(
                        text = "+$remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmojiReactionBarPreview() {
    MaterialTheme {
        var selectedReaction by remember { mutableStateOf<ReactionType?>(null) }
        
        Column {
            EmojiReactionBar(
                selectedReaction = selectedReaction,
                onReactionSelected = { selectedReaction = it },
                onReactionRemoved = { selectedReaction = null },
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // With selection
            EmojiReactionBar(
                selectedReaction = ReactionType.LOVE,
                onReactionSelected = { selectedReaction = it },
                onReactionRemoved = { selectedReaction = null },
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid layout
            EmojiReactionBar(
                selectedReaction = selectedReaction,
                onReactionSelected = { selectedReaction = it },
                onReactionRemoved = { selectedReaction = null },
                isCompact = false,
                reactions = ReactionType.values().toList(),
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reaction summary
            val summary = ReactionSummary(
                targetId = "msg1",
                counts = mapOf(
                    ReactionType.LIKE to 5,
                    ReactionType.LOVE to 3,
                    ReactionType.LAUGH to 2,
                    ReactionType.WOW to 1
                ),
                total = 11,
                userReaction = ReactionType.LIKE
            )
            
            ReactionSummaryBar(
                summary = summary,
                onReactionClicked = { /* do nothing in preview */ },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}