package com.freeapp.founderrooms.chat

import com.freeapp.founderrooms.auth.UsersTable
import com.freeapp.founderrooms.tiers.TiersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object MessagesTable : Table("messages") {
    val id = long("id").autoIncrement()
    val tierId = varchar("tier_id", 32).references(TiersTable.id)
    val authorId = long("author_id").references(UsersTable.id)

    // Snapshotted at send time, not joined live (Task 3): a later display-name
    // change or re-verification must not rewrite what earlier messages show.
    val authorName = varchar("author_name", 255)
    val authorMrrBadge = varchar("author_mrr_badge", 32).nullable()

    val body = text("body")
    val linkUrl = text("link_url").nullable()
    val linkTitle = varchar("link_title", 255).nullable()
    val quotedAuthor = varchar("quoted_author", 255).nullable()
    val quotedExcerpt = text("quoted_excerpt").nullable()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
