package com.freeapp.founderrooms.items

import kotlinx.serialization.Serializable

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
