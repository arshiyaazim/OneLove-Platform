package com.kilagee.onelove.ui.authentication

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kilagee.onelove.R
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.navigation.Screen
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val username = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val ageConfirmed = remember { mutableStateOf(false) }
    val countries = listOf("United States", "Canada", "United Kingdom", "Australia", "Germany", "France", "Japan", "Other")
    val selectedCountry = remember { mutableStateOf(countries[0]) }
    val countryDropdownExpanded = remember { mutableStateOf(false) }
    val location = remember { mutableStateOf("") }
    val genders = listOf("Male", "Female", "Non-binary", "Prefer not to say")
    val selectedGender = remember { mutableStateOf(genders[0]) }
    val genderDropdownExpanded = remember { mutableStateOf(false) }
    
    val profilePictureUri = remember { mutableStateOf<Uri?>(null) }
    val idDocumentUri = remember { mutableStateOf<Uri?>(null) }
    
    val registerState by viewModel.registerState.collectAsState()
    val context = LocalContext.current
    
    // Effect to navigate after successful registration
    LaunchedEffect(registerState) {
        if (registerState is Resource.Success) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
            viewModel.clearRegisterState()
        }
    }
    
    // Image Picker Launcher for Profile Picture
    val profilePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profilePictureUri.value = uri
    }
    
    // Image Picker Launcher for ID Document
    val idDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        idDocumentUri.value = uri
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.register)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create New Account",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { profilePictureLauncher.launch("image/*") }
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profilePictureUri.value != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profilePictureUri.value)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Camera,
                        contentDescription = "Upload Photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Personal Info Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // First Name
                OutlinedTextField(
                    value = firstName.value,
                    onValueChange = { firstName.value = it },
                    label = { Text(stringResource(R.string.first_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Last Name
                OutlinedTextField(
                    value = lastName.value,
                    onValueChange = { lastName.value = it },
                    label = { Text(stringResource(R.string.last_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Username
                OutlinedTextField(
                    value = username.value,
                    onValueChange = { username.value = it },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Account Info Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Email
                OutlinedTextField(
                    value = email.value,
                    onValueChange = { email.value = it },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Password
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { password.value = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword.value,
                    onValueChange = { confirmPassword.value = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Additional Info Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Additional Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Country Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCountry.value,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.country)) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { countryDropdownExpanded.value = true }) {
                                Icon(Icons.Filled.ArrowDropDown, "Country dropdown")
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = countryDropdownExpanded.value,
                        onDismissRequest = { countryDropdownExpanded.value = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country) },
                                onClick = {
                                    selectedCountry.value = country
                                    countryDropdownExpanded.value = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Location/City
                OutlinedTextField(
                    value = location.value,
                    onValueChange = { location.value = it },
                    label = { Text("City/Location") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Gender Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedGender.value,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.gender)) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { genderDropdownExpanded.value = true }) {
                                Icon(Icons.Filled.ArrowDropDown, "Gender dropdown")
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = genderDropdownExpanded.value,
                        onDismissRequest = { genderDropdownExpanded.value = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        genders.forEach { gender ->
                            DropdownMenuItem(
                                text = { Text(gender) },
                                onClick = {
                                    selectedGender.value = gender
                                    genderDropdownExpanded.value = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ID Document Upload
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Verification",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { idDocumentLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.Filled.Camera, contentDescription = "Upload ID")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (idDocumentUri.value != null)
                                "ID Document Uploaded"
                            else
                                "Upload ID/Passport for Verification"
                        )
                        
                        if (idDocumentUri.value != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Uploaded",
                                tint = Color.Green
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Age Confirmation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = ageConfirmed.value,
                        onCheckedChange = { ageConfirmed.value = it }
                    )
                    
                    Text(
                        text = "I confirm that I am 18 years of age or older",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error Message
            if (registerState is Resource.Error) {
                Text(
                    text = (registerState as Resource.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Register Button
            Button(
                onClick = {
                    // Basic validation
                    if (firstName.value.isBlank() || lastName.value.isBlank() || username.value.isBlank() || 
                        email.value.isBlank() || password.value.isBlank() || location.value.isBlank()) {
                        // Show error using resource since we're using the same pattern
                        viewModel.registerState.value = Resource.error("Please fill all required fields")
                        return@Button
                    }
                    
                    if (password.value != confirmPassword.value) {
                        viewModel.registerState.value = Resource.error("Passwords do not match")
                        return@Button
                    }
                    
                    if (!ageConfirmed.value) {
                        viewModel.registerState.value = Resource.error("You must confirm that you are 18 years or older")
                        return@Button
                    }
                    
                    if (profilePictureUri.value == null) {
                        viewModel.registerState.value = Resource.error("Please upload a profile picture")
                        return@Button
                    }
                    
                    // Register the user
                    viewModel.register(
                        firstName = firstName.value,
                        lastName = lastName.value,
                        username = username.value,
                        email = email.value,
                        password = password.value,
                        country = selectedCountry.value,
                        location = location.value,
                        gender = selectedGender.value,
                        profileImageUri = profilePictureUri.value,
                        idDocumentUri = idDocumentUri.value
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = registerState !is Resource.Loading
            ) {
                if (registerState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = stringResource(R.string.create_account))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Login Link
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.already_have_account))
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                    Text(text = stringResource(R.string.login))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}