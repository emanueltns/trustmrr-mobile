package com.freeapp.founderrooms.items

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ItemsTable : Table("items") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
