package com.bridger.job.repository

import com.bridger.job.db.JobRecord
import com.bridger.job.db.JobStatus
import java.util.*

interface JobsRepository {
    fun createJob(userId: String): UUID
    fun delete(jobId: UUID): Unit
    fun find(jobId: UUID): JobRecord?
    fun getAllJobsAndFilter(status: JobStatus?, page: Int = 0, pageSize: Int = 20): List<JobRecord>
    fun updateStatus(jobId: UUID, status: JobStatus)
    fun markFailed(jobId: UUID, error: String)
}