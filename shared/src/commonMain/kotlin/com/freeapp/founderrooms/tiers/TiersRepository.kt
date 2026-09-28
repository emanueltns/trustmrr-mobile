package com.freeapp.founderrooms.tiers

import com.freeapp.founderrooms.api.ApiClient
import com.freeapp.founderrooms.auth.Me

/**
 * The six tiers with member counts and this user's unlock state (design doc
 * section 5, "TiersRepository / GetTiersUseCase"). No local cache: unlike
 * items/ItemsRepository.kt, the tier list is small, cheap to refetch, and its
 * `locked` field depends on the caller's [Me], so caching it would mean
 * caching a value that is only correct for whoever was signed in when it was
 * written.
 */
class TiersRepository(
    private val api: ApiClient,
) {
    suspend fun getTiers(me: Me?): List<Tier> = api.getTiers().toTiers(me)
}
