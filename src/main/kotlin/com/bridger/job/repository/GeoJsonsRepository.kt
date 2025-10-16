package com.bridger.job.repository

import com.bridger.job.db.GeoJsonRecord
import java.util.*

interface GeoJsonsRepository {
    fun createRecord(jobId: UUID, userId: String, geoJson: String): UUID
    fun updateKml(id: UUID, kml: String)
    fun find(id: UUID): GeoJsonRecord?
    fun findByJobId(jobId: UUID): GeoJsonRecord?
}