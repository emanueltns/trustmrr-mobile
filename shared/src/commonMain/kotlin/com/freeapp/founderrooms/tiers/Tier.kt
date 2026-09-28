package com.freeapp.founderrooms.tiers

import com.freeapp.founderrooms.auth.Me
import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /tiers`. No `locked` field: that is computed
 * client-side from `Me.verifiedMrr` (design doc decision 2), never sent by
 * the server.
 *
 * ASSUMPTION: `id` is a String, and the public room's id is the literal
 * `"public"`, matching the seed data in the design doc section 7
 * (`(public, 0, 0, 0), (1, "🌿", ...), (2, ...) ...`). This is the one place
 * client code depends on a specific seed value rather than a server-computed
 * flag, because the wire contract carries no `isPublic` field to key off
 * instead. If the server ends up using a different id for the public room,
 * [PUBLIC_TIER_ID] is the single place to change.
 */
@Serializable
data class TierDto(
    val id: String,
    val emoji: String,
    val label: String,
    val mrrFloor: Double,
    val mrrCeiling: Double? = null,
    val memberCount: Int,
)

/** Domain shape: [TierDto] plus this user's unlock state. */
data class Tier(
    val id: String,
    val emoji: String,
    val label: String,
    val mrrFloor: Double,
    val mrrCeiling: Double?,
    val memberCount: Int,
    val locked: Boolean,
)

/** See the id assumption on [TierDto]. */
internal const val PUBLIC_TIER_ID = "public"

/**
 * Maps the wire list to the domain list, computing [Tier.locked] per design
 * doc decision 2: Public is always unlocked; a private tier unlocks only for
 * the user's own matching band `[mrrFloor, mrrCeiling)`, never "at or
 * below/above". Sorted ascending by `mrrFloor`, the design doc's own
 * pass/fail check for `GetTiersUseCase`.
 *
 * Pure and network-free on purpose: this is the seam commonTest exercises
 * directly with hand-built [TierDto] lists standing in for a server
 * response, rather than a faked [com.freeapp.founderrooms.api.ApiClient]
 * (there is no interface seam on that concrete class to fake against, and a
 * pure mapping function is the more direct test of the lock predicate).
 */
internal fun List<TierDto>.toTiers(me: Me?): List<Tier> =
    sortedBy { it.mrrFloor }.map { dto ->
        Tier(
            id = dto.id,
            emoji = dto.emoji,
            label = dto.label,
            mrrFloor = dto.mrrFloor,
            mrrCeiling = dto.mrrCeiling,
            memberCount = dto.memberCount,
            locked = !dto.isUnlockedFor(me),
        )
    }

private fun TierDto.isUnlockedFor(me: Me?): Boolean {
    if (id == PUBLIC_TIER_ID) return true
    val verifiedMrr = me?.verifiedMrr ?: return false
    return verifiedMrr >= mrrFloor && (mrrCeiling == null || verifiedMrr < mrrCeiling)
}
