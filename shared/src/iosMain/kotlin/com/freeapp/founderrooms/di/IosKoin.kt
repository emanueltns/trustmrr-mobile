package com.freeapp.founderrooms.di

import com.freeapp.founderrooms.db.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * Call from the iOS app entry point (e.g., in @main App's init):
 *   IosKoinKt.doInitKoinIos()
 *
 * EXTENSION POINT: add iOS-only Koin bindings here (e.g., Keychain wrappers).
 */
fun initKoinIos() = initKoin {
    modules(
        module { single { DatabaseDriverFactory() } }
    )
}
