package com.freeapp.founderrooms.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

sealed interface SignupResult {
    data class Success(val tokens: AuthTokens, val me: Me) : SignupResult
    data object EmailTaken : SignupResult
}

sealed interface LoginResult {
    data class Success(val tokens: AuthTokens, val me: Me) : LoginResult
    data object InvalidCredentials : LoginResult
}

sealed interface RefreshResult {
    data class Success(val tokens: AuthTokens) : RefreshResult
    data object InvalidToken : RefreshResult
}

/**
 * Signup, login and refresh (decisions 1 and 4), plus the small `me` lookup
 * that TiersRoutes and ChatRoutes need for the tier-unlock check. Business
 * logic lives here rather than in a separate repository class, the same way
 * `ItemsRepository` mixes queries with the validation it needs; UsersTable
 * stays the only place that owns column names.
 */
class AuthService(private val jwt: JwtService) {

    suspend fun signup(request: SignupRequest): SignupResult = dbQuery {
        val email = request.email.trim().lowercase()
        val existing = UsersTable.selectAll().where { UsersTable.email eq email }.limit(1).firstOrNull()
        if (existing != null) return@dbQuery SignupResult.EmailTaken

        val displayName = request.displayName.trim()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val id = UsersTable.insert {
            it[UsersTable.email] = email
            it[passwordHash] = PasswordHashing.hash(request.password)
            it[UsersTable.displayName] = displayName
            it[verifiedMrr] = null
            it[refreshVersion] = 0
            it[createdAt] = now
        } get UsersTable.id

        val me = Me(id = id, email = email, displayName = displayName, verifiedMrr = null)
        SignupResult.Success(tokens = issueTokens(id, version = 0), me = me)
    }

    suspend fun login(request: LoginRequest): LoginResult = dbQuery {
        val email = request.email.trim().lowercase()
        val row = UsersTable.selectAll().where { UsersTable.email eq email }.limit(1).firstOrNull()
            ?: return@dbQuery LoginResult.InvalidCredentials

        if (!PasswordHashing.matches(request.password, row[UsersTable.passwordHash])) {
            return@dbQuery LoginResult.InvalidCredentials
        }

        LoginResult.Success(tokens = issueTokens(row[UsersTable.id], row[UsersTable.refreshVersion]), me = toMe(row))
    }

    /**
     * Rotation (decision 4): the update only succeeds if `refresh_version`
     * still matches the version carried in the presented token, and the same
     * statement bumps it. This is one atomic UPDATE rather than a SELECT
     * followed by an unconditional UPDATE, so two concurrent refreshes with
     * the same stale token cannot both read "version still matches" and both
     * win. A replayed (already-rotated) token's `ver` matches no row, the
     * update affects zero rows, and the caller gets [RefreshResult.InvalidToken]
     * instead of a fresh pair.
     */
    suspend fun refresh(token: String): RefreshResult = dbQuery {
        val decoded = jwt.verifyRefreshToken(token) ?: return@dbQuery RefreshResult.InvalidToken
        val userId = jwt.userIdOf(decoded)
        val presentedVersion = jwt.refreshVersionOf(decoded)

        val updatedRows = UsersTable.update({
            (UsersTable.id eq userId) and (UsersTable.refreshVersion eq presentedVersion)
        }) {
            it[refreshVersion] = presentedVersion + 1
        }
        if (updatedRows != 1) return@dbQuery RefreshResult.InvalidToken

        RefreshResult.Success(issueTokens(userId, presentedVersion + 1))
    }

    suspend fun me(userId: Long): Me? = dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq userId }.limit(1).firstOrNull()?.let(::toMe)
    }

    private fun issueTokens(userId: Long, version: Int): AuthTokens = AuthTokens(
        accessToken = jwt.issueAccessToken(userId),
        refreshToken = jwt.issueRefreshToken(userId, version),
    )

    private fun toMe(row: ResultRow) = Me(
        id = row[UsersTable.id],
        email = row[UsersTable.email],
        displayName = row[UsersTable.displayName],
        verifiedMrr = row[UsersTable.verifiedMrr],
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
