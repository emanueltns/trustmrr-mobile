package com.freeapp.founderrooms.di

import com.freeapp.founderrooms.auth.AuthViewModel
import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.chat.ChatViewModel
import com.freeapp.founderrooms.groups.GroupsViewModel
import com.freeapp.founderrooms.items.ItemsViewModel
import com.freeapp.founderrooms.locked.LockedViewModel
import com.freeapp.founderrooms.verify.VerifyViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * UI-layer Koin module: ViewModels live here, repositories live in
 * `sharedModule` (:shared), which knows nothing about the UI.
 *
 * EXTENSION POINT: register every new screen ViewModel here.
 *
 * `AuthViewModel(AuthRepository)` and `GroupsViewModel(AuthRepository,
 * TiersRepository)` resolve `get()` against types registered in
 * `sharedModule`; this module compiles against those types by package name
 * (`com.freeapp.founderrooms.auth` / `.tiers`) but the `get()` calls only
 * resolve at runtime once that module actually registers
 * `single<AuthRepository> { ... }` and `single<TiersRepository> { ... }`,
 * which is not this file's responsibility.
 *
 * `ChatViewModel`, `VerifyViewModel` and `LockedViewModel` additionally need
 * nav-time values (`tierId`, `me`) their constructors take directly rather
 * than resolving from Koin -- Koin has no idea what tier was tapped or who is
 * signed in. Koin's parametrized `viewModel { params -> ... }` DSL supplies
 * those from a `ParametersHolder`, filled positionally by the caller's
 * `parametersOf(...)` (see `App.kt`): `params.get<T>()` reads values back in
 * the exact order they were passed in, so the argument order below must
 * match each `parametersOf(...)` call site, not just each constructor.
 */
val appModule: Module = module {
    viewModel { ItemsViewModel(get()) }
    viewModel { AuthViewModel(get()) }
    viewModel { GroupsViewModel(get(), get()) }
    viewModel { params -> ChatViewModel(get(), get(), params.get<String>(), params.get<Me>()) }
    viewModel { params -> VerifyViewModel(get(), get(), params.get<Me>()) }
    viewModel { params -> LockedViewModel(get(), params.get<String>(), params.get<Me>()) }
}
