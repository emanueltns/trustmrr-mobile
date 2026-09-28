package com.freeapp.founderrooms.core.auth

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable

/**
 * The pair the server hands back at login and at refresh.
 *
 * Field names must match the auth endpoint's JSON byte for byte, exactly like
 * every other DTO in this template.
 */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/** Where the tokens live between app launches. */
interface TokenStore {
    fun read(): AuthTokens?
    fun save(tokens: AuthTokens)
    fun clear()
}

/**
 * Default implementation: SharedPreferences on Android, NSUserDefaults on iOS,
 * through multiplatform-settings.
 *
 * This is plain storage, not a secure enclave. It is the right default for a
 * short-lived access token; an app holding anything more sensitive should swap
 * in EncryptedSharedPreferences and the iOS Keychain behind this same
 * interface, which is why the interface exists.
 */
class SettingsTokenStore(
    private val settings: Settings = Settings(),
) : TokenStore {

    override fun read(): AuthTokens? {
        val access = settings.getStringOrNull(KEY_ACCESS) ?: return null
        val refresh = settings.getStringOrNull(KEY_REFRESH) ?: return null
        return AuthTokens(access, refresh)
    }

    override fun save(tokens: AuthTokens) {
        settings.putString(KEY_ACCESS, tokens.accessToken)
        settings.putString(KEY_REFRESH, tokens.refreshToken)
    }

    override fun clear() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "auth.access_token"
        const val KEY_REFRESH = "auth.refresh_token"
    }
}
