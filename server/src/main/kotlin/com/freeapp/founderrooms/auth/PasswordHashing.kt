package com.freeapp.founderrooms.auth

import org.mindrot.jbcrypt.BCrypt

/**
 * One hashing scheme shared by real signups (AuthService) and the demo
 * founder accounts (FounderSeeder), so there is exactly one place that knows
 * how a password becomes a stored hash.
 */
internal object PasswordHashing {
    fun hash(plaintext: String): String = BCrypt.hashpw(plaintext, BCrypt.gensalt())

    fun matches(plaintext: String, hash: String): Boolean = BCrypt.checkpw(plaintext, hash)
}
