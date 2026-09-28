package com.freeapp.founderrooms.tiers

import com.freeapp.founderrooms.auth.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class TiersRepository {

    /** The six tiers, sorted ascending by `mrr_floor`, each with its live member count. */
    suspend fun all(): List<TierResponse> = dbQuery {
        val tiers = TiersTable.selectAll().orderBy(TiersTable.sortOrder to SortOrder.ASC).map(::toTier)
        val everyVerifiedMrr = UsersTable.select(UsersTable.verifiedMrr).map { it[UsersTable.verifiedMrr] }

        tiers.map { tier ->
            val memberCount = everyVerifiedMrr.count { verifiedMrr -> unlocks(tier, verifiedMrr) }
            TierResponse(
                id = tier.id,
                emoji = tier.emoji,
                label = tier.label,
                mrrFloor = tier.mrrFloor,
                mrrCeiling = tier.mrrCeiling,
                memberCount = memberCount,
            )
        }
    }

    suspend fun findById(tierId: String): Tier? = dbQuery {
        TiersTable.selectAll().where { TiersTable.id eq tierId }.limit(1).map(::toTier).firstOrNull()
    }

    /**
     * Raw domain tiers (no member-count join), sorted ascending by
     * sortOrder. Added for ChatService's "author's own tier" badge lookup
     * (build brief Task 3): that needs the six [Tier] rows to run [unlocks]
     * against, not the wire-shaped [TierResponse] with its memberCount.
     */
    suspend fun allTiers(): List<Tier> = dbQuery {
        TiersTable.selectAll().orderBy(TiersTable.sortOrder to SortOrder.ASC).map(::toTier)
    }

    private fun toTier(row: ResultRow) = Tier(
        id = row[TiersTable.id],
        emoji = row[TiersTable.emoji],
        label = row[TiersTable.label],
        mrrFloor = row[TiersTable.mrrFloor],
        mrrCeiling = row[TiersTable.mrrCeiling],
        sortOrder = row[TiersTable.sortOrder],
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
