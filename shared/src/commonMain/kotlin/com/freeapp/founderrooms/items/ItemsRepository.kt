package com.freeapp.founderrooms.items

import com.freeapp.founderrooms.api.ApiClient
import com.freeapp.founderrooms.db.AppDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * EXTENSION POINT: this is the reference repository pattern for the template.
 * Copy this file when adding a new entity and rename `Item` → `YourDomain`.
 *
 * Convention:
 *   - `observe*` returns a Flow backed by the local SQLDelight cache
 *   - `refresh*` fetches from the server and updates the cache
 *   - never expose `ApiClient` or `AppDatabase` directly to the UI layer
 */
class ItemsRepository(
    private val api: ApiClient,
    db: AppDatabase,
) {
    private val queries = db.itemQueries

    fun observeAll(): Flow<List<Item>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { Item(it.id, it.name, it.createdAt) } }

    suspend fun refresh() {
        val remote = api.getItems()
        remote.forEach { queries.upsert(it.id, it.name, it.createdAt) }
    }

    suspend fun create(name: String): Item {
        val created = api.createItem(name)
        queries.upsert(created.id, created.name, created.createdAt)
        return created
    }
}
