package com.kilagee.onelove.ui.screens.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.ui.LocalSnackbarHostState
import com.kilagee.onelove.ui.components.EmptyStateView
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Home screen for user discovery
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToFilter: () -> Unit,
    onNavigateToMatch: (String) -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val discoverCards by viewModel.discoverCards.collectAsState()
    val topPicks by viewModel.topPicks.collectAsState()
    val likesYou by viewModel.likesYou.collectAsState()
    val recentlyActive by viewModel.recentlyActive.collectAsState()
    val remainingLikes by viewModel.remainingLikes.collectAsState()
    val remainingSuperLikes by viewModel.remainingSuperLikes.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    
    var showMatchDialog by remember { mutableStateOf(false) }
    var matchUserId by remember { mutableStateOf("") }
    var matchUser by remember { mutableStateOf<User?>(null) }
    
    var showReportDialog by remember { mutableStateOf(false) }
    var reportUserId by remember { mutableStateOf("") }
    
    var showBlockDialog by remember { mutableStateOf(false) }
    var blockUserId by remember { mutableStateOf("") }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeEvent.Match -> {
                    // Find the matched user
                    matchUserId = event.matchId
                    // In a real app, you would fetch the match data here
                    // For now, we'll just show the dialog
                    showMatchDialog = true
                }
                is HomeEvent.SuperLiked -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("You super liked this user!")
                    }
                }
                is HomeEvent.BoostActivated -> {
                    coroutineScope.launch {
                        val expiryMinutes = ((event.expiryTime - System.currentTimeMillis()) / 60000).toInt()
                        snackbarHostState.showSnackbar("Boost activated! Active for $expiryMinutes minutes")
                    }
                }
                is HomeEvent.ActionUndone -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Last action undone")
                    }
                }
                is HomeEvent.UserBlocked -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("User blocked")
                    }
                }
                is HomeEvent.UserReported -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("User reported")
                    }
                }
                is HomeEvent.Error -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }
    
    // Show errors in snackbar
    LaunchedEffect(uiState) {
        if (uiState is HomeUiState.Error) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar((uiState as HomeUiState.Error).message)
                viewModel.clearErrors()
            }
        }
    }
    
    // Content based on UI state
    when (uiState) {
        is HomeUiState.Loading -> {
            LoadingStateView("Finding your matches...")
        }
        is HomeUiState.Content -> {
            HomeScreenContent(
                discoverCards = discoverCards,
                topPicks = topPicks,
                likesYou = likesYou,
                recentlyActive = recentlyActive,
                remainingLikes = remainingLikes,
                remainingSuperLikes = remainingSuperLikes,
                isPremium = currentUser?.isPremium ?: false,
                onCardLike = viewModel::likeUser,
                onCardSuperLike = viewModel::superLikeUser,
                onCardPass = viewModel::passUser,
                onCardClick = onNavigateToProfile,
                onChatClick = onNavigateToChat,
                onFilterClick = onNavigateToFilter,
                onUndoClick = viewModel::undoLastAction,
                onBoostClick = viewModel::boostVisibility,
                onRefresh = viewModel::refreshAll,
                onSubscribeClick = onNavigateToSubscription,
                onReportUser = { userId ->
                    reportUserId = userId
                    showReportDialog = true
                },
                onBlockUser = { userId ->
                    blockUserId = userId
                    showBlockDialog = true
                }
            )
        }
        is HomeUiState.Empty -> {
            EmptyStateView(
                message = "No more recommendations right now",
                iconVector = Icons.Default.Refresh,
                actionText = "Refresh",
                onAction = viewModel::refreshAll
            )
        }
        is HomeUiState.Error -> {
            ErrorStateView(
                message = (uiState as HomeUiState.Error).message,
                onRetry = viewModel::refreshAll
            )
        }
    }
    
    // Match dialog
    if (showMatchDialog && matchUserId.isNotEmpty()) {
        MatchDialog(
            matchId = matchUserId,
            onDismiss = { showMatchDialog = false },
            onMessage = { 
                showMatchDialog = false
                onNavigateToChat(matchUserId)
            },
            onKeepSwiping = { showMatchDialog = false }
        )
    }
    
    // Report dialog
    if (showReportDialog && reportUserId.isNotEmpty()) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason, details ->
                viewModel.reportUser(reportUserId, reason, details)
                showReportDialog = false
            }
        )
    }
    
    // Block dialog
    if (showBlockDialog && blockUserId.isNotEmpty()) {
        BlockDialog(
            onDismiss = { showBlockDialog = false },
            onBlock = { reason ->
                viewModel.blockUser(blockUserId, reason)
                showBlockDialog = false
            }
        )
    }
}

