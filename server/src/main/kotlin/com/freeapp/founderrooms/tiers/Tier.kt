package com.freeapp.founderrooms.tiers

import kotlinx.serialization.Serializable

/**
 * The "$0" / public band that is always open (design doc section 7). A
 * String, not an Int: the client's TierDto.id is a String and its own
 * comment is explicit that the public room's id must be the literal
 * "public" (shared/.../tiers/Tier.kt PUBLIC_TIER_ID). Every other tier id is
 * also a String on the wire ("1".."5"), matching the V2 migration's seed.
 */
const val PUBLIC_TIER_ID = "public"

/** Internal row shape, used for the unlock predicate; the wire shape is [TierResponse]. */
data class Tier(
    val id: String,
    val emoji: String,
    val label: String,
    val mrrFloor: Double,
    val mrrCeiling: Double?,
    val sortOrder: Int,
)

@Serializable
data class TierResponse(
    val id: String,
    val emoji: String,
    val label: String,
    val mrrFloor: Double,
    val mrrCeiling: Double?,
    val memberCount: Int,
)

/**
 * Decision 2: Public is open to everyone; a private tier unlocks only for the
 * user's own matching band, `[mrrFloor, mrrCeiling)`, never "at or below" or
 * "at or above". A null [verifiedMrr] (never verified) unlocks nothing but
 * Public. The top tier has no ceiling, so it unlocks for anything at or above
 * its floor.
 */
fun unlocks(tier: Tier, verifiedMrr: Double?): Boolean {
    if (tier.id == PUBLIC_TIER_ID) return true
    if (verifiedMrr == null) return false
    val ceiling = tier.mrrCeiling
    return verifiedMrr >= tier.mrrFloor && (ceiling == null || verifiedMrr < ceiling)
}
