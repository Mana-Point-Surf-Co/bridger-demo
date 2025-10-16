package com.bridger.job.controller

import com.bridger.job.db.JobStatus
import com.bridger.job.repository.GeoJsonsRepository
import com.bridger.job.repository.JobsRepository
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private val KML_MEDIA_TYPE = MediaType("application", "vnd.google-earth.kml+xml")

@RestController
@RequestMapping("/api/job")
class JobController(
    private val geoJsonRepository: GeoJsonsRepository,
    private val jobsRepository: JobsRepository,
    private val wakeChannel: Channel<Unit>
) {
    private val logger = KotlinLogging.logger {}

    // Return a 202.
    @PostMapping("/convert")
    fun convertJob(@RequestBody body: ConvertRequest): ResponseEntity<ConvertJobResponse> {

        // Create the Job first
        val jobRecordUuid = jobsRepository.createJob(body.userId)
        logger.info { "Created Job ID: $jobRecordUuid" }

        // Then create the GeoJSON record that references the Job
        val geoRecordUuid = geoJsonRepository.createRecord(jobRecordUuid, body.userId, body.geo.toString())
        logger.info { "Created Geo Record UUID: $geoRecordUuid" }

        wakeChannel.trySend(Unit) // enqueue signal

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            ConvertJobResponse(
                jobId = jobRecordUuid,
                geoRecordId = geoRecordUuid,
                status = JobStatus.PENDING.name
            )
        )
    }

    /**
     * Get job status by id.  TODO: also needs to be returned in a websocket.
     */
    @GetMapping("/{id}")
    fun getJobStatus(@PathVariable id: UUID): ResponseEntity<out Any> {
        val jobRecord = jobsRepository.find(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Job ID: $id not found"))
        val geoJsonRecord = geoJsonRepository.findByJobId(id)

        return ResponseEntity.status(HttpStatus.OK).body(
            JobStatusResponse(
                jobId = id,
                geoRecordId = geoJsonRecord?.id!!,
                status = jobRecord.status.name
            )
        )
    }

    /**
     * Get all jobs with optional status filter and pagination.
     */
    @GetMapping
    fun getAllJobs(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): ResponseEntity<out Any> {
        // Validate status....
        val jobStatus = status?.let {
            try {
                JobStatus.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ErrorResponse("Invalid status value. Must be one of: ${JobStatus.entries.joinToString()}")
                )
            }
        }

        val jobs = jobsRepository.getAllJobsAndFilter(jobStatus, page, pageSize)

        return ResponseEntity.status(HttpStatus.OK).body(
            JobListResponse(
                jobs = jobs.map { job ->
                    val geoJsonRecord = geoJsonRepository.findByJobId(job.id)
                    JobSummary(
                        id = job.id,
                        geoRecordId = geoJsonRecord?.id,
                        status = job.status.name,
                        attempts = job.attempts,
                        lastError = job.lastError
                    )
                },
                page = page,
                pageSize = pageSize,
                count = jobs.size
            )
        )
    }

    /**
     * Ok, so get the job and geo son record.
     * If the job is done return both the kml and geojson else return just the geojson.
     */
    @GetMapping("/{id}/files")
    fun getFiles(@PathVariable id: UUID): ResponseEntity<out Any> {
        val jobRecord = jobsRepository.find(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Job ID: $id not found"))
        val geoJsonRecord = geoJsonRepository.findByJobId(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("GeoJSON record for Job ID: $id not found"))

        return ResponseEntity.status(HttpStatus.OK).body(
            JobFilesResponse(
                jobId = id,
                geoRecordId = geoJsonRecord.id,
                status = jobRecord.status.name,
                geoJson = geoJsonRecord.geoJson,
                kml = if (jobRecord.status == JobStatus.DONE) geoJsonRecord.kml else null
            )
        )
    }

    /**
     * Download KML file directly for a completed job.
     * Returns KML with proper content type for direct download/import to Google Earth.
     */
    @GetMapping("/{id}/kml", produces = ["application/vnd.google-earth.kml+xml"])
    fun downloadKml(@PathVariable id: UUID): ResponseEntity<String> {
        val jobRecord = jobsRepository.find(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job ID: $id not found")

        when(jobRecord.status) {
            JobStatus.FAILED -> return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job has failed. Cannot download KML.")
            JobStatus.DONE -> { /* continue */ }
            else -> return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Job is not complete yet. Current status: ${jobRecord.status.name}")
        }

        val geoJsonRecord = geoJsonRepository.findByJobId(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("GeoJSON record for Job ID: $id not found")

        val kml = geoJsonRecord.kml
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("KML not available for this job")

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"job-$id.kml\"")
            .body(kml)
    }

    /**
     * Delete job regardless of status.
     * Use cascade up in database to delete the geo record which includes the kml.
     */
    @DeleteMapping("/{id}")
    fun deleteJob(@PathVariable id: UUID): ResponseEntity<out Any> {
        // This might need to be a check what if processing and stop in progressing  stuff.
        try {
            // FOR NOW just delete the record.
            jobsRepository.delete(id)
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Job ID: $id not found"))
        }

        return ResponseEntity.ok(DeleteJobResponse("Deleted"))
    }
}

data class ConvertRequest(val userId: String, val geo: JsonNode)

data class ConvertJobResponse(
    val jobId: UUID,
    val geoRecordId: UUID,
    val status: String
)

data class JobStatusResponse(
    val jobId: UUID,
    val geoRecordId: UUID,
    val status: String
)

data class JobSummary(
    val id: UUID,
    val geoRecordId: UUID?,
    val status: String,
    val attempts: Int,
    val lastError: String?
)

data class JobListResponse(
    val jobs: List<JobSummary>,
    val page: Int,
    val pageSize: Int,
    val count: Int
)

data class JobFilesResponse(
    val jobId: UUID,
    val geoRecordId: UUID,
    val status: String,
    val geoJson: String,
    val kml: String? = null
)

data class DeleteJobResponse(
    val message: String
)

data class ErrorResponse(
    val error: String
)