@Composable
fun HomeScreenContent(
    discoverCards: List<DiscoverCard>,
    topPicks: List<DiscoverCard>,
    likesYou: List<User>,
    recentlyActive: List<User>,
    remainingLikes: Int,
    remainingSuperLikes: Int,
    isPremium: Boolean,
    onCardLike: (String) -> Unit,
    onCardSuperLike: (String) -> Unit,
    onCardPass: (String) -> Unit,
    onCardClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onFilterClick: () -> Unit,
    onUndoClick: () -> Unit,
    onBoostClick: () -> Unit,
    onRefresh: () -> Unit,
    onSubscribeClick: () -> Unit,
    onReportUser: (String) -> Unit,
    onBlockUser: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("OneLove") },
                actions = {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = "Filter"
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 80.dp) // Space for action buttons
            ) {
                // Main discovery cards
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .padding(16.dp)
                ) {
                    if (discoverCards.isNotEmpty()) {
                        DiscoveryCardStack(
                            cards = discoverCards,
                            onLike = onCardLike,
                            onSuperLike = onCardSuperLike,
                            onPass = onCardPass,
                            onCardClick = onCardClick,
                            onReportUser = onReportUser,
                            onBlockUser = onBlockUser
                        )
                    } else {
                        EmptyStateView(
                            message = "No more recommendations",
                            iconVector = Icons.Default.Refresh,
                            actionText = "Refresh",
                            onAction = onRefresh
                        )
                    }
                }
                
                // Action buttons
                DiscoveryActions(
                    remainingLikes = remainingLikes,
                    remainingSuperLikes = remainingSuperLikes,
                    onUndo = onUndoClick,
                    onPass = { 
                        if (discoverCards.isNotEmpty()) {
                            onCardPass(discoverCards.first().user.id)
                        }
                    },
                    onLike = { 
                        if (discoverCards.isNotEmpty()) {
                            onCardLike(discoverCards.first().user.id)
                        }
                    },
                    onSuperLike = { 
                        if (discoverCards.isNotEmpty()) {
                            onCardSuperLike(discoverCards.first().user.id)
                        }
                    },
                    onBoost = onBoostClick,
                    isPremium = isPremium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Top picks section
                if (topPicks.isNotEmpty()) {
                    SectionHeader(
                        title = "Today's Top Picks",
                        subtitle = if (isPremium) "Curated matches just for you" else "Upgrade to see who's picked for you",
                        actionText = if (isPremium) null else "Upgrade",
                        onAction = if (isPremium) null else onSubscribeClick
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topPicks) { card ->
                            TopPickCard(
                                card = card,
                                isPremium = isPremium,
                                onCardClick = { onCardClick(card.user.id) },
                                onSubscribeClick = onSubscribeClick
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Likes you section
                if (likesYou.isNotEmpty()) {
                    SectionHeader(
                        title = "Likes You",
                        subtitle = if (isPremium) "${likesYou.size} people like you" else "Upgrade to see who likes you",
                        actionText = if (isPremium) null else "Upgrade",
                        onAction = if (isPremium) null else onSubscribeClick
                    )
                    
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                    ) {
                        items(likesYou) { user ->
                            LikeCard(
                                user = user,
                                isPremium = isPremium,
                                onCardClick = { onCardClick(user.id) },
                                onSubscribeClick = onSubscribeClick
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Recently active section
                if (recentlyActive.isNotEmpty()) {
                    SectionHeader(
                        title = "Recently Active",
                        subtitle = "Users who were active in the last 24 hours"
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyActive) { user ->
                            ActiveUserCard(
                                user = user,
                                onCardClick = { onCardClick(user.id) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoveryCardStack(
    cards: List<DiscoverCard>,
    onLike: (String) -> Unit,
    onSuperLike: (String) -> Unit,
    onPass: (String) -> Unit,
    onCardClick: (String) -> Unit,
    onReportUser: (String) -> Unit,
    onBlockUser: (String) -> Unit
) {
    if (cards.isEmpty()) return
    
    // Show only the top 3 cards for performance
    val visibleCards = cards.take(3)
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Stack the cards from bottom to top
        visibleCards.reversed().forEachIndexed { index, card ->
            val isTopCard = index == visibleCards.size - 1
            
            if (isTopCard) {
                SwipeableCard(
                    card = card,
                    onLike = { onLike(card.user.id) },
                    onSuperLike = { onSuperLike(card.user.id) },
                    onPass = { onPass(card.user.id) },
                    onCardClick = { onCardClick(card.user.id) },
                    onReportUser = { onReportUser(card.user.id) },
                    onBlockUser = { onBlockUser(card.user.id) }
                )
            } else {
                // Background cards
                val scale = 0.95f - (0.05f * index)
                val yOffset = 8.dp * index
                
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = yOffset)
                        .scale(scale)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    // Just show the image for background cards
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Profile image
                        AsyncImage(
                            model = card.user.profileImageUrls.firstOrNull() ?: "",
                            contentDescription = "Profile picture of ${card.user.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeableCard(
    card: DiscoverCard,
    onLike: () -> Unit,
    onSuperLike: () -> Unit,
    onPass: () -> Unit,
    onCardClick: () -> Unit,
    onReportUser: () -> Unit,
    onBlockUser: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // State for card swiping
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val rotationAngle by animateFloatAsState(
        targetValue = offsetX * 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )
    
    // Calculate swipe progress
    val swipeThreshold = 300f
    val swipeProgress = (offsetX / swipeThreshold).coerceIn(-1f, 1f)
    
    // Like/Pass indicators
    val likeOpacity = if (swipeProgress > 0) swipeProgress else 0f
    val passOpacity = if (swipeProgress < 0) -swipeProgress else 0f
    
    // More options menu
    var showOptionsMenu by remember { mutableStateOf(false) }
    
    // Pager for profile images
    val pagerState = rememberPagerState { card.user.profileImageUrls.size.coerceAtLeast(1) }
    
    Card(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .rotate(rotationAngle)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX > swipeThreshold) {
                            // Swiped right (like)
                            coroutineScope.launch {
                                offsetX = size.width.toFloat() * 2
                                delay(300)
                                onLike()
                            }
                        } else if (offsetX < -swipeThreshold) {
                            // Swiped left (pass)
                            coroutineScope.launch {
                                offsetX = -size.width.toFloat() * 2
                                delay(300)
                                onPass()
                            }
                        } else if (offsetY < -swipeThreshold) {
                            // Swiped up (super like)
                            coroutineScope.launch {
                                offsetY = -size.height.toFloat() * 2
                                delay(300)
                                onSuperLike()
                            }
                        } else {
                            // Reset position
                            coroutineScope.launch {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile images pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onCardClick() }
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    // Profile image
                    AsyncImage(
                        model = card.user.profileImageUrls.getOrNull(page) ?: "",
                        contentDescription = "Profile picture of ${card.user.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    ),
                                    startY = 300f
                                )
                            )
                    )
                }
            }
            
            // Page indicator
            if (card.user.profileImageUrls.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.Center
                ) {
                    card.user.profileImageUrls.forEachIndexed { index, _ ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
            
            // More options button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                
                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Report") },
                        leadingIcon = { Icon(Icons.Default.Report, null) },
                        onClick = { 
                            showOptionsMenu = false
                            onReportUser() 
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Block") },
                        leadingIcon = { Icon(Icons.Default.Block, null) },
                        onClick = { 
                            showOptionsMenu = false
                            onBlockUser() 
                        }
                    )
                }
            }
            
            // User info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${card.user.name}, ${card.user.age}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (card.user.isVerified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Location
                if (card.user.location?.city != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = card.user.location.city,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Bio
                if (!card.user.bio.isNullOrEmpty()) {
                    Text(
                        text = card.user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Match reason if available
                card.recommendation.reasons.firstOrNull()?.let { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (reason.type) {
                                "INTEREST" -> Icons.Default.Favorite
                                "LOCATION" -> Icons.Default.LocationOn
                                else -> Icons.Default.Star
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reason.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Like/Pass indicators
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                // Like indicator
                Text(
                    text = "LIKE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Green,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .alpha(likeOpacity)
                        .rotate(-20f)
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Pass indicator
                Text(
                    text = "NOPE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Red,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .alpha(passOpacity)
                        .rotate(20f)
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Super Like indicator (only shown when swiping up)
                if (offsetY < -50) {
                    Text(
                        text = "SUPER LIKE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Blue,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .alpha(-offsetY / swipeThreshold)
                            .background(
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoveryActions(
    remainingLikes: Int,
    remainingSuperLikes: Int,
    onUndo: () -> Unit,
    onPass: () -> Unit,
    onLike: () -> Unit,
    onSuperLike: () -> Unit,
    onBoost: () -> Unit,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo button
        FloatingActionButton(
            onClick = onUndo,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Undo",
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Pass button
        FloatingActionButton(
            onClick = onPass,
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Pass",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Super Like button
        BadgedBox(
            badge = {
                if (remainingSuperLikes > 0) {
                    Badge {
                        Text(
                            text = "$remainingSuperLikes",
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        ) {
            FloatingActionButton(
                onClick = onSuperLike,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Super Like",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Like button
        BadgedBox(
            badge = {
                if (!isPremium && remainingLikes > 0) {
                    Badge {
                        Text(
                            text = "$remainingLikes",
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        ) {
            FloatingActionButton(
                onClick = onLike,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Boost button
        FloatingActionButton(
            onClick = onBoost,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.Thunderstorm,
                contentDescription = "Boost",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            if (actionText != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(text = actionText)
                }
            }
        }
        
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopPickCard(
    card: DiscoverCard,
    isPremium: Boolean,
    onCardClick: () -> Unit,
    onSubscribeClick: () -> Unit
) {
    ElevatedCard(
        onClick = if (isPremium) onCardClick else onSubscribeClick,
        modifier = Modifier
            .width(180.dp)
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile image
            AsyncImage(
                model = card.user.profileImageUrls.firstOrNull() ?: "",
                contentDescription = "Profile picture of ${card.user.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Premium blur overlay for non-premium users
            if (!isPremium) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Upgrade to see top picks",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 150f
                            )
                        )
                )
                
                // User info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${card.user.name}, ${card.user.age}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (card.user.location?.city != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = card.user.location.city,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Top Pick badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Top Pick",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikeCard(
    user: User,
    isPremium: Boolean,
    onCardClick: () -> Unit,
    onSubscribeClick: () -> Unit
) {
    ElevatedCard(
        onClick = if (isPremium) onCardClick else onSubscribeClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile image
            AsyncImage(
                model = user.profileImageUrls.firstOrNull() ?: "",
                contentDescription = "Profile picture of ${user.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Premium blur overlay for non-premium users
            if (!isPremium) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 100f
                            )
                        )
                )
                
                // User info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${user.name}, ${user.age}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Likes you badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Likes you",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveUserCard(
    user: User,
    onCardClick: () -> Unit
) {
    ElevatedCard(
        onClick = onCardClick,
        modifier = Modifier
            .width(120.dp)
            .height(160.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Profile image
            AsyncImage(
                model = user.profileImageUrls.firstOrNull() ?: "",
                contentDescription = "Profile picture of ${user.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )
            
            // User info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${user.age}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            
            // Active badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(
                        color = Color.Green,
                        shape = CircleShape
                    )
                    .size(8.dp)
            )
        }
    }
}

@Composable
fun MatchDialog(
    matchId: String,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onKeepSwiping: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Confetti animation could be added here
                
                Text(
                    text = "It's a Match!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "You and your match have liked each other",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Send message button
                Button(
                    onClick = onMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send a Message",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Keep swiping button
                OutlinedButton(
                    onClick = onKeepSwiping,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Keep Swiping",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onReport: (String, String?) -> Unit
) {
    val reportReasons = listOf(
        "Inappropriate photos",
        "Fake profile/Spam",
        "Offensive behavior",
        "Underage user",
        "Other"
    )
    
    var selectedReason by remember { mutableStateOf("") }
    var otherDetails by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                Text(
                    text = "Report User",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Why are you reporting this user?",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedReason == "Other") {
                    OutlinedTextField(
                        value = otherDetails,
                        onValueChange = { otherDetails = it },
                        label = { Text("Please provide details") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { 
                            onReport(
                                selectedReason, 
                                if (selectedReason == "Other") otherDetails else null
                            ) 
                        },
                        enabled = selectedReason.isNotEmpty() && 
                                 (selectedReason != "Other" || otherDetails.isNotEmpty())
                    ) {
                        Text("Report")
                    }
                }
            }
        }
    }
}

@Composable
fun BlockDialog(
    onDismiss: () -> Unit,
    onBlock: (String?) -> Unit
) {
    var blockReason by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                Text(
                    text = "Block User",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "You won't see this person anymore and they won't be able to contact you. You can unblock users from settings.",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = blockReason,
                    onValueChange = { blockReason = it },
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { 
                            onBlock(if (blockReason.isNotEmpty()) blockReason else null) 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Block")
                    }
                }
            }
        }
    }
}