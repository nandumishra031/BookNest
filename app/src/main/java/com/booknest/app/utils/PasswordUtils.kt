package com.booknest.app.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PasswordUtils {

    /**
     * Hash a password with salt for secure storage
     */
    fun hashPassword(password: String, salt: String? = null): String {
        val saltToUse = salt ?: generateSalt()
        val md = MessageDigest.getInstance("SHA-256")
        val saltedPassword = password + saltToUse
        val hash = md.digest(saltedPassword.toByteArray())
        val hashedPassword = Base64.getEncoder().encodeToString(hash)
        return "$saltToUse:$hashedPassword"
    }

    /**
     * Verify a password against a stored hash
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        val salt = parts[0]
        val hash = parts[1]

        val testHash = hashPassword(password, salt)
        return testHash == storedHash
    }

    /**
     * Generate a random salt for password hashing
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Validate password strength
     */
    fun validatePasswordStrength(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()

        if (password.length < 6) {
            errors.add("Password must be at least 6 characters long")
        }

        if (!password.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }

        if (!password.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }

        if (!password.any { it.isDigit() }) {
            errors.add("Password must contain at least one number")
        }

        return PasswordValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            strength = calculatePasswordStrength(password)
        )
    }

    private fun calculatePasswordStrength(password: String): PasswordStrength {
        var score = 0

        // Length scoring
        when {
            password.length >= 12 -> score += 2
            password.length >= 8 -> score += 1
        }

        // Character variety scoring
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score >= 6 -> PasswordStrength.STRONG
            score >= 4 -> PasswordStrength.MEDIUM
            score >= 2 -> PasswordStrength.WEAK
            else -> PasswordStrength.VERY_WEAK
        }
    }
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val strength: PasswordStrength
)

enum class PasswordStrength {
    VERY_WEAK,
    WEAK,
    MEDIUM,
    STRONG
}
