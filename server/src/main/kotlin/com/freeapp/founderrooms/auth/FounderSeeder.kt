package com.freeapp.founderrooms.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64

/**
 * Seeds the 4 demo founder accounts, one per private tier 1 through 4, so v1
 * launches with real members in the private rooms instead of empty tiers
 * (design doc section 7). This runs as Kotlin at startup rather than as SQL
 * in the V2 Flyway migration for two reasons: it needs jbcrypt, which is a
 * JVM library a SQL file cannot call (Postgres has no bcrypt function without
 * the pgcrypto extension), and it needs to log a plaintext password once,
 * which a migration file cannot do either. V2's migration only creates the
 * schema and seeds the 6 tier rows, none of which carry a secret.
 *
 * Idempotent: each founder is inserted only if its email is still absent, and
 * a freshly generated password is logged exactly once, at the moment that
 * row is inserted. On every later restart the row already exists, so nothing
 * is generated and nothing is logged again.
 */
class FounderSeeder {

    suspend fun seedIfNeeded() {
        for (seed in SEED_FOUNDERS) {
            insertIfMissing(seed)
        }
    }

    private suspend fun insertIfMissing(seed: SeedFounder) = newSuspendedTransaction(Dispatchers.IO) {
        val exists = UsersTable.selectAll().where { UsersTable.email eq seed.email }.limit(1).firstOrNull() != null
        if (exists) return@newSuspendedTransaction

        val plaintext = randomPassword()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        UsersTable.insert {
            it[email] = seed.email
            it[passwordHash] = PasswordHashing.hash(plaintext)
            it[displayName] = seed.displayName
            it[verifiedMrr] = seed.verifiedMrr
            it[refreshVersion] = 0
            it[createdAt] = now
        }
        log.warn(
            "Seeded demo founder {} (tier {}) with password: {} -- shown once here, never written to disk in plaintext.",
            seed.email,
            seed.tierId,
            plaintext,
        )
    }

    private fun randomPassword(): String {
        val bytes = ByteArray(18)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private data class SeedFounder(
        val email: String,
        val displayName: String,
        val tierId: Int,
        val verifiedMrr: Double,
    )

    private companion object {
        val log: Logger = LoggerFactory.getLogger(FounderSeeder::class.java)

        val SEED_FOUNDERS = listOf(
            SeedFounder("founder1@seed.internal", "Founder One", tierId = 1, verifiedMrr = 500.0),
            SeedFounder("founder2@seed.internal", "Founder Two", tierId = 2, verifiedMrr = 5000.0),
            SeedFounder("founder3@seed.internal", "Founder Three", tierId = 3, verifiedMrr = 50000.0),
            SeedFounder("founder4@seed.internal", "Founder Four", tierId = 4, verifiedMrr = 500000.0),
        )
    }
}
