package com.kilagee.onelove.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.data.model.User

/**
 * Edit Profile Screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.user.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    val nameInput by viewModel.nameInput.collectAsState()
    val bioInput by viewModel.bioInput.collectAsState()
    val ageInput by viewModel.ageInput.collectAsState()
    val genderInput by viewModel.genderInput.collectAsState()
    val locationInput by viewModel.locationInput.collectAsState()
    val interestsInput by viewModel.interestsInput.collectAsState()
    val lookingForInput by viewModel.lookingForInput.collectAsState()
    val minAgePreference by viewModel.minAgePreferenceInput.collectAsState()
    val maxAgePreference by viewModel.maxAgePreferenceInput.collectAsState()
    val maxDistance by viewModel.maxDistanceInput.collectAsState()
    val selectedPhotoUris by viewModel.selectedPhotoUris.collectAsState()
    val currentPhotos by viewModel.currentPhotos.collectAsState()
    
    var interestInputText by remember { mutableStateOf("") }
    var lookingForInputText by remember { mutableStateOf("") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addPhotoUri(it) }
    }
    
    // Track if there's an error
    LaunchedEffect(uiState) {
        if (uiState is EditProfileUiState.Error) {
            errorMessage = (uiState as EditProfileUiState.Error).message
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onCancelClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.saveProfile(onSuccess = onSaveClick)
                        },
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.saveProfile(onSuccess = onSaveClick)
                },
                enabled = !isSaving
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Profile"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is EditProfileUiState.Loading -> {
                    if (!isSaving) {
                        // Only show the loading indicator if it's the initial load, not when saving
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                is EditProfileUiState.Error -> {
                    // Error will be shown in a dialog
                }
                
                is EditProfileUiState.Success -> {
                    // Main content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Photos section
                        Text(
                            text = "Profile Photos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Current and New Photos
                        val allPhotos = currentPhotos + selectedPhotoUris.map { it.toString() }
                        
                        if (allPhotos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Photo,
                                        contentDescription = "Add Photos",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Add Photos",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(currentPhotos) { photoUrl ->
                                    PhotoItem(
                                        photoUrl = photoUrl,
                                        onDeleteClick = { viewModel.removePhoto(photoUrl) }
                                    )
                                }
                                
                                items(selectedPhotoUris) { uri ->
                                    PhotoItem(
                                        photoUri = uri,
                                        onDeleteClick = { viewModel.removePhotoUri(uri) }
                                    )
                                }
                                
                                // Add photo button
                                item {
                                    AddPhotoButton(onClick = { photoPickerLauncher.launch("image/*") })
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Basic Information Section
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Name Field
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { viewModel.onNameChanged(it) },
                            label = { Text("Name*") },
                            placeholder = { Text("Your full name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        
                        // Age Field
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { viewModel.onAgeChanged(it) },
                            label = { Text("Age*") },
                            placeholder = { Text("Your age") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                        
                        // Gender Field
                        OutlinedTextField(
                            value = genderInput,
                            onValueChange = { viewModel.onGenderChanged(it) },
                            label = { Text("Gender") },
                            placeholder = { Text("Your gender") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        
                        // Location Field
                        OutlinedTextField(
                            value = locationInput,
                            onValueChange = { viewModel.onLocationChanged(it) },
                            label = { Text("Location") },
                            placeholder = { Text("City, Country") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        
                        // Bio Field
                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { viewModel.onBioChanged(it) },
                            label = { Text("About Me") },
                            placeholder = { Text("Tell others about yourself...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                }
                            ),
                            maxLines = 5
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Interests Section
                        Text(
                            text = "Interests",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Add Interest Field
                        val interestFocusRequester = remember { FocusRequester() }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = interestInputText,
                                onValueChange = { interestInputText = it },
                                label = { Text("Add Interest") },
                                placeholder = { Text("E.g., Photography, Hiking, etc.") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                                    .focusRequester(interestFocusRequester),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (interestInputText.isNotBlank()) {
                                            viewModel.addInterest(interestInputText)
                                            interestInputText = ""
                                            interestFocusRequester.requestFocus()
                                        } else {
                                            focusManager.clearFocus()
                                        }
                                    }
                                ),
                                singleLine = true
                            )
                            
                            IconButton(
                                onClick = {
                                    if (interestInputText.isNotBlank()) {
                                        viewModel.addInterest(interestInputText)
                                        interestInputText = ""
                                        interestFocusRequester.requestFocus()
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Interest"
                                )
                            }
                        }
                        
                        // Display Interests
                        if (interestsInput.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                interestsInput.forEach { interest ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                start = 12.dp,
                                                end = 8.dp,
                                                top = 6.dp,
                                                bottom = 6.dp
                                            )
                                        ) {
                                            Text(
                                                text = interest,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove $interest",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.removeInterest(interest) },
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Looking For Section
                        Text(
                            text = "Looking For",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Add Looking For Field
                        val lookingForFocusRequester = remember { FocusRequester() }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = lookingForInputText,
                                onValueChange = { lookingForInputText = it },
                                label = { Text("Add Looking For") },
                                placeholder = { Text("E.g., Friendship, Dating, etc.") },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                                    .focusRequester(lookingForFocusRequester),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (lookingForInputText.isNotBlank()) {
                                            viewModel.addLookingFor(lookingForInputText)
                                            lookingForInputText = ""
                                            lookingForFocusRequester.requestFocus()
                                        } else {
                                            focusManager.clearFocus()
                                        }
                                    }
                                ),
                                singleLine = true
                            )
                            
                            IconButton(
                                onClick = {
                                    if (lookingForInputText.isNotBlank()) {
                                        viewModel.addLookingFor(lookingForInputText)
                                        lookingForInputText = ""
                                        lookingForFocusRequester.requestFocus()
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Looking For"
                                )
                            }
                        }
                        
                        // Display Looking For
                        if (lookingForInput.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                lookingForInput.forEach { lookingFor ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                start = 12.dp,
                                                end = 8.dp,
                                                top = 6.dp,
                                                bottom = 6.dp
                                            )
                                        ) {
                                            Text(
                                                text = lookingFor,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove $lookingFor",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.removeLookingFor(lookingFor) },
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Preferences Section
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Age Preference Range
                        Text(
                            text = "Age Range: $minAgePreference - $maxAgePreference",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        // Min Age Slider
                        Text(
                            text = "Minimum Age: $minAgePreference",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        
                        Slider(
                            value = minAgePreference.toFloat(),
                            onValueChange = { viewModel.onMinAgePreferenceChanged(it.toInt()) },
                            valueRange = 18f..64f,
                            steps = 46,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Max Age Slider
                        Text(
                            text = "Maximum Age: $maxAgePreference",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        
                        Slider(
                            value = maxAgePreference.toFloat(),
                            onValueChange = { viewModel.onMaxAgePreferenceChanged(it.toInt()) },
                            valueRange = 19f..100f,
                            steps = 81,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Distance Preference
                        Text(
                            text = "Maximum Distance: $maxDistance km",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        
                        Slider(
                            value = maxDistance.toFloat(),
                            onValueChange = { viewModel.onMaxDistanceChanged(it.toInt()) },
                            valueRange = 1f..500f,
                            steps = 19,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Bottom spacer for FAB
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
            
            // Show progress indicator while saving
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Error Dialog
            errorMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("Error") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Photo item component
 */
@Composable
fun PhotoItem(
    photoUrl: String? = null,
    photoUri: Uri? = null,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // Photo
        if (photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (photoUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Delete button
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = CircleShape
                )
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Photo",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Add photo button component
 */
@Composable
fun AddPhotoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Photo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Add Photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}