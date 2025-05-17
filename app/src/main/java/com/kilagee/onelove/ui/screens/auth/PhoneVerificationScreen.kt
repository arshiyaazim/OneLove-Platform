package com.kilagee.onelove.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kilagee.onelove.R
import com.kilagee.onelove.ui.LocalSnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Phone verification screen for the app
 */
@Composable
fun PhoneVerificationScreen(
    viewModel: PhoneVerificationViewModel = hiltViewModel(),
    onVerificationSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val verificationCode by viewModel.verificationCode.collectAsState()
    val isCodeSent by viewModel.isCodeSent.collectAsState()
    val isPhoneNumberValid by viewModel.isPhoneNumberValid.collectAsState()
    val isVerificationCodeValid by viewModel.isVerificationCodeValid.collectAsState()
    
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    
    // Handle one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PhoneVerificationEvent.CodeSent -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Verification code sent to $phoneNumber")
                    }
                }
                is PhoneVerificationEvent.VerificationSuccess -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Phone verification successful")
                        delay(1000) // Give time for the snackbar to be seen
                        onVerificationSuccess()
                    }
                }
                is PhoneVerificationEvent.NavigateBack -> {
                    onNavigateBack()
                }
                is PhoneVerificationEvent.ValidationError -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }
    
    // Show errors in snackbar
    LaunchedEffect(uiState) {
        if (uiState is PhoneVerificationUiState.Error) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar((uiState as PhoneVerificationUiState.Error).message)
                viewModel.clearErrors()
            }
        }
    }
    
    PhoneVerificationScreenContent(
        phoneNumber = phoneNumber,
        verificationCode = verificationCode,
        isCodeSent = isCodeSent,
        isPhoneNumberValid = isPhoneNumberValid,
        isVerificationCodeValid = isVerificationCodeValid,
        isLoading = uiState is PhoneVerificationUiState.Loading,
        onPhoneNumberChange = viewModel::updatePhoneNumber,
        onVerificationCodeChange = viewModel::updateVerificationCode,
        onSendCodeClick = { viewModel.sendVerificationCode() },
        onVerifyClick = { viewModel.verifyCode() },
        onResendClick = { viewModel.sendVerificationCode() },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun PhoneVerificationScreenContent(
    phoneNumber: String,
    verificationCode: String,
    isCodeSent: Boolean,
    isPhoneNumberValid: Boolean,
    isVerificationCodeValid: Boolean,
    isLoading: Boolean,
    onPhoneNumberChange: (String) -> Unit,
    onVerificationCodeChange: (String) -> Unit,
    onSendCodeClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val codeFocusRequester = remember { FocusRequester() }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Phone Verification") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) }
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "OneLove Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Heading
                Text(
                    text = if (isCodeSent) "Enter Verification Code" else "Verify Your Phone Number",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subheading
                Text(
                    text = if (isCodeSent) {
                        "We've sent a verification code to $phoneNumber. Please enter it below."
                    } else {
                        "We'll send a verification code to your phone number to verify it's you."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isCodeSent) {
                            // Phone number field
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = onPhoneNumberChange,
                                label = { Text("Phone Number") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Phone"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isError = !isPhoneNumberValid,
                                supportingText = {
                                    if (!isPhoneNumberValid) {
                                        Text("Please enter a valid phone number")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        onSendCodeClick()
                                    }
                                ),
                                singleLine = true,
                                enabled = !isLoading
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Send Code button
                            Button(
                                onClick = onSendCodeClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Send Verification Code",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        } else {
                            // Verification code field
                            
                            // Show the entered phone number
                            Text(
                                text = "Verification code sent to:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Code input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedTextField(
                                    value = verificationCode,
                                    onValueChange = onVerificationCodeChange,
                                    modifier = Modifier
                                        .width(200.dp)
                                        .focusRequester(codeFocusRequester),
                                    isError = !isVerificationCodeValid,
                                    label = { Text("Verification Code") },
                                    supportingText = {
                                        if (!isVerificationCodeValid) {
                                            Text("Please enter a valid 6-digit code")
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            onVerifyClick()
                                        }
                                    ),
                                    singleLine = true,
                                    enabled = !isLoading,
                                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            
                            // Focus the code input when it appears
                            LaunchedEffect(Unit) {
                                delay(300) // Short delay to ensure the UI is ready
                                codeFocusRequester.requestFocus()
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Verify button
                            Button(
                                onClick = onVerifyClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                enabled = !isLoading && verificationCode.length == 6,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Verify Code",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Resend code
                            TextButton(
                                onClick = onResendClick,
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Didn't receive the code? Resend",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Change number
                            TextButton(
                                onClick = onNavigateBack,
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Change phone number",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Additional info
                Text(
                    text = "Your phone number helps us verify your identity and secure your account.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}