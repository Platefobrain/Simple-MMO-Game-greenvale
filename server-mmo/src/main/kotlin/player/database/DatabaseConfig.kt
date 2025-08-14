package player.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun initialize(): Database {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5432/game_db"
            username = "game_user"
            password = "game_password"
            maximumPoolSize = 10
            minimumIdle = 5
            connectionTimeout = 30_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
        }

        return Database.connect(HikariDataSource(config))
    }
}