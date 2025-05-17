package com.kilagee.onelove.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kilagee.onelove.R
import com.kilagee.onelove.ui.components.OneLoveTextField

/**
 * Register screen component
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: AuthViewModel
) {
    val registerState by viewModel.registerState.collectAsState()
    
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    
    var nameError by rememberSaveable { mutableStateOf("") }
    var emailError by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf("") }
    var confirmPasswordError by rememberSaveable { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterState.Success -> {
                onRegisterSuccess()
                viewModel.resetRegisterState()
            }
            is RegisterState.Error -> {
                val errorMessage = (registerState as RegisterState.Error).message
                snackbarHostState.showSnackbar(errorMessage)
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and app name
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp)
            )
            
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Join OneLove and find your perfect match",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            // Name field
            OneLoveTextField(
                value = name,
                onValueChange = { 
                    name = it
                    if (nameError.isNotEmpty()) {
                        nameError = ""
                    }
                },
                label = "Full Name",
                placeholder = "Enter your name",
                isError = nameError.isNotEmpty(),
                errorMessage = nameError,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Name Icon"
                    )
                },
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                onAction = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            
            // Email field
            OneLoveTextField(
                value = email,
                onValueChange = { 
                    email = it
                    if (emailError.isNotEmpty()) {
                        emailError = ""
                    }
                },
                label = "Email",
                placeholder = "Enter your email",
                isError = emailError.isNotEmpty(),
                errorMessage = emailError,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Icon"
                    )
                },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onAction = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            
            // Password field
            OneLoveTextField(
                value = password,
                onValueChange = { 
                    password = it
                    if (passwordError.isNotEmpty()) {
                        passwordError = ""
                    }
                    
                    // Check if confirm password matches when typing password
                    if (confirmPassword.isNotEmpty() && confirmPassword != it) {
                        confirmPasswordError = "Passwords do not match"
                    } else {
                        confirmPasswordError = ""
                    }
                },
                label = "Password",
                placeholder = "Create a password",
                isError = passwordError.isNotEmpty(),
                errorMessage = passwordError,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                onAction = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            
            // Confirm Password field
            OneLoveTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    confirmPasswordError = if (it != password) {
                        "Passwords do not match"
                    } else {
                        ""
                    }
                },
                label = "Confirm Password",
                placeholder = "Confirm your password",
                isError = confirmPasswordError.isNotEmpty(),
                errorMessage = confirmPasswordError,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onAction = KeyboardActions(
                    onDone = { 
                        focusManager.clearFocus()
                        performRegistration(
                            name = name,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            setNameError = { nameError = it },
                            setEmailError = { emailError = it },
                            setPasswordError = { passwordError = it },
                            setConfirmPasswordError = { confirmPasswordError = it },
                            register = { viewModel.register(name, email, password) }
                        )
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Register button
            Button(
                onClick = {
                    performRegistration(
                        name = name,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        setNameError = { nameError = it },
                        setEmailError = { emailError = it },
                        setPasswordError = { passwordError = it },
                        setConfirmPasswordError = { confirmPasswordError = it },
                        register = { viewModel.register(name, email, password) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = registerState !is RegisterState.Loading
            ) {
                if (registerState is RegisterState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Login link
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onLoginClick) {
                        Text(
                            text = "Log In",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
            
            // Terms and conditions text
            Text(
                text = "By creating an account, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }
    }
}

/**
 * Helper function to validate and perform registration
 */
private fun performRegistration(
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    setNameError: (String) -> Unit,
    setEmailError: (String) -> Unit,
    setPasswordError: (String) -> Unit,
    setConfirmPasswordError: (String) -> Unit,
    register: () -> Unit
) {
    var isValid = true
    
    if (name.isBlank()) {
        setNameError("Name cannot be empty")
        isValid = false
    }
    
    if (email.isBlank()) {
        setEmailError("Email cannot be empty")
        isValid = false
    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        setEmailError("Please enter a valid email address")
        isValid = false
    }
    
    if (password.isBlank()) {
        setPasswordError("Password cannot be empty")
        isValid = false
    } else if (password.length < 6) {
        setPasswordError("Password must be at least 6 characters")
        isValid = false
    }
    
    if (confirmPassword.isBlank()) {
        setConfirmPasswordError("Please confirm your password")
        isValid = false
    } else if (password != confirmPassword) {
        setConfirmPasswordError("Passwords do not match")
        isValid = false
    }
    
    if (isValid) {
        register()
    }
}