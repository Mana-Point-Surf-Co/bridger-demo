package com.bridger.job

import com.bridger.job.db.GeoJsonRecords
import com.bridger.job.db.Jobs
import com.bridger.job.service.JobQueue
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class DemoApplication {

	@Bean
	fun onStartup(jobQueue: JobQueue): ApplicationRunner = ApplicationRunner {
		transaction {
			SchemaUtils.createMissingTablesAndColumns(GeoJsonRecords, Jobs)
		}
		// Start the job queue worker after tables are created
		jobQueue.startWorker()
	}
}

fun main(args: Array<String>) {
	runApplication<DemoApplication>(*args)
}
