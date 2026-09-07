package com.eldroid.smartnest.util

import android.util.Patterns

object AuthValidator {

    private const val MIN_PASSWORD_LENGTH = 8
    private const val MIN_NAME_LENGTH = 2
    private val ALLOWED_NAME_EXTRA_CHARS = setOf(' ', '\'', '.', '-')

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun validateName(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult.Invalid("Name is required")
        }
        if (name.length < MIN_NAME_LENGTH) {
            return ValidationResult.Invalid("Name must be at least $MIN_NAME_LENGTH characters")
        }
        val hasOnlyAllowedChars = name.all { it.isLetter() || it in ALLOWED_NAME_EXTRA_CHARS }
        if (!hasOnlyAllowedChars) {
            return ValidationResult.Invalid("Name can only contain letters, spaces, and - ' .")
        }
        return ValidationResult.Valid
    }

    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult.Invalid("Email is required")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult.Invalid("Enter a valid email address")
        }
        return ValidationResult.Valid
    }

    fun validatePasswordComplexity(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult.Invalid("Password is required")
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return ValidationResult.Invalid("Password must be at least $MIN_PASSWORD_LENGTH characters")
        }
        if (password.contains(" ")) {
            return ValidationResult.Invalid("Password cannot contain spaces")
        }
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            return ValidationResult.Invalid("Password must include at least one letter and one number")
        }
        return ValidationResult.Valid
    }

    fun validatePasswordMatch(password: String, confirmPassword: String): ValidationResult {
        return if (password != confirmPassword) {
            ValidationResult.Invalid("Passwords do not match")
        } else {
            ValidationResult.Valid
        }
    }

    fun validateLoginPassword(password: String): ValidationResult {
        return if (password.isBlank()) {
            ValidationResult.Invalid("Password is required")
        } else {
            ValidationResult.Valid
        }
    }
}