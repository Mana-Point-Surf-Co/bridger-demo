package com.bridger.job.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.*

object GeoJsonRecords : Table("geojsons") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val jobId = uuid("job_id").references(Jobs.id, onDelete = ReferenceOption.CASCADE) // DELETE IT.
    val userId = varchar("user_id", 100).index() // Probably don't need this.
    val geoJson = text("geojson_data")
    val kml = text("kml").nullable()

    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(id)
}

data class GeoJsonRecord(
    val id: UUID,
    val jobId: UUID,
    val userId: String,
    val geoJson: String,
    val kml: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

fun ResultRow.toGeoJsonRecord() = GeoJsonRecord(
    id = this[GeoJsonRecords.id],
    userId = this[GeoJsonRecords.userId],
    geoJson = this[GeoJsonRecords.geoJson],
    kml = this[GeoJsonRecords.kml],
    jobId = this[GeoJsonRecords.jobId],
    createdAt = this[GeoJsonRecords.createdAt],
    updatedAt = this[GeoJsonRecords.updatedAt],
)