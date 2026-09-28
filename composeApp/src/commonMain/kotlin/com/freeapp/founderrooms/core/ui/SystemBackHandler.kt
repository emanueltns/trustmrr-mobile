package com.freeapp.founderrooms.core.ui

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform system-back gesture/button. `androidx.activity.compose.BackHandler`
 * is Android-only (its dependency lives in androidMain, not commonMain), so this
 * stays expect/actual rather than importing it directly here.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
