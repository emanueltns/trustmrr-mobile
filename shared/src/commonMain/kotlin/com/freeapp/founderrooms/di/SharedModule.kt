package com.freeapp.founderrooms.di

import com.freeapp.founderrooms.api.ApiClient
import com.freeapp.founderrooms.auth.AuthRepository
import com.freeapp.founderrooms.chat.ChatRepository
import com.freeapp.founderrooms.core.auth.SettingsTokenStore
import com.freeapp.founderrooms.core.auth.TokenStore
import com.freeapp.founderrooms.db.AppDatabase
import com.freeapp.founderrooms.db.DatabaseDriverFactory
import com.freeapp.founderrooms.items.ItemsRepository
import com.freeapp.founderrooms.tiers.TiersRepository
import com.freeapp.founderrooms.verify.VerifyRepository
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Koin module shared across Android + iOS.
 *
 * EXTENSION POINT: register new repositories / use-cases here.
 * Per-platform setup (DatabaseDriverFactory wiring) is in the platform modules:
 *   - androidModule (provided in :composeApp:androidMain via initKoin from MainActivity)
 *   - iosModule (provided when bridging to SwiftUI)
 */
val sharedModule: Module = module {
    single { AppDatabase(get<DatabaseDriverFactory>().createDriver()) }
    single<TokenStore> { SettingsTokenStore() }
    // Drop the `tokens` argument in an app with no login: the client then
    // sends no Authorization header and installs no 401 interceptor.
    // The type argument is explicit on purpose: the parameter is nullable, and
    // a bare get() there resolves against a nullable expected type, which Koin
    // reports only at first resolution, not at compile time.
    single { ApiClient(ApiClient.defaultClient(tokens = get<TokenStore>())) }
    single { ItemsRepository(get(), get()) }
    single { AuthRepository(get(), get()) }
    single { TiersRepository(get()) }
    single { ChatRepository(get(), get()) }
    single { VerifyRepository(get()) }
}

/**
 * Convenience: starts Koin with the shared module plus any extra platform modules.
 * Call this from MainActivity (Android) and the iOS app entry point.
 */
fun initKoin(extra: KoinAppDeclaration? = null) {
    startKoin {
        extra?.invoke(this)
        modules(sharedModule)
    }
}
