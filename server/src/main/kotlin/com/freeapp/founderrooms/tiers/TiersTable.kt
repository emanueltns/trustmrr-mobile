package com.freeapp.founderrooms.tiers

import org.jetbrains.exposed.sql.Table

object TiersTable : Table("tiers") {
    // varchar, not auto-increment: every insert supplies the id explicitly
    // (see V2 migration's seed rows). The client's contract requires the
    // public tier's id to be the literal string "public" (see Tier.kt).
    val id = varchar("id", 32)
    val emoji = varchar("emoji", 16)
    val label = varchar("label", 32)
    val mrrFloor = double("mrr_floor")
    val mrrCeiling = double("mrr_ceiling").nullable()
    val sortOrder = integer("sort_order")

    override val primaryKey = PrimaryKey(id)
}
