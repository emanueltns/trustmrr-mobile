package com.freeapp.founderrooms.chat

import kotlinx.serialization.Serializable

/**
 * Field names must match the client's `shared/.../chat/Message.kt` byte for
 * byte (same convention as every other DTO in this template, see
 * items/Item.kt): that file is also what a client decodes off the
 * `/chat/{tierId}/socket` WebSocket, one JSON object per text frame.
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
