package com.freeapp.founderrooms.di

import com.freeapp.founderrooms.auth.AuthService
import com.freeapp.founderrooms.auth.FounderSeeder
import com.freeapp.founderrooms.auth.JwtService
import com.freeapp.founderrooms.chat.ChatService
import com.freeapp.founderrooms.chat.ChatSessionRegistry
import com.freeapp.founderrooms.items.ItemsRepository
import com.freeapp.founderrooms.tiers.TiersRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * EXTENSION POINT: register new server-side singletons here.
 * One Koin binding per repository / use-case.
 *
 * A function, not a top-level val: [JwtService] needs the secret read out of
 * `application.yaml`'s `jwt.secret` key (env-backed, see that file), and
 * Application.module() only has that value at runtime, after
 * environment.config is available. Passing it in here keeps the secret out
 * of a hardcoded default anywhere in code.
 */
fun serverModule(jwtSecret: String): Module = module {
    single { ItemsRepository() }
    single { JwtService(jwtSecret) }
    single { AuthService(get()) }
    single { TiersRepository() }
    single { FounderSeeder() }
    single { ChatSessionRegistry() }
    single { ChatService(get(), get()) }
}
