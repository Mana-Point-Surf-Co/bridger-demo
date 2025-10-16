package com.bridger.job.service

import com.bridger.job.db.JobRecord
import com.bridger.job.db.JobStatus
import com.bridger.job.repository.GeoJsonsRepository
import com.bridger.job.repository.JobsRepository
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.coroutines.coroutineContext

data class JobStatusMessage(
    val type: String = "JOB_STATUS",
    val jobId: String,
    val status: JobStatus,
    val jobType: String = "CONVERT",
    val geoRecordId: String? = null,
    val error: String? = null
)

@Component
class JobQueue(
    private val jobs: JobsRepository,
    private val geoJsons: GeoJsonsRepository,
    private val converter: GeoJsonToKmlConverter,
    private val notifier: com.bridger.job.websocket.WsNotifier,
    private val wakeChannel: Channel<Unit>,
) {
    private val log = KotlinLogging.logger {}
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun startWorker() {
        scope.launch { worker() }
    }

    // Graceful shutdown! lol.
    @PreDestroy
    fun shutdown() = scope.cancel()

    private suspend fun worker() {
        log.info { "Queue worker started, let's go...." }
        while (coroutineContext.isActive) {

            try {
                val job = nextPendingOrNull()
                if (job == null) {
                    withTimeoutOrNull(500) { wakeChannel.receive() } // sleep until signaled or timeout
                    continue
                }
                notifier.notifyUser(job.userId, JobStatusMessage(
                    jobId = job.id.toString(),
                    status = JobStatus.PENDING
                ))

                jobs.updateStatus(job.id, JobStatus.PROCESSING)
                notifier.notifyUser(job.userId, JobStatusMessage(
                    jobId = job.id.toString(),
                    status = JobStatus.PROCESSING
                ))

                try {
                    val geo = geoJsons.findByJobId(job.id) ?: error("GeoJSON not found for job ${job.id}")

                    val kml = converter.convertToKml(geo.geoJson)
                    geoJsons.updateKml(geo.id, kml)

                    jobs.updateStatus(job.id, JobStatus.DONE)
                    notifier.notifyUser(job.userId, JobStatusMessage(
                        jobId = job.id.toString(),
                        status = JobStatus.DONE,
                        geoRecordId = geo.id.toString()
                    ))
                } catch (t: Throwable) {
                    // one-shot fail: record and notify
                    jobs.markFailed(job.id, t.message ?: t::class.simpleName ?: "Unknown error")
                    notifier.notifyUser(job.userId, JobStatusMessage(
                        jobId = job.id.toString(),
                        status = JobStatus.FAILED,
                        error = t.message ?: "Unknown error"
                    ))
                    log.error(t) { "Job ${job.id} failed" }
                }
            } catch (loopErr: Throwable) {
                log.error(loopErr) { "Worker loop error" }
                delay(500)
            }
        }
    }

    fun nextPendingOrNull(): JobRecord? =
        jobs.getAllJobsAndFilter(JobStatus.PENDING, page = 0, pageSize = 1).firstOrNull()
}
