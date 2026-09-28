package com.freeapp.founderrooms.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.net.URI
import java.net.URLDecoder
import javax.sql.DataSource

object DatabaseFactory {
    lateinit var dataSource: DataSource
        private set

    fun init(config: ApplicationConfig) {
        val rawUrl = config.property("database.url").getString()
        val defaultUser = config.property("database.user").getString()
        val defaultPassword = config.property("database.password").getString()
        val maxPoolSize = config.property("database.maxPoolSize").getString().toInt()

        val (jdbcUrl, user, password) = resolveDbUrl(rawUrl, defaultUser, defaultPassword)
        dataSource = hikari(jdbcUrl, user, password, maxPoolSize)
        migrate(dataSource)
        Database.connect(dataSource)
    }

    /**
     * Accepts both URL flavors so the same template works on any host:
     *   - jdbc:postgresql://host:port/db        → use env DATABASE_USER / DATABASE_PASSWORD
     *   - postgres://user:pass@host:port/db     → parse credentials out of the URI
     *   - postgresql://user:pass@host:port/db   → same
     *
     * Managed platforms (Fly.io, Render, Railway, Heroku) inject the postgres:// form
     * via DATABASE_URL automatically; self-hosted Postgres usually wants the jdbc: form.
     */
    private fun resolveDbUrl(
        url: String,
        defaultUser: String,
        defaultPassword: String,
    ): Triple<String, String, String> {
        if (url.startsWith("jdbc:")) {
            return Triple(url, defaultUser, defaultPassword)
        }
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            val uri = URI(url)
            val userInfo = uri.userInfo?.split(":", limit = 2).orEmpty()
            val user = userInfo.getOrNull(0)?.let { URLDecoder.decode(it, "UTF-8") } ?: defaultUser
            val password = userInfo.getOrNull(1)?.let { URLDecoder.decode(it, "UTF-8") } ?: defaultPassword
            val host = uri.host ?: error("DATABASE_URL is missing a host")
            val port = if (uri.port == -1) 5432 else uri.port
            val db = uri.path.removePrefix("/")
            val query = uri.query?.let { "?$it" }.orEmpty()
            return Triple("jdbc:postgresql://$host:$port/$db$query", user, password)
        }
        error("Unsupported DATABASE_URL '$url' — must start with jdbc:, postgres://, or postgresql://")
    }

    private fun hikari(url: String, user: String, password: String, maxPoolSize: Int): HikariDataSource {
        val cfg = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(cfg)
    }

    private fun migrate(ds: DataSource) {
        Flyway.configure().dataSource(ds).load().migrate()
    }
}
