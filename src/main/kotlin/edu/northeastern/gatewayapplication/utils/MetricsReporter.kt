package edu.northeastern.gatewayapplication.utils

import com.timgroup.statsd.NonBlockingStatsDClient
import org.springframework.stereotype.Component

@Component
class MetricsReporter {
    private val statsDClient = NonBlockingStatsDClient("csye6225", "localhost", 8125)

    fun getStatsDClient(): NonBlockingStatsDClient {
        return statsDClient
    }
}