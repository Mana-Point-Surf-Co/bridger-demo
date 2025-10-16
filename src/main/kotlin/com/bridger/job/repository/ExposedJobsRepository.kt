package com.bridger.job.repository

import com.bridger.job.db.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ExposedJobsRepository : JobsRepository {
    private val logger = KotlinLogging.logger {}

    // Maybe overkill.  Status updates will need to be done in line to manage attempts, last error, etc. good times.
    override fun createJob(userId: String): UUID {
        val jobId = UUID.randomUUID()
        transaction {
            Jobs.insert {
                it[Jobs.id] = jobId
                it[Jobs.userId] = userId
                it[status] = JobStatus.PENDING.name
                it[attempts] = 0
                it[lastError] = null
                it[createdAt] = OffsetDateTime.now()
                it[updatedAt] = OffsetDateTime.now()
            }
        }
        logger.info { "Created Job ID: $jobId" }
        return jobId
    }

    override fun delete(jobId: UUID) {
        transaction {
            Jobs.deleteWhere { id eq jobId }
        }
    }

    override fun find(jobId: UUID): JobRecord? = transaction {
        Jobs.selectAll()
            .where { Jobs.id eq jobId }
            .singleOrNull()?.toJobRecord()
    }

    override fun getAllJobsAndFilter(status: JobStatus?, page: Int, pageSize: Int): List<JobRecord> = transaction {
        // bro basic pagination.
        val offset = page * pageSize.toLong()

        val query = if (status != null) {
            Jobs.selectAll()
                .where { Jobs.status eq status.name }
        } else {
            Jobs.selectAll()
        }

        query
            .orderBy(Jobs.createdAt to SortOrder.DESC)
            .limit(pageSize)
            .offset(offset)
            .map { it.toJobRecord() }
    }

    override fun updateStatus(jobId: UUID, status: JobStatus) {
        transaction {
            Jobs.update({ Jobs.id eq jobId }) {
                it[Jobs.status] = status.name
                it[updatedAt] = OffsetDateTime.now()
            }
        }
        logger.info { "Updated Job $jobId status to ${status.name}" }
    }

    override fun markFailed(jobId: UUID, error: String) {
        transaction {
            Jobs.update({ Jobs.id eq jobId }) {
                it[status] = JobStatus.FAILED.name
                it[lastError] = error.take(1000) // ONLY -> Truncate to first 1000 characters
                it[updatedAt] = OffsetDateTime.now()
            }
        }
        logger.info { "Marked Job $jobId as FAILED: $error" }
    }
}