package com.kilagee.onelove.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kilagee.onelove.domain.model.AdminAction
import com.kilagee.onelove.domain.model.AdminPermission
import com.kilagee.onelove.domain.model.AdminRole
import com.kilagee.onelove.domain.model.AdminUser
import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.model.AppSettings
import com.kilagee.onelove.domain.model.NotificationPriority
import com.kilagee.onelove.domain.model.NotificationTargetType
import com.kilagee.onelove.domain.model.SubscriptionRequest
import com.kilagee.onelove.domain.model.SubscriptionRequestStatus
import com.kilagee.onelove.domain.model.UserProfile
import com.kilagee.onelove.domain.model.VerificationRequest
import com.kilagee.onelove.domain.model.VerificationStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val currentAdminUser by viewModel.currentAdminUser.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Show error message in snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearError()
        }
    }

    // Show success message in snackbar
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearSuccessMessage()
        }
    }

    // Check if user is an admin
    if (isAdmin.not()) {
        AdminAccessDeniedScreen(onNavigateBack = onNavigateBack)
        return
    }

    // Navigation Drawer Items
    val drawerItems = listOf(
        DrawerItem(
            title = "Dashboard",
            icon = Icons.Filled.Dashboard,
            tab = AdminPanelTab.DASHBOARD
        ),
        DrawerItem(
            title = "User Management",
            icon = Icons.Filled.People,
            tab = AdminPanelTab.USERS
        ),
        DrawerItem(
            title = "Verifications",
            icon = Icons.Filled.Check,
            tab = AdminPanelTab.VERIFICATION
        ),
        DrawerItem(
            title = "Subscriptions",
            icon = Icons.Outlined.Person,
            tab = AdminPanelTab.SUBSCRIPTIONS
        ),
        DrawerItem(
            title = "AI Profiles",
            icon = Icons.Outlined.SmartToy,
            tab = AdminPanelTab.AI_PROFILES
        ),
        DrawerItem(
            title = "App Settings",
            icon = Icons.Filled.Settings,
            tab = AdminPanelTab.APP_SETTINGS
        ),
        DrawerItem(
            title = "Analytics",
            icon = Icons.Outlined.Analytics,
            tab = AdminPanelTab.ANALYTICS
        ),
        DrawerItem(
            title = "Flagged Content",
            icon = Icons.Outlined.Flag,
            tab = AdminPanelTab.FLAGGED_CONTENT
        ),
        DrawerItem(
            title = "Activity Logs",
            icon = Icons.Outlined.History,
            tab = AdminPanelTab.ACTIVITY_LOGS
        )
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Admin header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OneLove Admin",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    currentAdminUser?.let { admin ->
                        Text(
                            text = "Logged in as: ${admin.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "Role: ${admin.role.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Divider()
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Drawer items
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(text = item.title) },
                        selected = selectedTab == item.tab,
                        onClick = {
                            viewModel.switchTab(item.tab)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = getScreenTitle(selectedTab))
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        content = {
                            Text(text = data.visuals.message)
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Show loading indicator
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)
                    )
                }
                
                // Content based on selected tab
                when (selectedTab) {
                    AdminPanelTab.DASHBOARD -> {
                        AdminDashboardScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.USERS -> {
                        UserManagementScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.VERIFICATION -> {
                        VerificationRequestsScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.SUBSCRIPTIONS -> {
                        SubscriptionRequestsScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.AI_PROFILES -> {
                        AIProfilesScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.APP_SETTINGS -> {
                        AppSettingsScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.ANALYTICS -> {
                        AnalyticsScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.FLAGGED_CONTENT -> {
                        FlaggedContentScreen(viewModel = viewModel)
                    }
                    
                    AdminPanelTab.ACTIVITY_LOGS -> {
                        ActivityLogsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAccessDeniedScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Access Denied",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Access Denied",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You don't have permission to access the admin panel.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNavigateBack
        ) {
            Text(text = "Go Back")
        }
    }
}

@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel) {
    val verificationRequests by viewModel.verificationRequests.collectAsState()
    val subscriptionRequests by viewModel.subscriptionRequests.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Users",
                    value = (analyticsData["totalUsers"] as? Number)?.toInt()?.toString() ?: "0",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatCard(
                    title = "Premium",
                    value = (analyticsData["premiumUsers"] as? Number)?.toInt()?.toString() ?: "0",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatCard(
                    title = "Verified",
                    value = (analyticsData["verifiedUsers"] as? Number)?.toInt()?.toString() ?: "0",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        
        // Pending verification requests
        item {
            Text(
                text = "Pending Verification Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (verificationRequests.isEmpty()) {
            item {
                EmptyStateMessage(message = "No pending verification requests")
            }
        } else {
            items(verificationRequests.take(5)) { request ->
                VerificationRequestItem(
                    request = request,
                    onApprove = { viewModel.approveVerification(request.id) },
                    onReject = { reason -> viewModel.rejectVerification(request.id, reason) }
                )
            }
            
            item {
                if (verificationRequests.size > 5) {
                    TextButton(
                        onClick = { viewModel.switchTab(AdminPanelTab.VERIFICATION) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "View All Verification Requests")
                    }
                }
            }
        }
        
        // Pending subscription requests
        item {
            Text(
                text = "Pending Subscription Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (subscriptionRequests.isEmpty()) {
            item {
                EmptyStateMessage(message = "No pending subscription requests")
            }
        } else {
            items(subscriptionRequests.take(5)) { request ->
                SubscriptionRequestItem(
                    request = request,
                    onApprove = { viewModel.approveSubscription(request.id) },
                    onReject = { reason -> viewModel.rejectSubscription(request.id, reason) }
                )
            }
            
            item {
                if (subscriptionRequests.size > 5) {
                    TextButton(
                        onClick = { viewModel.switchTab(AdminPanelTab.SUBSCRIPTIONS) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "View All Subscription Requests")
                    }
                }
            }
        }
        
        // Quick actions
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionButton(
                    text = "User Management",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.switchTab(AdminPanelTab.USERS) }
                )
                
                ActionButton(
                    text = "App Settings",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.switchTab(AdminPanelTab.APP_SETTINGS) }
                )
                
                ActionButton(
                    text = "Send Notification",
                    icon = Icons.Default.Notifications,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.switchTab(AdminPanelTab.APP_SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun UserManagementScreen(viewModel: AdminViewModel) {
    val users by viewModel.users.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadUsers()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.loadUsers(if (it.isNotEmpty()) it else null)
            },
            label = { Text("Search Users") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Users list or user details
        if (selectedUser != null) {
            UserDetailsScreen(
                user = selectedUser!!,
                onBack = { viewModel.loadUserProfile("") },
                onUpdateUser = { viewModel.updateUserProfile(it) },
                onDeleteUser = { userId, userName -> viewModel.deleteUser(userId, userName) }
            )
        } else {
            if (users.isEmpty()) {
                EmptyStateMessage(message = "No users found")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        UserItem(
                            user = user,
                            onClick = { viewModel.loadUserProfile(user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserItem(
    user: UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            AsyncImage(
                model = user.photoUrl ?: "https://via.placeholder.com/50",
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user.isPremium) {
                        StatusTag(label = "Premium", color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    if (user.isVerified) {
                        StatusTag(label = "Verified", color = MaterialTheme.colorScheme.primary)
                    }
                    
                    if (user.isBanned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusTag(label = "Banned", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            // Edit icon
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit User"
                )
            }
        }
    }
}

@Composable
fun UserDetailsScreen(
    user: UserProfile,
    onBack: () -> Unit,
    onUpdateUser: (UserProfile) -> Unit,
    onDeleteUser: (String, String) -> Unit
) {
    var updatedUser by remember { mutableStateOf(user) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Back button
        TextButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
            Text(text = "Back to Users List")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // User profile header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            AsyncImage(
                model = user.photoUrl ?: "https://via.placeholder.com/80",
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = "Account Created: ${formatTimestamp(user.createdAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        // User profile fields
        UserProfileField(
            label = "Display Name",
            value = updatedUser.displayName,
            onValueChange = { updatedUser = updatedUser.copy(displayName = it) }
        )
        
        UserProfileField(
            label = "Email",
            value = updatedUser.email,
            onValueChange = { updatedUser = updatedUser.copy(email = it) },
            enabled = false
        )
        
        UserProfileField(
            label = "Bio",
            value = updatedUser.bio ?: "",
            onValueChange = { updatedUser = updatedUser.copy(bio = it) }
        )
        
        // Toggle buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleButton(
                label = "Premium Status",
                isActive = updatedUser.isPremium,
                onToggle = { updatedUser = updatedUser.copy(isPremium = it) },
                activeColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            
            ToggleButton(
                label = "Verification Status",
                isActive = updatedUser.isVerified,
                onToggle = { updatedUser = updatedUser.copy(isVerified = it) },
                activeColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            ToggleButton(
                label = "Ban User",
                isActive = updatedUser.isBanned,
                onToggle = { updatedUser = updatedUser.copy(isBanned = it) },
                activeColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Ban reason field (only visible if isBanned is true)
        if (updatedUser.isBanned) {
            UserProfileField(
                label = "Ban Reason",
                value = updatedUser.banReason ?: "",
                onValueChange = { updatedUser = updatedUser.copy(banReason = it) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Save and Delete buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onUpdateUser(updatedUser) },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Save Changes")
            }
            
            OutlinedButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete User"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Delete User")
            }
        }
        
        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(text = "Delete User") },
                text = { Text(text = "Are you sure you want to delete ${user.displayName}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteUser(user.id, user.displayName)
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text(text = "Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmation = false }
                    ) {
                        Text(text = "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun UserProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
    }
}

@Composable
fun ToggleButton(
    label: String,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        
        Switch(
            checked = isActive,
            onCheckedChange = onToggle,
            activeColor = activeColor
        )
    }
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (checked) activeColor else Color.Gray.copy(alpha = 0.5f))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(5.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun VerificationRequestsScreen(viewModel: AdminViewModel) {
    val verificationRequests by viewModel.verificationRequests.collectAsState()
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadVerificationRequests()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Pending Verification Requests",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (verificationRequests.isEmpty()) {
            EmptyStateMessage(message = "No pending verification requests")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(verificationRequests) { request ->
                    VerificationRequestItem(
                        request = request,
                        onApprove = { viewModel.approveVerification(request.id) },
                        onReject = { reason -> viewModel.rejectVerification(request.id, reason) }
                    )
                }
            }
        }
    }
}

@Composable
fun VerificationRequestItem(
    request: VerificationRequest,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                AsyncImage(
                    model = request.userPhotoUrl.ifEmpty { "https://via.placeholder.com/40" },
                    contentDescription = "User Photo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // User info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = request.userDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Submitted: ${formatTimestamp(request.submittedAt)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Status tag
                StatusTag(
                    label = request.status.name,
                    color = when (request.status) {
                        VerificationStatus.PENDING -> MaterialTheme.colorScheme.secondary
                        VerificationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        VerificationStatus.REJECTED -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Document info
            Text(
                text = "Document Type: ${request.documentType}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Document images (preview)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (request.documentUrl.isNotEmpty()) {
                    AsyncImage(
                        model = request.documentUrl,
                        contentDescription = "Document Image",
                        modifier = Modifier
                            .height(120.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                if (request.selfieUrl.isNotEmpty()) {
                    AsyncImage(
                        model = request.selfieUrl,
                        contentDescription = "Selfie Image",
                        modifier = Modifier
                            .height(120.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            if (request.additionalInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Additional Information:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = request.additionalInfo,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Approval/Rejection buttons (only for PENDING requests)
            if (request.status == VerificationStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Approve"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Approve")
                    }
                    
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reject"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reject")
                    }
                }
            }
        }
    }
    
    // Reject dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text(text = "Reject Verification Request") },
            text = {
                Column {
                    Text(text = "Please provide a reason for rejection:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectReason)
                        showRejectDialog = false
                        rejectReason = ""
                    },
                    enabled = rejectReason.isNotEmpty()
                ) {
                    Text(text = "Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        rejectReason = ""
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
fun SubscriptionRequestsScreen(viewModel: AdminViewModel) {
    val subscriptionRequests by viewModel.subscriptionRequests.collectAsState()
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadSubscriptionRequests()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Pending Subscription Requests",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (subscriptionRequests.isEmpty()) {
            EmptyStateMessage(message = "No pending subscription requests")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(subscriptionRequests) { request ->
                    SubscriptionRequestItem(
                        request = request,
                        onApprove = { viewModel.approveSubscription(request.id) },
                        onReject = { reason -> viewModel.rejectSubscription(request.id, reason) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionRequestItem(
    request: SubscriptionRequest,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = request.userDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = request.userEmail,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Submitted: ${formatTimestamp(request.submittedAt)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Status tag
                StatusTag(
                    label = request.status.name,
                    color = when (request.status) {
                        SubscriptionRequestStatus.PENDING -> MaterialTheme.colorScheme.secondary
                        SubscriptionRequestStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        SubscriptionRequestStatus.REJECTED -> MaterialTheme.colorScheme.error
                        SubscriptionRequestStatus.CANCELLED -> Color.Gray
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subscription details
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    DetailItem(label = "Tier", value = request.tierName)
                    DetailItem(label = "Duration", value = "${request.duration} days")
                    DetailItem(label = "Payment Method", value = request.paymentMethod)
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    DetailItem(
                        label = "Amount",
                        value = "$${request.amount} ${request.currency}"
                    )
                    
                    if (request.paymentProofUrl != null) {
                        TextButton(onClick = { /* Open payment proof */ }) {
                            Text(text = "View Payment Proof")
                        }
                    }
                }
            }
            
            if (request.notes?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Notes:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = request.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Approval/Rejection buttons (only for PENDING requests)
            if (request.status == SubscriptionRequestStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Approve"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Approve")
                    }
                    
                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reject"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reject")
                    }
                }
            }
        }
    }
    
    // Reject dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text(text = "Reject Subscription Request") },
            text = {
                Column {
                    Text(text = "Please provide a reason for rejection:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectReason)
                        showRejectDialog = false
                        rejectReason = ""
                    },
                    enabled = rejectReason.isNotEmpty()
                ) {
                    Text(text = "Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        rejectReason = ""
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
fun AIProfilesScreen(viewModel: AdminViewModel) {
    val aiProfiles by viewModel.aiProfiles.collectAsState()
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<AIProfile?>(null) }
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadAIProfiles()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "AI Profiles",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (aiProfiles.isEmpty()) {
                EmptyStateMessage(message = "No AI profiles found")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(aiProfiles) { profile ->
                        AIProfileItem(
                            profile = profile,
                            onEdit = {
                                selectedProfile = profile
                                showCreateEditDialog = true
                            },
                            onDelete = { viewModel.deleteAIProfile(profile.id, profile.name) }
                        )
                    }
                }
            }
        }
        
        // FAB to add new AI profile
        FloatingActionButton(
            onClick = {
                selectedProfile = null
                showCreateEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add AI Profile"
            )
        }
    }
    
    // Create/Edit dialog
    if (showCreateEditDialog) {
        AIProfileDialog(
            profile = selectedProfile,
            onDismiss = { showCreateEditDialog = false },
            onSave = { profile ->
                viewModel.saveAIProfile(profile)
                showCreateEditDialog = false
            }
        )
    }
}

@Composable
fun AIProfileItem(
    profile: AIProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI avatar
                AsyncImage(
                    model = profile.photoUrl.ifEmpty { "https://via.placeholder.com/50" },
                    contentDescription = "AI Profile Photo",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Profile info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusTag(label = profile.personalityType, color = MaterialTheme.colorScheme.tertiary)
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (profile.isPremium) {
                            StatusTag(label = "Premium", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                // Edit and Delete buttons
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile"
                    )
                }
                
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Profile",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile details
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Response count
            Text(
                text = "Responses: ${profile.responses.size}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = "Delete AI Profile") },
            text = { Text(text = "Are you sure you want to delete ${profile.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
fun AIProfileDialog(
    profile: AIProfile?,
    onDismiss: () -> Unit,
    onSave: (AIProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var photoUrl by remember { mutableStateOf(profile?.photoUrl ?: "") }
    var personalityType by remember { mutableStateOf(profile?.personalityType ?: "Friendly") }
    var isPremium by remember { mutableStateOf(profile?.isPremium ?: false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (profile == null) "Create AI Profile" else "Edit AI Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("Photo URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = personalityType,
                    onValueChange = { personalityType = it },
                    label = { Text("Personality Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Premium Profile",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Switch(
                        checked = isPremium,
                        onCheckedChange = { isPremium = it },
                        activeColor = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(text = "Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val updatedProfile = (profile ?: AIProfile()).copy(
                                name = name,
                                bio = bio,
                                photoUrl = photoUrl,
                                personalityType = personalityType,
                                isPremium = isPremium
                            )
                            onSave(updatedProfile)
                        },
                        enabled = name.isNotEmpty() && bio.isNotEmpty()
                    ) {
                        Text(text = "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AppSettingsScreen(viewModel: AdminViewModel) {
    val appSettings by viewModel.appSettings.collectAsState()
    var updatedSettings by remember { mutableStateOf(appSettings) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(appSettings) {
        updatedSettings = appSettings
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App info
        SettingsSection(title = "App Information") {
            SettingsField(
                label = "App Name",
                value = updatedSettings.appName,
                onValueChange = { updatedSettings = updatedSettings.copy(appName = it) }
            )
            
            SettingsField(
                label = "App Version",
                value = updatedSettings.appVersion,
                onValueChange = { updatedSettings = updatedSettings.copy(appVersion = it) }
            )
        }
        
        // App Modes
        SettingsSection(title = "System Status") {
            SettingsToggle(
                label = "Maintenance Mode",
                isChecked = updatedSettings.maintenanceMode,
                onCheckedChange = { updatedSettings = updatedSettings.copy(maintenanceMode = it) }
            )
            
            if (updatedSettings.maintenanceMode) {
                SettingsField(
                    label = "Maintenance Message",
                    value = updatedSettings.maintenanceMessage,
                    onValueChange = { updatedSettings = updatedSettings.copy(maintenanceMessage = it) }
                )
            }
            
            SettingsToggle(
                label = "Enable Registration",
                isChecked = updatedSettings.enableRegistration,
                onCheckedChange = { updatedSettings = updatedSettings.copy(enableRegistration = it) }
            )
            
            SettingsToggle(
                label = "Enable Matching",
                isChecked = updatedSettings.enableMatching,
                onCheckedChange = { updatedSettings = updatedSettings.copy(enableMatching = it) }
            )
            
            SettingsToggle(
                label = "Enable AI Profiles",
                isChecked = updatedSettings.enableAIProfiles,
                onCheckedChange = { updatedSettings = updatedSettings.copy(enableAIProfiles = it) }
            )
            
            SettingsToggle(
                label = "Enable Calling",
                isChecked = updatedSettings.enableCalling,
                onCheckedChange = { updatedSettings = updatedSettings.copy(enableCalling = it) }
            )
        }
        
        // User Limits
        SettingsSection(title = "User Limits") {
            SettingsNumberField(
                label = "Max Matches Per Day",
                value = updatedSettings.maxMatchesPerDay,
                onValueChange = { updatedSettings = updatedSettings.copy(maxMatchesPerDay = it) }
            )
            
            SettingsNumberField(
                label = "Max Offers Per Day",
                value = updatedSettings.maxOffersPerDay,
                onValueChange = { updatedSettings = updatedSettings.copy(maxOffersPerDay = it) }
            )
            
            SettingsNumberField(
                label = "Age Restriction",
                value = updatedSettings.ageRestriction,
                onValueChange = { updatedSettings = updatedSettings.copy(ageRestriction = it) }
            )
        }
        
        // Verification settings
        SettingsSection(title = "Verification Settings") {
            SettingsToggle(
                label = "Require Email Verification",
                isChecked = updatedSettings.requireEmailVerification,
                onCheckedChange = { updatedSettings = updatedSettings.copy(requireEmailVerification = it) }
            )
            
            SettingsToggle(
                label = "Require Phone Verification",
                isChecked = updatedSettings.requirePhoneVerification,
                onCheckedChange = { updatedSettings = updatedSettings.copy(requirePhoneVerification = it) }
            )
            
            SettingsToggle(
                label = "Verified Badge Enabled",
                isChecked = updatedSettings.verifiedBadgeEnabled,
                onCheckedChange = { updatedSettings = updatedSettings.copy(verifiedBadgeEnabled = it) }
            )
        }
        
        // Points system
        SettingsSection(title = "Points System") {
            SettingsToggle(
                label = "Points System Enabled",
                isChecked = updatedSettings.pointsSystemEnabled,
                onCheckedChange = { updatedSettings = updatedSettings.copy(pointsSystemEnabled = it) }
            )
            
            if (updatedSettings.pointsSystemEnabled) {
                // Points per action
                Text(
                    text = "Points Per Action",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                updatedSettings.pointsPerAction.forEach { (action, points) ->
                    SettingsNumberField(
                        label = actionToLabel(action),
                        value = points,
                        onValueChange = {
                            val updatedPointsMap = updatedSettings.pointsPerAction.toMutableMap()
                            updatedPointsMap[action] = it
                            updatedSettings = updatedSettings.copy(pointsPerAction = updatedPointsMap)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.updateAppSettings(updatedSettings) },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Save Settings")
            }
            
            OutlinedButton(
                onClick = { showNotificationDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Send Notification"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Send System Message")
            }
        }
    }
    
    // Notification dialog
    if (showNotificationDialog) {
        NotificationDialog(
            onDismiss = { showNotificationDialog = false },
            onSend = { title, message, targetType, priority ->
                viewModel.sendSystemNotification(
                    title = title,
                    message = message,
                    targetType = targetType,
                    priority = priority
                )
                showNotificationDialog = false
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { 
                val newValue = it.toIntOrNull() ?: 0
                onValueChange(newValue)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsToggle(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun NotificationDialog(
    onDismiss: () -> Unit,
    onSend: (String, String, NotificationTargetType, NotificationPriority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedTargetType by remember { mutableStateOf(NotificationTargetType.ALL_USERS) }
    var selectedPriority by remember { mutableStateOf(NotificationPriority.NORMAL) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Send System Notification",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Notification Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Target Type
                Text(
                    text = "Target Audience",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val targetOptions = listOf(
                    NotificationTargetType.ALL_USERS to "All Users",
                    NotificationTargetType.PREMIUM_USERS to "Premium Users Only",
                    NotificationTargetType.NON_PREMIUM_USERS to "Non-Premium Users",
                    NotificationTargetType.NEW_USERS to "New Users"
                )
                
                targetOptions.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedTargetType = type },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTargetType == type,
                            onClick = { selectedTargetType = type }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Priority
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val priorityOptions = listOf(
                    NotificationPriority.LOW to "Low",
                    NotificationPriority.NORMAL to "Normal",
                    NotificationPriority.HIGH to "High",
                    NotificationPriority.URGENT to "Urgent"
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    priorityOptions.forEach { (priority, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedPriority = priority }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedPriority == priority) 
                                            getPriorityColor(priority) 
                                        else 
                                            Color.Gray.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedPriority == priority) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(text = "Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            onSend(title, message, selectedTargetType, selectedPriority)
                        },
                        enabled = title.isNotEmpty() && message.isNotEmpty()
                    ) {
                        Text(text = "Send Notification")
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsScreen(viewModel: AdminViewModel) {
    val analyticsData by viewModel.analyticsData.collectAsState()
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadAnalytics()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "App Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // User Stats
        AnalyticsSection(title = "User Statistics") {
            AnalyticsRow(
                items = listOf(
                    AnalyticsItem(
                        label = "Total Users",
                        value = (analyticsData["totalUsers"] as? Number)?.toInt()?.toString() ?: "0",
                        color = MaterialTheme.colorScheme.primary
                    ),
                    AnalyticsItem(
                        label = "Premium Users",
                        value = (analyticsData["premiumUsers"] as? Number)?.toInt()?.toString() ?: "0",
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    AnalyticsItem(
                        label = "Verified Users",
                        value = (analyticsData["verifiedUsers"] as? Number)?.toInt()?.toString() ?: "0",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            
            AnalyticsRow(
                items = listOf(
                    AnalyticsItem(
                        label = "Daily Active",
                        value = (analyticsData["dailyActiveUsers"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "Monthly Active",
                        value = (analyticsData["monthlyActiveUsers"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "New Users",
                        value = (analyticsData["newUsersInPeriod"] as? Number)?.toInt()?.toString() ?: "0"
                    )
                )
            )
        }
        
        // Engagement Stats
        AnalyticsSection(title = "Engagement") {
            AnalyticsRow(
                items = listOf(
                    AnalyticsItem(
                        label = "Matches Made",
                        value = (analyticsData["matchesMade"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "Messages Sent",
                        value = (analyticsData["messagesSent"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "Avg. Session",
                        value = "${(analyticsData["averageSessionDuration"] as? Number)?.toDouble()?.toString() ?: "0"} min"
                    )
                )
            )
            
            AnalyticsRow(
                items = listOf(
                    AnalyticsItem(
                        label = "Calls Initiated",
                        value = (analyticsData["callsInitiated"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "Calls Completed",
                        value = (analyticsData["callsCompleted"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "Completion Rate",
                        value = calculateCompletionRate(
                            analyticsData["callsCompleted"] as? Number,
                            analyticsData["callsInitiated"] as? Number
                        )
                    )
                )
            )
        }
        
        // Offer Stats
        AnalyticsSection(title = "Offers & Conversion") {
            AnalyticsRow(
                items = listOf(
                    AnalyticsItem(
                        label = "Offers Created",
                        value = (analyticsData["offersCreated"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "Offers Accepted",
                        value = (analyticsData["offersAccepted"] as? Number)?.toInt()?.toString() ?: "0"
                    ),
                    AnalyticsItem(
                        label = "Acceptance Rate",
                        value = calculateCompletionRate(
                            analyticsData["offersAccepted"] as? Number,
                            analyticsData["offersCreated"] as? Number
                        )
                    )
                )
            )
            
            AnalyticsRow(
                items = listOf(
                    AnalyticsItem(
                        label = "Premium Conversion",
                        value = "${((analyticsData["premiumConversionRate"] as? Number)?.toDouble()?.times(100))?.toString() ?: "0"}%"
                    ),
                    AnalyticsItem(
                        label = "Verification Rate",
                        value = "${((analyticsData["verificationRate"] as? Number)?.toDouble()?.times(100))?.toString() ?: "0"}%"
                    )
                )
            )
        }
    }
}

@Composable
fun AnalyticsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        content()
    }
}

@Composable
fun AnalyticsRow(
    items: List<AnalyticsItem>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEach { item ->
            AnalyticsCard(
                label = item.label,
                value = item.value,
                color = item.color,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Fill any remaining slots with empty boxes for layout consistency
        if (items.size < 3) {
            repeat(3 - items.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AnalyticsCard(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class AnalyticsItem(
    val label: String,
    val value: String,
    val color: Color = Color.Unspecified
)

@Composable
fun FlaggedContentScreen(viewModel: AdminViewModel) {
    val flaggedContent by viewModel.flaggedContent.collectAsState()
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadFlaggedContent()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Flagged Content",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (flaggedContent.isEmpty()) {
            EmptyStateMessage(message = "No flagged content to review")
        } else {
            // Implementation would depend on the structure of flagged content
            Text(
                text = "${flaggedContent.size} items flagged for review",
                style = MaterialTheme.typography.bodyLarge
            )
            
            // This is a placeholder - actual implementation would require knowledge of
            // how flagged content is structured
            Text(
                text = "Flagged content review UI would be implemented here based on the actual data structure.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun ActivityLogsScreen(viewModel: AdminViewModel) {
    val adminLogs by viewModel.adminLogs.collectAsState()
    
    LaunchedEffect(key1 = Unit) {
        viewModel.loadAdminLogs()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Admin Activity Logs",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (adminLogs.isEmpty()) {
            EmptyStateMessage(message = "No admin activity logs found")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(adminLogs) { log ->
                    AdminLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun AdminLogItem(log: AdminLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getActionColor(log.action).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getActionIcon(log.action),
                    contentDescription = null,
                    tint = getActionColor(log.action)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Log details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${log.adminName} - ${formatTimestamp(log.timestamp)}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "${log.action.name} ${log.targetType.name} (${log.targetId})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatusTag(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// Helper functions
fun getScreenTitle(tab: AdminPanelTab): String {
    return when (tab) {
        AdminPanelTab.DASHBOARD -> "Admin Dashboard"
        AdminPanelTab.USERS -> "User Management"
        AdminPanelTab.VERIFICATION -> "Verification Requests"
        AdminPanelTab.SUBSCRIPTIONS -> "Subscription Requests"
        AdminPanelTab.AI_PROFILES -> "AI Profiles"
        AdminPanelTab.APP_SETTINGS -> "App Settings"
        AdminPanelTab.ANALYTICS -> "Analytics"
        AdminPanelTab.FLAGGED_CONTENT -> "Flagged Content"
        AdminPanelTab.ACTIVITY_LOGS -> "Activity Logs"
    }
}

fun formatTimestamp(timestamp: Long): String {
    // In a real app, format this properly
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm").format(java.util.Date(timestamp))
}

fun actionToLabel(action: String): String {
    return when (action) {
        "daily_login" -> "Daily Login"
        "profile_completion" -> "Profile Completion"
        "upload_photo" -> "Upload Photo"
        "match_made" -> "Match Made"
        "message_sent" -> "Message Sent"
        "call_made" -> "Call Made"
        "video_call_made" -> "Video Call Made"
        "offer_sent" -> "Offer Sent"
        "offer_accepted" -> "Offer Accepted"
        "verification_completed" -> "Verification Completed"
        else -> action.replace("_", " ").capitalize()
    }
}

fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun calculateCompletionRate(completed: Number?, total: Number?): String {
    if (completed == null || total == null || total.toDouble() == 0.0) {
        return "0%"
    }
    
    val percentage = (completed.toDouble() / total.toDouble()) * 100
    return "${percentage.toInt()}%"
}

fun getPriorityColor(priority: NotificationPriority): Color {
    return when (priority) {
        NotificationPriority.LOW -> Color(0xFF8BC34A)     // Green
        NotificationPriority.NORMAL -> Color(0xFF2196F3)  // Blue
        NotificationPriority.HIGH -> Color(0xFFFFC107)    // Yellow
        NotificationPriority.URGENT -> Color(0xFFE91E63)  // Red
    }
}

fun getActionColor(action: AdminAction): Color {
    return when (action) {
        AdminAction.VIEW -> Color(0xFF2196F3)            // Blue
        AdminAction.CREATE -> Color(0xFF4CAF50)          // Green
        AdminAction.UPDATE -> Color(0xFFFF9800)          // Orange
        AdminAction.DELETE -> Color(0xFFF44336)          // Red
        AdminAction.APPROVE -> Color(0xFF8BC34A)         // Light Green
        AdminAction.REJECT -> Color(0xFFE91E63)          // Pink
        AdminAction.SEND_NOTIFICATION -> Color(0xFF9C27B0) // Purple
        AdminAction.MODIFY_SETTINGS -> Color(0xFF009688) // Teal
        AdminAction.LOGIN, AdminAction.LOGOUT -> Color(0xFF607D8B) // Blue Grey
    }
}

fun getActionIcon(action: AdminAction): ImageVector {
    return when (action) {
        AdminAction.VIEW -> Icons.Default.Search
        AdminAction.CREATE -> Icons.Default.Add
        AdminAction.UPDATE -> Icons.Default.Edit
        AdminAction.DELETE -> Icons.Default.Delete
        AdminAction.APPROVE -> Icons.Default.Check
        AdminAction.REJECT -> Icons.Default.Close
        AdminAction.SEND_NOTIFICATION -> Icons.Default.Notifications
        AdminAction.MODIFY_SETTINGS -> Icons.Default.Settings
        AdminAction.LOGIN, AdminAction.LOGOUT -> Icons.Default.Person
    }
}

data class DrawerItem(
    val title: String,
    val icon: ImageVector,
    val tab: AdminPanelTab
)