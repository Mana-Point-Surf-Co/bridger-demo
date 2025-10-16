package com.bridger.job.config

import org.jetbrains.exposed.sql.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class ExposedConfig {

    @Bean(name = ["exposedDatabase"])
    fun exposedDatabase(dataSource: DataSource): Database {
        return Database.connect(dataSource)
    }
}
