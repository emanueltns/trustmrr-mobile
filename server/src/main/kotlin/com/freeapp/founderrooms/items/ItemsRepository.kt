package com.freeapp.founderrooms.items

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ItemsRepository {

    suspend fun all(): List<Item> = dbQuery {
        ItemsTable.selectAll().map(::toItem)
    }

    suspend fun create(name: String): Item = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val id = ItemsTable.insert {
            it[ItemsTable.name] = name
            it[createdAt] = now
        } get ItemsTable.id
        Item(id = id, name = name, createdAt = now.toString())
    }

    private fun toItem(row: ResultRow): Item = Item(
        id = row[ItemsTable.id],
        name = row[ItemsTable.name],
        createdAt = row[ItemsTable.createdAt].toString(),
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
