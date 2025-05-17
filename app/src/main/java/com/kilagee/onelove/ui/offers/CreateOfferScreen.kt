package com.kilagee.onelove.ui.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kilagee.onelove.data.model.OfferType
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import com.kilagee.onelove.ui.components.UserSelectionDialog
import com.kilagee.onelove.ui.viewmodels.UserViewModel
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.clock.ClockDialog
import com.maxkeppeler.sheets.clock.models.ClockConfig
import com.maxkeppeler.sheets.clock.models.ClockSelection
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOfferScreen(
    navController: NavController,
    offerViewModel: OfferViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val operationState by offerViewModel.operationState.collectAsState()
    val matchesState by userViewModel.matchesState.collectAsState()
    
    // Form state
    var selectedReceiver by remember { mutableStateOf<User?>(null) }
    var selectedOfferType by remember { mutableStateOf(OfferType.COFFEE) }
    var offerTitle by remember { mutableStateOf("") }
    var offerDescription by remember { mutableStateOf("") }
    var offerLocation by remember { mutableStateOf("") }
    var offerPoints by remember { mutableStateOf("0") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    
    // Dialog states
    var showUserSelection by remember { mutableStateOf(false) }
    
    // Date and time picker dialogs
    val calendarState = rememberUseCaseState()
    val timeState = rememberUseCaseState()
    
    // Load matches when screen is shown
    LaunchedEffect(Unit) {
        userViewModel.loadMatches()
    }
    
    // Handle operation result
    LaunchedEffect(operationState) {
        if (operationState is Resource.Success) {
            // Offer created successfully
            navController.popBackStack()
            offerViewModel.clearOperationState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Offer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Select receiver
                OutlinedButton(
                    onClick = { showUserSelection = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedReceiver?.let { "${it.firstName} ${it.lastName}" }
                                ?: "Select User"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Offer type selection
                Text(
                    text = "Offer Type",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    OfferTypeOption.values().forEach { offerTypeOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = selectedOfferType == offerTypeOption.type,
                                    onClick = { selectedOfferType = offerTypeOption.type },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOfferType == offerTypeOption.type,
                                onClick = null
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Icon(
                                imageVector = offerTypeOption.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = offerTypeOption.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                OutlinedTextField(
                    value = offerTitle,
                    onValueChange = { offerTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                OutlinedTextField(
                    value = offerDescription,
                    onValueChange = { offerDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Location
                OutlinedTextField(
                    value = offerLocation,
                    onValueChange = { offerLocation = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date and time
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Date picker
                    OutlinedButton(
                        onClick = { calendarState.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateText = selectedDate?.let {
                                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                val date = Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
                                formatter.format(date)
                            } ?: "Select Date"
                            Text(text = dateText)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Time picker
                    OutlinedButton(
                        onClick = { timeState.show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val timeText = selectedTime?.let {
                                val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                                val calendar = Calendar.getInstance()
                                calendar.set(Calendar.HOUR_OF_DAY, it.hour)
                                calendar.set(Calendar.MINUTE, it.minute)
                                formatter.format(calendar.time)
                            } ?: "Select Time"
                            Text(text = timeText)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Points
                OutlinedTextField(
                    value = offerPoints,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d+\$"))) offerPoints = it },
                    label = { Text("Points Offered") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Submit button
                Button(
                    onClick = {
                        val proposedTime = if (selectedDate != null && selectedTime != null) {
                            val date = selectedDate!!
                            val time = selectedTime!!
                            val dateTime = date.atTime(time)
                            Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
                        } else {
                            null
                        }
                        
                        offerViewModel.createOffer(
                            receiverId = selectedReceiver?.id ?: return@Button,
                            type = selectedOfferType,
                            title = offerTitle,
                            description = offerDescription,
                            location = offerLocation,
                            proposedTime = proposedTime,
                            pointsOffered = offerPoints.toIntOrNull() ?: 0
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedReceiver != null && offerTitle.isNotBlank()
                ) {
                    Text("Send Offer")
                }
            }
            
            // Operation state overlay
            when (val state = operationState) {
                is Resource.Loading -> {
                    LoadingStateView()
                }
                is Resource.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetryClick = { offerViewModel.clearOperationState() }
                    )
                }
                else -> {}
            }
            
            // User selection dialog
            if (showUserSelection) {
                when (val state = matchesState) {
                    is Resource.Success -> {
                        UserSelectionDialog(
                            users = state.data,
                            onUserSelected = {
                                selectedReceiver = it
                                showUserSelection = false
                            },
                            onDismiss = { showUserSelection = false }
                        )
                    }
                    is Resource.Error -> {
                        AlertDialog(
                            onDismissRequest = { showUserSelection = false },
                            title = { Text("Error") },
                            text = { Text("Failed to load matches: ${state.message}") },
                            confirmButton = {
                                Button(onClick = { showUserSelection = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                    is Resource.Loading -> {
                        AlertDialog(
                            onDismissRequest = { },
                            title = { Text("Loading") },
                            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
                            confirmButton = { }
                        )
                    }
                }
            }
            
            // Date picker
            CalendarDialog(
                state = calendarState,
                config = CalendarConfig(
                    monthSelection = true,
                    yearSelection = true
                ),
                selection = CalendarSelection.Date { date ->
                    selectedDate = date
                }
            )
            
            // Time picker
            ClockDialog(
                state = timeState,
                config = ClockConfig(
                    is24HourFormat = false
                ),
                selection = ClockSelection.HoursMinutes { hours, minutes ->
                    selectedTime = LocalTime.of(hours, minutes)
                }
            )
        }
    }
}

enum class OfferTypeOption(
    val type: OfferType,
    val label: String,
    val icon: ImageVector
) {
    COFFEE(OfferType.COFFEE, "Coffee", Icons.Filled.Coffee),
    DINNER(OfferType.DINNER, "Dinner", Icons.Filled.Restaurant),
    MOVIE(OfferType.MOVIE, "Movie", Icons.Filled.Movie),
    DRINKS(OfferType.DRINKS, "Drinks", Icons.Filled.LocalBar),
    WALK(OfferType.WALK, "Walk", Icons.Filled.DirectionsWalk),
    VIDEO_CALL(OfferType.VIDEO_CALL, "Video Call", Icons.Filled.VideoCall),
    TRAVEL(OfferType.TRAVEL, "Travel", Icons.Filled.Flight),
    CUSTOM(OfferType.CUSTOM, "Custom", Icons.Filled.Event)
}