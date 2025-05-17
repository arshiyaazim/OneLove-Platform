package com.kilagee.onelove.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kilagee.onelove.data.model.ThemePreference
import com.kilagee.onelove.data.model.UserPreferences
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import com.kilagee.onelove.ui.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPreferencesScreen(
    navController: NavController,
    viewModel: UserViewModel = hiltViewModel()
) {
    val preferencesState by viewModel.preferencesState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    
    // Local state for user preferences
    var minAge by remember { mutableStateOf(18) }
    var maxAge by remember { mutableStateOf(50) }
    var genderPreference by remember { mutableStateOf<String?>(null) }
    var distancePreference by remember { mutableStateOf(50) }
    var enableMatchNotifications by remember { mutableStateOf(true) }
    var enableMessageNotifications by remember { mutableStateOf(true) }
    var enableOfferNotifications by remember { mutableStateOf(true) }
    var showOnlineStatus by remember { mutableStateOf(true) }
    var showDistance by remember { mutableStateOf(true) }
    var showAge by remember { mutableStateOf(true) }
    var themePreference by remember { mutableStateOf(ThemePreference.SYSTEM) }
    
    // Initialize local state from preferences
    LaunchedEffect(preferencesState) {
        if (preferencesState is Resource.Success) {
            val prefs = (preferencesState as Resource.Success<UserPreferences>).data
            minAge = prefs.minAgePreference ?: 18
            maxAge = prefs.maxAgePreference ?: 50
            genderPreference = prefs.genderPreference
            distancePreference = prefs.distancePreferenceKm
            enableMatchNotifications = prefs.enableMatchNotifications
            enableMessageNotifications = prefs.enableMessageNotifications
            enableOfferNotifications = prefs.enableOfferNotifications
            showOnlineStatus = prefs.showOnlineStatus
            showDistance = prefs.showDistance
            showAge = prefs.showAge
            themePreference = prefs.themePreference
        }
    }
    
    // Handle update state
    LaunchedEffect(updateState) {
        if (updateState is Resource.Success) {
            // Update successful, navigate back
            navController.popBackStack()
            viewModel.clearUpdateState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Save button
                    TextButton(
                        onClick = {
                            // Get current preferences
                            if (preferencesState is Resource.Success) {
                                val currentPrefs = (preferencesState as Resource.Success<UserPreferences>).data
                                
                                // Create updated preferences
                                val updatedPrefs = currentPrefs.copy(
                                    minAgePreference = minAge,
                                    maxAgePreference = maxAge,
                                    genderPreference = genderPreference,
                                    distancePreferenceKm = distancePreference,
                                    enableMatchNotifications = enableMatchNotifications,
                                    enableMessageNotifications = enableMessageNotifications,
                                    enableOfferNotifications = enableOfferNotifications,
                                    showOnlineStatus = showOnlineStatus,
                                    showDistance = showDistance,
                                    showAge = showAge,
                                    themePreference = themePreference
                                )
                                
                                // Update preferences
                                viewModel.updateUserPreferences(updatedPrefs)
                            }
                        }
                    ) {
                        Text("Save")
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
            when (val state = preferencesState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Matching Preferences Section
                        Text(
                            text = "Matching Preferences",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Age Preference
                        Text(
                            text = "Age Range: $minAge - $maxAge",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Min Age Slider
                        Text(
                            text = "Minimum Age",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = minAge.toFloat(),
                            onValueChange = { minAge = it.toInt() },
                            valueRange = 18f..maxAge.toFloat(),
                            steps = (maxAge - 18) / 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Max Age Slider
                        Text(
                            text = "Maximum Age",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = maxAge.toFloat(),
                            onValueChange = { maxAge = it.toInt() },
                            valueRange = minAge.toFloat()..100f,
                            steps = (100 - minAge) / 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Gender Preference
                        Text(
                            text = "Gender Preference",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectableGroup()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = genderPreference == "Male",
                                        onClick = { genderPreference = "Male" },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = genderPreference == "Male",
                                    onClick = null
                                )
                                Text(
                                    text = "Male",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = genderPreference == "Female",
                                        onClick = { genderPreference = "Female" },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = genderPreference == "Female",
                                    onClick = null
                                )
                                Text(
                                    text = "Female",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = genderPreference == "Any",
                                        onClick = { genderPreference = "Any" },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = genderPreference == "Any",
                                    onClick = null
                                )
                                Text(
                                    text = "Any",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Distance Preference
                        Text(
                            text = "Maximum Distance: $distancePreference km",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = distancePreference.toFloat(),
                            onValueChange = { distancePreference = it.toInt() },
                            valueRange = 1f..100f,
                            steps = 99,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Notification Preferences Section
                        Text(
                            text = "Notification Preferences",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Match Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Match Notifications",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Switch(
                                checked = enableMatchNotifications,
                                onCheckedChange = { enableMatchNotifications = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Message Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Message Notifications",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Switch(
                                checked = enableMessageNotifications,
                                onCheckedChange = { enableMessageNotifications = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Offer Notifications
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Offer Notifications",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Switch(
                                checked = enableOfferNotifications,
                                onCheckedChange = { enableOfferNotifications = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Privacy Preferences Section
                        Text(
                            text = "Privacy Preferences",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Show Online Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Show Online Status",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Switch(
                                checked = showOnlineStatus,
                                onCheckedChange = { showOnlineStatus = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show Distance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Show Distance",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Switch(
                                checked = showDistance,
                                onCheckedChange = { showDistance = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show Age
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Show Age",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Switch(
                                checked = showAge,
                                onCheckedChange = { showAge = it }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Theme Preferences Section
                        Text(
                            text = "Theme Preferences",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectableGroup()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = themePreference == ThemePreference.LIGHT,
                                        onClick = { themePreference = ThemePreference.LIGHT },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themePreference == ThemePreference.LIGHT,
                                    onClick = null
                                )
                                Text(
                                    text = "Light",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = themePreference == ThemePreference.DARK,
                                        onClick = { themePreference = ThemePreference.DARK },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themePreference == ThemePreference.DARK,
                                    onClick = null
                                )
                                Text(
                                    text = "Dark",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = themePreference == ThemePreference.SYSTEM,
                                        onClick = { themePreference = ThemePreference.SYSTEM },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themePreference == ThemePreference.SYSTEM,
                                    onClick = null
                                )
                                Text(
                                    text = "System Default",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Save Button
                        Button(
                            onClick = {
                                // Get current preferences
                                val currentPrefs = state.data
                                
                                // Create updated preferences
                                val updatedPrefs = currentPrefs.copy(
                                    minAgePreference = minAge,
                                    maxAgePreference = maxAge,
                                    genderPreference = genderPreference,
                                    distancePreferenceKm = distancePreference,
                                    enableMatchNotifications = enableMatchNotifications,
                                    enableMessageNotifications = enableMessageNotifications,
                                    enableOfferNotifications = enableOfferNotifications,
                                    showOnlineStatus = showOnlineStatus,
                                    showDistance = showDistance,
                                    showAge = showAge,
                                    themePreference = themePreference
                                )
                                
                                // Update preferences
                                viewModel.updateUserPreferences(updatedPrefs)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Preferences")
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = { viewModel.loadUserPreferences() }
                    )
                }
            }
            
            // Show loading overlay during update
            if (updateState is Resource.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Show error dialog if update fails
            if (updateState is Resource.Error) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearUpdateState() },
                    title = { Text("Error") },
                    text = { Text((updateState as Resource.Error).message) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearUpdateState() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}