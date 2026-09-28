package com.freeapp.founderrooms.chat

import kotlinx.serialization.Serializable

/**
 * EXTENSION POINT: mirror this file for new domain entities.
 * Field names must match the server-side chat DTO byte for byte, exactly
 * like every other DTO in this template (see items/Item.kt). This shape is
 * also what arrives over the `/chat/{tierId}/socket` WebSocket, one JSON
 * object per text frame.
 *
 * ASSUMPTION: `id` and `authorId` are Long (matching `Me.id`'s convention);
 * `tierId` is a String and must equal a [com.freeapp.founderrooms.tiers.TierDto.id]
 * value for the tier lock/routing logic to line up.
 */
@Serializable
data class Message(
    val id: Long,
    val tierId: String,
    val authorId: Long,
    val authorName: String,
    val authorMrrBadge: String? = null,
    val body: String,
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val quotedAuthor: String? = null,
    val quotedExcerpt: String? = null,
    val createdAt: String,
)

/** Request body of `POST /chat/{tierId}/messages`. */
@Serializable
data class SendMessageRequest(
    val body: String,
    val linkUrl: String? = null,
)
