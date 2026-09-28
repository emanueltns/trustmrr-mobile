package com.freeapp.founderrooms.chat

import com.freeapp.founderrooms.auth.AuthService
import com.freeapp.founderrooms.tiers.PUBLIC_TIER_ID
import com.freeapp.founderrooms.tiers.TiersRepository
import com.freeapp.founderrooms.tiers.unlocks
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Locale
import kotlin.math.roundToLong

/**
 * History + send for a tier room (design doc section 5's "ChatRepository" on
 * the client side; named Service here per house convention, see
 * AuthService, and to keep it distinct from the client's commonMain
 * ChatRepository even though the two live in different modules and could
 * not collide at compile time anyway).
 *
 * Depends on [AuthService] rather than querying UsersTable directly: the
 * author's display name + verifiedMrr are exactly what [AuthService.me]
 * already returns, and duplicating that query here would violate
 * reuse-before-write for zero benefit (send() is not on a hot enough path to
 * care about the extra transaction).
 */
class ChatService(
    private val tiersRepository: TiersRepository,
    private val authService: AuthService,
) {

    /**
     * Last 50 messages for [tierId], oldest first. Ordered by `id` (the
     * BIGSERIAL insert order), not `createdAt`: two messages inserted within
     * the same millisecond would tie on a timestamp sort and could come back
     * out of send order, but `id` is strictly monotonic.
     */
    suspend fun history(tierId: String): List<Message> = dbQuery {
        MessagesTable.selectAll()
            .where { MessagesTable.tierId eq tierId }
            .orderBy(MessagesTable.id to SortOrder.DESC)
            .limit(50)
            .map(::toMessage)
            .reversed()
    }

    suspend fun send(tierId: String, authorId: Long, body: String, linkUrl: String?): Message {
        val author = authService.me(authorId) ?: error("author $authorId not found")
        val badge = authorBadge(author.verifiedMrr)

        // Design doc decision 6: the (+) dialog posts a link card as
        // {url, title = url}, there is no separate title field on the wire
        // (SendMessageRequest carries only body + linkUrl). The title is
        // filled in here, once, at send time, rather than left null and
        // patched up by every reader.
        val linkTitle = linkUrl

        return dbQuery {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val id = MessagesTable.insert {
                it[MessagesTable.tierId] = tierId
                it[MessagesTable.authorId] = authorId
                it[authorName] = author.displayName
                it[authorMrrBadge] = badge
                it[MessagesTable.body] = body
                it[MessagesTable.linkUrl] = linkUrl
                it[MessagesTable.linkTitle] = linkTitle
                it[quotedAuthor] = null
                it[quotedExcerpt] = null
                it[createdAt] = now
            } get MessagesTable.id

            Message(
                id = id,
                tierId = tierId,
                authorId = authorId,
                authorName = author.displayName,
                authorMrrBadge = badge,
                body = body,
                linkUrl = linkUrl,
                linkTitle = linkTitle,
                quotedAuthor = null,
                quotedExcerpt = null,
                createdAt = now.toString(),
            )
        }
    }

    /**
     * The author's OWN current tier (not the room being posted into), per
     * Task 3's badge rule: the highest private tier whose band the author's
     * verifiedMrr falls into, or Public if none does (a never-verified
     * author, null, is treated as 0.0). The bands in TiersTable are
     * contiguous and non-overlapping, so at most one private tier matches.
     */
    private suspend fun authorBadge(verifiedMrr: Double?): String {
        val effectiveMrr = verifiedMrr ?: 0.0
        val allTiers = tiersRepository.allTiers()
        val ownTier = allTiers.firstOrNull { it.id != PUBLIC_TIER_ID && unlocks(it, effectiveMrr) }
            ?: allTiers.first { it.id == PUBLIC_TIER_ID }
        return "${ownTier.emoji} ${formatAmount(effectiveMrr)}/mo"
    }

    /**
     * Deliberately duplicates the client's MrrFormatter
     * (shared/.../util/MrrFormatter.kt): the server module cannot import
     * commonMain client code, and this badge's exact format (plain dollars
     * under $1K, "$2K" from $1K-$1M, "$1.2M" one decimal, trimmed, above
     * $1M) is specified independently for the server in the build brief.
     */
    private fun formatAmount(mrr: Double): String {
        val dollars = mrr.roundToLong()
        return when {
            dollars < 1_000L -> "$$dollars"
            dollars < 1_000_000L -> "$${(dollars / 1_000.0).roundToLong()}K"
            else -> {
                val millions = String.format(Locale.ROOT, "%.1f", dollars / 1_000_000.0)
                val trimmed = if (millions.endsWith(".0")) millions.dropLast(2) else millions
                "$${trimmed}M"
            }
        }
    }

    private fun toMessage(row: ResultRow) = Message(
        id = row[MessagesTable.id],
        tierId = row[MessagesTable.tierId],
        authorId = row[MessagesTable.authorId],
        authorName = row[MessagesTable.authorName],
        authorMrrBadge = row[MessagesTable.authorMrrBadge],
        body = row[MessagesTable.body],
        linkUrl = row[MessagesTable.linkUrl],
        linkTitle = row[MessagesTable.linkTitle],
        quotedAuthor = row[MessagesTable.quotedAuthor],
        quotedExcerpt = row[MessagesTable.quotedExcerpt],
        createdAt = row[MessagesTable.createdAt].toString(),
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
