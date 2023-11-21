package edu.northeastern.gatewayapplication.controller

import com.timgroup.statsd.NonBlockingStatsDClient
import edu.northeastern.gatewayapplication.utils.GenericUtils
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.SQLException
import javax.sql.DataSource

/**
 * Controller that determines the health of the application.
 * Pings the DB for each incoming request to determine this state
 *
 * @property dataSource: micronaut injected datasource that contains DB configs
 */

@RestController
@RequestMapping("/healthz")
class HealthController(val dataSource: DataSource) {

    private val logger = KotlinLogging.logger {}
    private val utils = GenericUtils()
    private val metricsReporter = NonBlockingStatsDClient("webapp", "localhost", 8125)

    /**
     * pings the DB to figure out the state of the application
     * @param request: HttpRequest object injected by micronaut
     * @return HTTPResponse object with appropriate status code
     */
    @GetMapping(produces = ["application/json"])
    fun health(@RequestBody(required = false) requestBody: String?, request: HttpServletRequest): ResponseEntity<String> {
        logger.info { "Hitting the /healthz endpoint" }
        metricsReporter.increment("health.get")
        val response: ResponseEntity<String> = try {
            if (utils.requestContainBodyOrParams(requestBody, request))
                return ResponseEntity.badRequest().build()

            dataSource.connection.use {
                it.prepareStatement("SELECT CURRENT_TIMESTAMP").executeQuery().close()
            }
            logger.info { "Connection to database successful. Executed test query" }
            ResponseEntity.ok().build()
        }catch (e: SQLException) {
            logger.info { "Connection to database failed: " + e.localizedMessage}
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }
        return response
    }
}