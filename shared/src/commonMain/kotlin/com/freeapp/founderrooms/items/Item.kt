package com.freeapp.founderrooms.items

import kotlinx.serialization.Serializable

/**
 * EXTENSION POINT: mirror this file for new domain entities.
 * Keep field names and JSON shape identical to the server-side DTO
 * in `server/src/main/kotlin/com.freeapp.founderrooms/items/Item.kt`.
 */
@Serializable
data class Item(
    val id: Long,
    val name: String,
    val createdAt: String,
)

@Serializable
data class CreateItemRequest(
    val name: String,
)
