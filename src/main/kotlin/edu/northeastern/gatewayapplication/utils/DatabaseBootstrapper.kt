package edu.northeastern.gatewayapplication.utils

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import edu.northeastern.gatewayapplication.pojo.Account
import edu.northeastern.gatewayapplication.service.AccountService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger{}

@Component
class DatabaseBootstrapper(
    private val accountService: AccountService,
    private val bcryptEncoder: BcryptEncoder
): ApplicationListener<ApplicationReadyEvent> {

    @Value("\${application.config.csv-path}")
    private lateinit var filePath: String

    @Value("\${application.config.user-file}")
    private lateinit var fileName: String

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        logger.info { "Bootstrapping the database" }
        logger.info { filePath + fileName }

        csvReader().open(filePath + fileName) {
            readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->

                val account = Account(
                    firstName = row["first_name"]!!,
                    lastName = row["last_name"]!!,
                    email = row["email"]!!,
                    password = bcryptEncoder.encode(row["password"]!!),
                    accountCreated = getCurrentTimeInISO8601(),
                )
                accountService.save(account)
            }
        }
    }

    fun getCurrentTimeInISO8601(): String {
        val currentTime = ZonedDateTime.now()
        val formatter = DateTimeFormatter.ISO_INSTANT
        return currentTime.format(formatter)
    }

}