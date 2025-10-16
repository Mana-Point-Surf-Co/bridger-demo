package com.bridger.job.repository

import com.bridger.job.db.GeoJsonRecord
import com.bridger.job.db.GeoJsonRecords
import com.bridger.job.db.toGeoJsonRecord
import mu.KotlinLogging
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

@Repository
class ExposedGeoJsonsRepository : GeoJsonsRepository {
    private val logger = KotlinLogging.logger {}

    override fun createRecord(jobId: UUID, userId: String, geoJson: String): UUID {
        val id = UUID.randomUUID()
        transaction {
            GeoJsonRecords.insert {
                it[GeoJsonRecords.id] = id
                it[GeoJsonRecords.jobId] = jobId
                it[GeoJsonRecords.userId] = userId
                it[GeoJsonRecords.geoJson] = geoJson
                it[kml] = null
                it[createdAt] = OffsetDateTime.now()
                it[updatedAt] = OffsetDateTime.now()
            }
        }
        logger.info { "Created Geo Json Record $id" }
        return id
    }

    override fun updateKml(id: UUID, kml: String) = transaction {
        GeoJsonRecords.update({ GeoJsonRecords.id eq id }) {
            it[GeoJsonRecords.kml] = kml
            it[updatedAt] = OffsetDateTime.now()
        }
        logger.info { "updated KML $id" }
    }

    override fun find(id: UUID): GeoJsonRecord? = transaction {
        GeoJsonRecords.selectAll()
            .where { GeoJsonRecords.id eq id }
            .limit(1)
            .map { it.toGeoJsonRecord() }
            .singleOrNull()
    }

    override fun findByJobId(jobId: UUID): GeoJsonRecord? = transaction {
        GeoJsonRecords.selectAll()
            .where { GeoJsonRecords.jobId eq jobId }
            .limit(1)
            .map { it.toGeoJsonRecord() }
            .singleOrNull()
    }
}