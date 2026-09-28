package com.freeapp.founderrooms.items

import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.BaseViewModel

/**
 * EXTENSION POINT: the reference ViewModel for the template. Copy this file
 * when adding a screen and rename `Items` to your domain.
 *
 * Note the split the template relies on: the cache flow is the source of the
 * list, and the network call only reports whether the refresh worked. The list
 * therefore renders from disk on a cold start with no server reachable.
 */
data class ItemsUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val newItemName: String = "",
    val error: ErrorState? = null,
)

sealed interface ItemsEffect {
    data class ItemCreated(val name: String) : ItemsEffect
}

class ItemsViewModel(
    private val repository: ItemsRepository,
) : BaseViewModel<ItemsUiState, ItemsEffect>(ItemsUiState()) {

    init {
        observeItems()
        refresh()
    }

    private fun observeItems() = tryToCollect(
        flow = repository::observeAll,
        onEach = { items -> updateState { it.copy(items = items) } },
        onError = { error -> updateState { it.copy(error = error) } },
    )

    fun refresh() = tryToExecute(
        call = repository::refresh,
        onSuccess = { updateState { it.copy(isLoading = false, error = null) } },
        onError = { error -> updateState { it.copy(isLoading = false, error = error) } },
    )

    fun onNewItemNameChange(value: String) = updateState { it.copy(newItemName = value) }

    fun onAddItem() {
        val name = state.value.newItemName.trim()
        if (name.isEmpty() || state.value.isSaving) return
        updateState { it.copy(isSaving = true) }
        tryToExecute(
            call = { repository.create(name) },
            onSuccess = { created ->
                updateState { it.copy(isSaving = false, newItemName = "", error = null) }
                sendEffect(ItemsEffect.ItemCreated(created.name))
            },
            onError = { error -> updateState { it.copy(isSaving = false, error = error) } },
        )
    }
}
