package com.kilagee.onelove.ui.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.BillingAddress
import com.kilagee.onelove.domain.repository.PaymentMethodDetails
import com.kilagee.onelove.ui.components.ErrorStateView
import com.kilagee.onelove.ui.components.LoadingStateView
import com.kilagee.onelove.ui.subscription.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentMethodScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    // Card details state
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    
    // Billing details state
    var billingName by remember { mutableStateOf("") }
    var billingEmail by remember { mutableStateOf("") }
    var billingPhone by remember { mutableStateOf("") }
    var billingLine1 by remember { mutableStateOf("") }
    var billingLine2 by remember { mutableStateOf("") }
    var billingCity by remember { mutableStateOf("") }
    var billingState by remember { mutableStateOf("") }
    var billingPostalCode by remember { mutableStateOf("") }
    var billingCountry by remember { mutableStateOf("") }
    
    // Save for future use
    var saveForFutureUse by remember { mutableStateOf(true) }
    
    // Form errors
    var cardNumberError by remember { mutableStateOf<String?>(null) }
    var expiryError by remember { mutableStateOf<String?>(null) }
    var cvcError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    // UI state
    var showBillingDetails by remember { mutableStateOf(false) }
    
    // Observe payment method state
    val newPaymentMethodState by viewModel.newPaymentMethodState.collectAsState()
    
    // Handle navigation on success
    LaunchedEffect(newPaymentMethodState) {
        if (newPaymentMethodState is Resource.Success) {
            // Go back to previous screen
            navController.popBackStack()
            
            // Clear state
            viewModel.clearNewPaymentMethodState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Payment Method") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Card details section
                Text(
                    text = "Card Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Card number
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { 
                        // Only allow digits and spaces, max 19 chars (16 digits + 3 spaces)
                        if (it.length <= 19 && it.all { char -> char.isDigit() || char.isWhitespace() }) {
                            cardNumber = it
                            cardNumberError = null
                        }
                    },
                    label = { Text("Card Number") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = cardNumberError != null,
                    supportingText = cardNumberError?.let { { Text(it) } }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Expiry date and CVC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Expiry month
                    OutlinedTextField(
                        value = expiryMonth,
                        onValueChange = { 
                            // Only allow 2 digits, max value 12
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                val month = it.toIntOrNull() ?: 0
                                if (month in 0..12) {
                                    expiryMonth = it
                                    expiryError = null
                                }
                            }
                        },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = expiryError != null
                    )
                    
                    Text(
                        text = "/",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Expiry year
                    OutlinedTextField(
                        value = expiryYear,
                        onValueChange = { 
                            // Only allow 2 digits
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                expiryYear = it
                                expiryError = null
                            }
                        },
                        label = { Text("YY") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = expiryError != null,
                        supportingText = expiryError?.let { { Text(it) } }
                    )
                    
                    // CVC
                    OutlinedTextField(
                        value = cvc,
                        onValueChange = { 
                            // Only allow 3-4 digits
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                cvc = it
                                cvcError = null
                            }
                        },
                        label = { Text("CVC") },
                        modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        isError = cvcError != null,
                        supportingText = cvcError?.let { { Text(it) } }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Card holder name
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { 
                        holderName = it
                        nameError = null
                    },
                    label = { Text("Cardholder Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Billing details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Billing Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Switch(
                        checked = showBillingDetails,
                        onCheckedChange = { showBillingDetails = it }
                    )
                }
                
                if (showBillingDetails) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Billing name
                    OutlinedTextField(
                        value = billingName,
                        onValueChange = { billingName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Billing email
                    OutlinedTextField(
                        value = billingEmail,
                        onValueChange = { billingEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Billing phone
                    OutlinedTextField(
                        value = billingPhone,
                        onValueChange = { 
                            // Only allow digits, +, parentheses, and spaces
                            if (it.all { char -> char.isDigit() || char in setOf('+', '(', ')', '-', ' ') }) {
                                billingPhone = it
                            }
                        },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Address line 1
                    OutlinedTextField(
                        value = billingLine1,
                        onValueChange = { billingLine1 = it },
                        label = { Text("Address Line 1") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Address line 2
                    OutlinedTextField(
                        value = billingLine2,
                        onValueChange = { billingLine2 = it },
                        label = { Text("Address Line 2 (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // City and State
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // City
                        OutlinedTextField(
                            value = billingCity,
                            onValueChange = { billingCity = it },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        // State
                        OutlinedTextField(
                            value = billingState,
                            onValueChange = { billingState = it },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Postal code and country
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Postal code
                        OutlinedTextField(
                            value = billingPostalCode,
                            onValueChange = { billingPostalCode = it },
                            label = { Text("Postal Code") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                        
                        // Country
                        OutlinedTextField(
                            value = billingCountry,
                            onValueChange = { billingCountry = it },
                            label = { Text("Country") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Save for future use
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = saveForFutureUse,
                        onCheckedChange = { saveForFutureUse = it }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Save this card for future payments",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Add button
                Button(
                    onClick = {
                        // Validate form
                        var isValid = true
                        
                        if (cardNumber.replace(" ", "").length != 16) {
                            cardNumberError = "Please enter a valid card number"
                            isValid = false
                        }
                        
                        if (expiryMonth.isEmpty() || expiryYear.isEmpty()) {
                            expiryError = "Please enter a valid expiry date"
                            isValid = false
                        }
                        
                        if (cvc.length < 3) {
                            cvcError = "Please enter a valid CVC"
                            isValid = false
                        }
                        
                        if (holderName.isEmpty()) {
                            nameError = "Please enter the cardholder name"
                            isValid = false
                        }
                        
                        if (isValid) {
                            // Create billing address if details are provided
                            val billingAddress = if (showBillingDetails && billingLine1.isNotEmpty() && 
                                                     billingCity.isNotEmpty() && billingPostalCode.isNotEmpty() && 
                                                     billingCountry.isNotEmpty()) {
                                BillingAddress(
                                    line1 = billingLine1,
                                    line2 = if (billingLine2.isNotEmpty()) billingLine2 else null,
                                    city = billingCity,
                                    state = if (billingState.isNotEmpty()) billingState else null,
                                    postalCode = billingPostalCode,
                                    country = billingCountry
                                )
                            } else null
                            
                            // Create payment method details
                            val paymentMethodDetails = PaymentMethodDetails(
                                type = "card",
                                cardNumber = cardNumber.replace(" ", ""),
                                expiryMonth = expiryMonth.toIntOrNull(),
                                expiryYear = expiryYear.toIntOrNull()?.let { 2000 + it },
                                cvc = cvc,
                                billingName = if (showBillingDetails && billingName.isNotEmpty()) billingName else holderName,
                                billingEmail = if (showBillingDetails && billingEmail.isNotEmpty()) billingEmail else null,
                                billingPhone = if (showBillingDetails && billingPhone.isNotEmpty()) billingPhone else null,
                                billingAddress = billingAddress,
                                saveForFutureUse = saveForFutureUse
                            )
                            
                            // Save payment method
                            viewModel.savePaymentMethod(paymentMethodDetails)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = newPaymentMethodState !is Resource.Loading
                ) {
                    if (newPaymentMethodState is Resource.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Add Payment Method")
                    }
                }
                
                if (newPaymentMethodState is Resource.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = (newPaymentMethodState as Resource.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Security note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Your payment information is secure",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}