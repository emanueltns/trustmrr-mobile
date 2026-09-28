package com.freeapp.founderrooms.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object UsersTable : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 255)
    val verifiedMrr = double("verified_mrr").nullable()
    val refreshVersion = integer("refresh_version").default(0)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
