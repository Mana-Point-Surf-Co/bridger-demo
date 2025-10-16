package com.bridger.job.db

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.*

enum class JobStatus {
    PENDING,
    PROCESSING,
    IN_PROGRESS,
    DONE,
    FAILED
}

object Jobs : Table("jobs") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val userId = varchar("user_id", 100).index()

    val status = varchar("status", 32).index()
    val attempts = integer("attempts").default(0)
    val lastError = varchar("last_error", 1000).nullable()

    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

data class JobRecord(
    val id: UUID,
    val userId: String,
    val status: JobStatus,
    val attempts: Int,
    val lastError: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

fun ResultRow.toJobRecord() = JobRecord(
    id = this[Jobs.id],
    userId = this[Jobs.userId],
    status = JobStatus.valueOf(this[Jobs.status]),
    attempts = this[Jobs.attempts],
    lastError = this[Jobs.lastError],
    createdAt = this[Jobs.createdAt],
    updatedAt = this[Jobs.updatedAt],
)


