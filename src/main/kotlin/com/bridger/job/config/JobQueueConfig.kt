package com.bridger.job.config

import kotlinx.coroutines.channels.Channel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JobQueueConfig {
    // TODO: SERVICE WORKER QUEUE.
    @Bean
    fun wakeChannel(): Channel<Unit> = Channel(capacity = Channel.CONFLATED)
}