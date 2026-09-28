package com.freeapp.founderrooms.core.ui

import com.freeapp.founderrooms.core.error.ErrorState

/**
 * The plain-English copy each [ErrorState] renders as, kept in one place so
 * every screen states the same failure the same way instead of each screen
 * inventing its own wording.
 */
fun ErrorState.toDisplayMessage(): String = when (this) {
    ErrorState.NoInternet -> "No connection. Check your network and try again."
    ErrorState.RequestTimeout -> "The server took too long to respond."
    ErrorState.Unauthorized -> "Your session expired. Please log in again."
    ErrorState.Forbidden -> "You don't have access to this room."
    ErrorState.NotFound -> "That wasn't found."
    is ErrorState.Validation -> message
    is ErrorState.ServerError -> "Something went wrong on our end ($status)."
    is ErrorState.InvalidResponse -> "Unexpected response from the server."
    is ErrorState.Unknown -> message
}
