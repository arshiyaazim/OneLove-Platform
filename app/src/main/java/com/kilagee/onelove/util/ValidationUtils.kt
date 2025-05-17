package com.kilagee.onelove.util

import java.util.regex.Pattern

/**
 * Utility class for validating user inputs
 */
object ValidationUtils {
    
    // Regular expression for email validation
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )
    
    // Regular expression for password validation (at least 8 characters, containing letters, numbers, and special chars)
    private val PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
    )
    
    // Regular expression for phone number validation (international format)
    private val PHONE_PATTERN = Pattern.compile(
        "^\\+[0-9]{10,15}$"
    )
    
    /**
     * Validate if email format is correct
     * @param email Email to validate
     * @return Validation result (true if valid)
     */
    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() && EMAIL_PATTERN.matcher(email).matches()
    }
    
    /**
     * Validate if password is strong enough
     * @param password Password to validate
     * @return Validation result (true if valid)
     */
    fun isValidPassword(password: String?): Boolean {
        return !password.isNullOrBlank() && password.length >= 8
    }
    
    /**
     * Validate if strong password meets security requirements
     * @param password Password to validate
     * @return Validation result (true if valid)
     */
    fun isStrongPassword(password: String?): Boolean {
        return !password.isNullOrBlank() && PASSWORD_PATTERN.matcher(password).matches()
    }
    
    /**
     * Validate if passwords match
     * @param password Password
     * @param confirmPassword Confirmation password
     * @return Validation result (true if passwords match)
     */
    fun doPasswordsMatch(password: String?, confirmPassword: String?): Boolean {
        return !password.isNullOrBlank() && password == confirmPassword
    }
    
    /**
     * Validate if phone number is in a valid format
     * @param phoneNumber Phone number to validate
     * @return Validation result (true if valid)
     */
    fun isValidPhoneNumber(phoneNumber: String?): Boolean {
        return !phoneNumber.isNullOrBlank() && PHONE_PATTERN.matcher(phoneNumber).matches()
    }
    
    /**
     * Validate if name is valid
     * @param name Name to validate
     * @return Validation result (true if valid)
     */
    fun isValidName(name: String?): Boolean {
        return !name.isNullOrBlank() && name.length >= 2
    }
    
    /**
     * Validate if user input is not empty
     * @param input User input to validate
     * @return Validation result (true if valid)
     */
    fun isNotEmpty(input: String?): Boolean {
        return !input.isNullOrBlank()
    }
    
    /**
     * Validate if age is valid (over 18)
     * @param age Age to validate
     * @return Validation result (true if valid)
     */
    fun isValidAge(age: Int?): Boolean {
        return age != null && age >= 18
    }
    
    /**
     * Validate credit card number using Luhn algorithm
     * @param cardNumber Credit card number to validate
     * @return Validation result (true if valid)
     */
    fun isValidCreditCardNumber(cardNumber: String?): Boolean {
        if (cardNumber.isNullOrBlank()) return false
        
        // Remove spaces and dashes
        val digitsOnly = cardNumber.replace("[ -]".toRegex(), "")
        
        // Validate length
        if (digitsOnly.length < 13 || digitsOnly.length > 19) return false
        
        // Luhn algorithm
        var sum = 0
        var alternate = false
        for (i in digitsOnly.length - 1 downTo 0) {
            var digit = digitsOnly[i] - '0'
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit = digit - 9
                }
            }
            sum += digit
            alternate = !alternate
        }
        return sum % 10 == 0
    }
    
    /**
     * Validate if expiry date is valid (MM/YY format)
     * @param expiryDate Expiry date to validate
     * @return Validation result (true if valid)
     */
    fun isValidExpiryDate(expiryDate: String?): Boolean {
        if (expiryDate.isNullOrBlank()) return false
        
        // Format MM/YY
        val parts = expiryDate.split("/")
        if (parts.size != 2) return false
        
        try {
            val month = parts[0].toInt()
            val year = parts[1].toInt() + 2000 // Convert YY to 20YY
            
            if (month < 1 || month > 12) return false
            
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1 // 0-based month
            
            // Card is expired
            if (year < currentYear) return false
            
            // Card expires this year, check month
            if (year == currentYear && month < currentMonth) return false
            
            return true
        } catch (e: NumberFormatException) {
            return false
        }
    }
    
    /**
     * Validate if CVC is valid (3-4 digits)
     * @param cvc CVC to validate
     * @return Validation result (true if valid)
     */
    fun isValidCVC(cvc: String?): Boolean {
        if (cvc.isNullOrBlank()) return false
        
        // 3-4 digits
        return cvc.matches("^[0-9]{3,4}$".toRegex())
    }
}